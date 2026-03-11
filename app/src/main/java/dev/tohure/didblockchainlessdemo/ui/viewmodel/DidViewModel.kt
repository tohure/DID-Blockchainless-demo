package dev.tohure.didblockchainlessdemo.ui.viewmodel

import android.app.Application
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import androidx.lifecycle.viewModelScope
import dev.tohure.didblockchainlessdemo.BuildConfig
import dev.tohure.didblockchainlessdemo.crypto.CryptoManager
import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel
import dev.tohure.didblockchainlessdemo.data.repository.CredentialRepository
import dev.tohure.didblockchainlessdemo.did.DIDKeyManager
import dev.tohure.didblockchainlessdemo.did.ProofJWTBuilder
import dev.tohure.didblockchainlessdemo.did.VpJWTBuilder
import dev.tohure.didblockchainlessdemo.storage.CredentialStore
import dev.tohure.didblockchainlessdemo.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class DidViewModel(application: Application) : BiometricAwareViewModel<DidUiState>(application) {

    private val didKeyManager = DIDKeyManager(application)
    private val repository = CredentialRepository()
    private val proofBuilder = ProofJWTBuilder(didKeyManager)
    private val vpBuilder = VpJWTBuilder(didKeyManager)

    private val crypto = CryptoManager()
    private val store = CredentialStore(application)

    override val vmTag = "did-vm"
    override val _uiState = MutableStateFlow(DidUiState())
    val uiState: StateFlow<DidUiState> = _uiState.asStateFlow()

    override fun DidUiState.withLoading(loading: Boolean) = copy(isLoading = loading)
    override fun DidUiState.withBiometricPrompt(show: Boolean) = copy(showBiometricPrompt = show)
    override fun DidUiState.withStatus(message: String) = copy(statusMessage = message)

    init {
        refreshKeyStatus()
    }

    fun generateDIDKeys() = launch {
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

    fun deleteDIDKeys() = launch {
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

    fun clearProofJwt() =
        _uiState.update { it.copy(lastProofJwt = "", statusMessage = "Proof JWT limpiado") }

    fun clearDecryptedMetadata() =
        _uiState.update { it.copy(decryptedMetadata = "", statusMessage = "Metadatos limpiados") }

    fun requestCredentialWithNonce(
        issuerUrl: String = BuildConfig.BASE_URL,
        credentialType: String = "UniversityDegreeCredential",
        subjectClaims: Map<String, String> = mapOf(
            "givenName" to "Juan", "familyName" to "Perez", "email" to "juan@example.com"
        ),
    ) {
        launch {
            _uiState.update { it.copy(statusMessage = "Iniciando proceso...") }

            runCatching {
                check(didKeyManager.keysExist()) { "Primero genera las claves DID" }

                val did = didKeyManager.getDID()
                val clientId = subjectClaims["email"] ?: error("El email es requerido")

                _uiState.update { it.copy(statusMessage = "Registrando DID...") }
                repository.registerDid(did, clientId).getOrThrow()

                _uiState.update { it.copy(statusMessage = "Solicitando nonce...") }
                val nonce = repository.fetchNonce(holderDid = did).getOrThrow()

                val proofJwt = proofBuilder.build(issuerUrl, nonce, credentialType, subjectClaims)
                _uiState.update { it.copy(lastProofJwt = proofJwt, statusMessage = "Enviando Proof JWT...") }

                val credentialVC = repository.registerProof(did, proofJwt).getOrThrow()

                _uiState.update { it.copy(statusMessage = "Obteniendo metadatos...") }
                val metadata = repository.getMetaDataCredential(did).getOrThrow()
                val metadataJson = Json.encodeToString(metadata)

                // Cifrar metadatos para almacenamiento seguro
                check(crypto.keyPairExists()) { "Se requieren claves RSA para guardar la credencial" }
                val payload = crypto.encrypt(credentialVC.credential)
                store.save(CREDENTIAL_ID, payload)

                Triple(proofJwt, metadataJson, payload)

            }.onSuccess { (proofJwt, metadataJson, payload) ->
                _uiState.update {
                    it.copy(
                        lastProofJwt = proofJwt,
                        decryptedMetadata = metadataJson,
                        encryptedCredential = payload,
                        statusMessage = "Metadatos recibidos y cifrados correctamente",
                    )
                }
            }.onFailure { e ->
                AppLogger.e(vmTag, "Error en requestCredentialWithNonce: ${e.message}", e)
                if (e !is UserNotAuthenticatedException && e !is KeyPermanentlyInvalidatedException) {
                    _uiState.update { it.withStatus("Error: ${e.message ?: "Desconocido"}") }
                } else {
                    throw e // Re-lanzar para que el launch base lo capture
                }
            }
        }
    }

    fun verifyVP() = launch {
        _uiState.update { it.copy(statusMessage = "Verificando VP...") }

        runCatching {
            check(crypto.keyPairExists()) { "No hay claves RSA" }
            val payload = store.load(CREDENTIAL_ID) ?: error("No hay credencial guardada")
            val credentialJson = crypto.decrypt(payload)

            val vpJwt = vpBuilder.build(credentialJson, BuildConfig.BASE_URL)
            repository.validateCredentials(vpJwt).getOrThrow()
        }.onSuccess { response ->
            val status = if (response.valid) "VP Válida" else "VP Inválida"
            val responseJson = prettyJson.encodeToString(response)
            _uiState.update {
                it.copy(
                    statusMessage = "Verificación: $status. Holder: ${response.holderDid}",
                    validationResponseJson = responseJson
                )
            }
        }.onFailure { e ->
            AppLogger.e(vmTag, "Error en verifyVP: ${e.message}", e)
            if (e !is UserNotAuthenticatedException && e !is KeyPermanentlyInvalidatedException) {
                _uiState.update { it.withStatus("Error al verificar: ${e.message}") }
            } else {
                throw e // Re-lanzar para que el launch base lo capture
            }
        }
    }

    private fun refreshKeyStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val didExists = didKeyManager.keysExist()
            val did = if (didExists) runCatching { didKeyManager.getDID() }.getOrDefault("") else ""
            val keyId =
                if (didExists) runCatching { didKeyManager.getKeyId() }.getOrDefault("") else ""
            val didLevel =
                if (didExists) didKeyManager.getSecurityLevel() else SecurityLevel.UNKNOWN

            _uiState.update {
                it.copy(
                    didKeysExist = didExists, did = did, keyId = keyId, didSecurityLevel = didLevel
                )
            }
        }
    }

    companion object {
        private const val CREDENTIAL_ID = "demo_vc"
        private val prettyJson = Json { prettyPrint = true }
    }
}