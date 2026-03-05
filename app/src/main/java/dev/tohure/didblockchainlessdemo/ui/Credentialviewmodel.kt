package dev.tohure.didblockchainlessdemo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tohure.didblockchainlessdemo.BuildConfig
import dev.tohure.didblockchainlessdemo.crypto.CryptoManager
import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel
import dev.tohure.didblockchainlessdemo.data.repository.CredentialRepository
import dev.tohure.didblockchainlessdemo.did.DIDKeyManager
import dev.tohure.didblockchainlessdemo.did.ProofJWTBuilder
import dev.tohure.didblockchainlessdemo.storage.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CredentialViewModel(application: Application) : AndroidViewModel(application) {

    private val didKeyManager = DIDKeyManager(application) // DID (secp256k1)
    private val crypto = CryptoManager() // cifrado VCs (RSA)
    private val store = CredentialStore(application)
    private val repository = CredentialRepository()
    private val proofBuilder get() = ProofJWTBuilder(didKeyManager)

    private val _uiState = MutableStateFlow(CredentialUiState())
    val uiState: StateFlow<CredentialUiState> = _uiState.asStateFlow()

    init {
        refreshAllKeyStatuses()
    }

    fun generateDIDKeys() = launchCrypto {
        val generated = didKeyManager.generateKeysIfNeeded()
        val level = didKeyManager.getSecurityLevel()
        val did = didKeyManager.getDID()
        val keyId = didKeyManager.getKeyId()
        val msg = if (generated) "Identidad DID creada (secp256k1) en: ${level.name}"
        else "Las claves DID ya existían"
        _uiState.update {
            it.copy(
                didKeysExist = true,
                did = did,
                keyId = keyId,
                didSecurityLevel = level,
                statusMessage = msg,
            )
        }
    }

    fun deleteDIDKeys() = launchCrypto {
        didKeyManager.deleteKeys()
        _uiState.update {
            it.copy(
                didKeysExist = false,
                did = "",
                keyId = "",
                didSecurityLevel = SecurityLevel.UNKNOWN,
                lastProofJwt = "",
                statusMessage = "Claves DID eliminadas",
            )
        }
    }

    fun generateRsaKeys() = launchCrypto {
        val generated = crypto.generateKeyPairIfNeeded()
        val level = crypto.getSecurityLevel()
        val pub = crypto.getPublicKeyBase64()
        val msg = if (generated) "Par RSA-2048 creado en el Keystore en: ${level.name}"
        else "Las claves RSA ya existían"
        _uiState.update {
            it.copy(
                rsaKeyExists = true,
                publicKeyBase64 = pub,
                rsaSecurityLevel = level,
                statusMessage = msg,
            )
        }
    }

    fun deleteRsaKeys() = launchCrypto {
        crypto.deleteKeyPair()
        store.clear()
        _uiState.update {
            it.copy(
                rsaKeyExists = false,
                publicKeyBase64 = "",
                encryptedPayload = "",
                decryptedJson = "",
                statusMessage = "Claves RSA y credenciales cifradas eliminadas",
            )
        }
    }

    fun encrypt() = launchCrypto {
        check(crypto.keyPairExists()) { "Primero genera las claves RSA" }
        val json = _uiState.value.jsonInput
        require(json.isNotBlank()) { "El JSON no puede estar vacío" }
        val payload = crypto.encrypt(json)
        store.save(CREDENTIAL_ID, payload)
        _uiState.update {
            it.copy(
                encryptedPayload = payload,
                decryptedJson = "",
                statusMessage = "JSON cifrado y guardado con AES-256-GCM + RSA-OAEP",
            )
        }
    }

    fun decrypt() = launchCrypto {
        check(crypto.keyPairExists()) { "Primero genera las claves RSA" }
        val payload =
            store.load(CREDENTIAL_ID) ?: error("No hay ninguna credencial guardada. Cifra primero.")
        val json = crypto.decrypt(payload)
        _uiState.update {
            it.copy(
                decryptedJson = json,
                statusMessage = "JSON descifrado correctamente con la clave privada del Keystore",
            )
        }
    }

    /**
     * Solicita un nonce al backend, construye el Proof JWT con las claves secp256k1
     * y lo muestra en la UI. Listo para enviarlo al endpoint de emisión de la VC.
     */
    fun requestCredentialWithNonce(
        issuerUrl: String = BuildConfig.BASE_URL,
        credentialType: String = "VerifiableCredential",
        subjectClaims: Map<String, String> = emptyMap(),
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isFetching = true, statusMessage = "Solicitando nonce al backend..."
                )
            }
            check(didKeyManager.keysExist()) { "Primero genera las claves DID" }

            val did = didKeyManager.getDID()

            repository.fetchNonce(holderDid = did).onSuccess { nonce ->
                    val proofJwt =
                        proofBuilder.build(issuerUrl, nonce, credentialType, subjectClaims)
                    _uiState.update {
                        it.copy(
                            isFetching = false,
                            lastProofJwt = proofJwt,
                            statusMessage = "Proof JWT generado ✓  —  listo para enviar al issuer",
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isFetching = false,
                            statusMessage = "Error al obtener nonce: ${e.message}",
                        )
                    }
                }
        }
    }

    fun fetchAndEncrypt(credentialId: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isFetching = true, statusMessage = "Descargando credencial..."
                )
            }
            repository.fetchCredential(credentialId, token).onSuccess { json ->
                    _uiState.update { it.copy(jsonInput = json, isFetching = false) }
                    encrypt()
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isFetching = false, statusMessage = "Error al descargar: ${e.message}"
                        )
                    }
                }
        }
    }

    fun updateJsonInput(value: String) = _uiState.update { it.copy(jsonInput = value) }

    fun clearDecryptedJson() =
        _uiState.update { it.copy(decryptedJson = "", statusMessage = "Texto limpiado") }

    fun clearProofJwt() =
        _uiState.update { it.copy(lastProofJwt = "", statusMessage = "Proof JWT limpiado") }

    private fun refreshAllKeyStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            // DID key
            val didExists = didKeyManager.keysExist()
            val did = if (didExists) runCatching { didKeyManager.getDID() }.getOrDefault("") else ""
            val keyId =
                if (didExists) runCatching { didKeyManager.getKeyId() }.getOrDefault("") else ""
            val didLevel =
                if (didExists) didKeyManager.getSecurityLevel() else SecurityLevel.UNKNOWN

            // RSA keys
            val rsaExists = crypto.keyPairExists()
            val pub =
                if (rsaExists) runCatching { crypto.getPublicKeyBase64() }.getOrDefault("") else ""
            val rsaLevel = if (rsaExists) crypto.getSecurityLevel() else SecurityLevel.UNKNOWN

            _uiState.update {
                it.copy(
                    didKeysExist = didExists,
                    did = did,
                    keyId = keyId,
                    didSecurityLevel = didLevel,
                    rsaKeyExists = rsaExists,
                    publicKeyBase64 = pub,
                    rsaSecurityLevel = rsaLevel,
                )
            }
        }
    }

    private fun launchCrypto(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Procesando...") }
            runCatching { block() }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "Error: ${e.message ?: e.javaClass.simpleName}"
                        )
                    }
                }.onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    companion object {
        private const val CREDENTIAL_ID = "demo_vc"
    }
}