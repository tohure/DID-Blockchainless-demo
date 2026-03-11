package dev.tohure.didblockchainlessdemo.ui.viewmodel

import android.app.Application
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DidViewModel(application: Application) : AndroidViewModel(application) {

    private val didKeyManager = DIDKeyManager(application)
    private val repository = CredentialRepository()
    private val proofBuilder = ProofJWTBuilder(didKeyManager)
    private val vpBuilder = VpJWTBuilder(didKeyManager)
    
    // Dependencias para guardar la credencial recibida
    private val crypto = CryptoManager()
    private val store = CredentialStore(application)

    private val _uiState = MutableStateFlow(DidUiState())
    val uiState: StateFlow<DidUiState> = _uiState.asStateFlow()
    
    private var pendingAction: (suspend () -> Unit)? = null

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
            "givenName" to "Juan",
            "familyName" to "Perez",
            "email" to "juan@example.com"
        ),
    ) {
        launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Iniciando proceso...") }

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
                        isLoading = false,
                        lastProofJwt = proofJwt,
                        decryptedMetadata = metadataJson,
                        encryptedCredential = payload,
                        statusMessage = "Metadatos recibidos y cifrados correctamente",
                    )
                }
            }.onFailure { e ->
                // Si es UserNotAuthenticatedException o KeyPermanentlyInvalidatedException, se maneja en el bloque launch general
                if (e !is UserNotAuthenticatedException && e !is KeyPermanentlyInvalidatedException) {
                    AppLogger.e("did-vm", "Error en requestCredentialWithNonce: ${e.message}", e)
                    _uiState.update {
                        it.copy(isLoading = false, statusMessage = "Error: ${e.message ?: "Desconocido"}")
                    }
                } else {
                    throw e // Re-lanzar para que launch lo capture
                }
            }
        }
    }

    fun verifyVP() = launch {
        _uiState.update { it.copy(isLoading = true, statusMessage = "Verificando VP...") }

        runCatching {
            check(crypto.keyPairExists()) { "No hay claves RSA" }
            val payload = store.load(CREDENTIAL_ID) ?: error("No hay credencial guardada")
            val credentialJson = crypto.decrypt(payload)

            val vpJwt = vpBuilder.build(credentialJson, BuildConfig.BASE_URL)
            
            repository.validateCredentials(vpJwt).getOrThrow()
        }.onSuccess { response ->
            val status = if (response.valid) "VP Válida" else "VP Inválida"
            
            val json = Json { prettyPrint = true }
            val responseJson = json.encodeToString(response)
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = "Verificación: $status. Holder: ${response.holderDid}",
                    validationResponseJson = responseJson
                )
            }
        }.onFailure { e ->
            if (e !is UserNotAuthenticatedException && e !is KeyPermanentlyInvalidatedException) {
                AppLogger.e("did-vm", "Error en verifyVP: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, statusMessage = "Error al verificar: ${e.message}")
                }
            } else {
                throw e
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(showBiometricPrompt = false) }
        pendingAction?.let { action ->
            pendingAction = null
            launch(action)
        }
    }

    fun onBiometricFailure() {
        _uiState.update { it.copy(showBiometricPrompt = false, isLoading = false, statusMessage = "Autenticación biométrica fallida") }
        pendingAction = null
    }
    
    fun onBiometricPromptDismissed() {
        _uiState.update { it.copy(showBiometricPrompt = false, isLoading = false) }
    }

    private fun refreshKeyStatus() {
        launch {
            val didExists = didKeyManager.keysExist()
            val did = if (didExists) runCatching { didKeyManager.getDID() }.getOrDefault("") else ""
            val keyId = if (didExists) runCatching { didKeyManager.getKeyId() }.getOrDefault("") else ""
            val didLevel = if (didExists) didKeyManager.getSecurityLevel() else SecurityLevel.UNKNOWN

            _uiState.update {
                it.copy(
                    didKeysExist = didExists,
                    did = did,
                    keyId = keyId,
                    didSecurityLevel = didLevel
                )
            }
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { block() }
                .onFailure { e ->
                    val cause = e.cause
                    // LOG EXTRA PARA DEPURACIÓN
                    AppLogger.d("did-vm", "Exception caught in launch: $e, Cause: $cause")
                    
                    if (e is KeyPermanentlyInvalidatedException || cause is KeyPermanentlyInvalidatedException) {
                        AppLogger.e("did-vm", "Clave invalidada permanentemente por cambios biométricos")
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                statusMessage = "Claves invalidadas. Se detectaron cambios biométricos. Por favor, regenera las claves."
                            ) 
                        }
                        pendingAction = null
                        
                    } else if (e is UserNotAuthenticatedException || cause is UserNotAuthenticatedException) {
                        AppLogger.w("did-vm", "Se requiere autenticación biométrica")
                        pendingAction = block
                        _uiState.update { it.copy(isLoading = false, showBiometricPrompt = true) }
                    } else {
                        AppLogger.e("did-vm", "Error en launch: ${e.message}", e)
                        _uiState.update { it.copy(isLoading = false, statusMessage = "Error: ${e.message}") }
                    }
                }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    companion object {
        private const val CREDENTIAL_ID = "demo_vc"
    }
}