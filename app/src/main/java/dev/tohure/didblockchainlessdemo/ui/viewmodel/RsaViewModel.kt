package dev.tohure.didblockchainlessdemo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tohure.didblockchainlessdemo.crypto.CryptoManager
import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel
import dev.tohure.didblockchainlessdemo.data.repository.CredentialRepository
import dev.tohure.didblockchainlessdemo.storage.CredentialStore
import dev.tohure.didblockchainlessdemo.utils.AppLogger
import dev.tohure.didblockchainlessdemo.utils.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RsaViewModel(application: Application) : AndroidViewModel(application) {

    private val crypto = CryptoManager()
    private val store = CredentialStore(application)
    private val repository = CredentialRepository()

    private val _uiState = MutableStateFlow(RsaUiState())
    val uiState: StateFlow<RsaUiState> = _uiState.asStateFlow()

    init {
        refreshKeyStatus()
    }

    fun generateRsaKeys() = launch {
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

    fun deleteRsaKeys() = launch {
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

    fun updateJsonInput(value: String) = _uiState.update { it.copy(jsonInput = value) }

    fun clearDecryptedJson() =
        _uiState.update { it.copy(decryptedJson = "", statusMessage = "Texto limpiado") }

    fun fetchAndEncrypt(credentialId: String, token: String) {
        launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Descargando credencial...") }
            
            val json = repository.fetchCredential(credentialId, token).getOrThrow()
            _uiState.update { it.copy(jsonInput = json) }

            val payload = performEncryption(json, validateJson = false)
            
            _uiState.update {
                it.copy(
                    encryptedPayload = payload,
                    decryptedJson = "",
                    statusMessage = "JSON cifrado y guardado con AES-256-GCM + RSA-OAEP",
                )
            }
        }
    }

    fun encrypt() = launch {
        val json = _uiState.value.jsonInput
        val payload = performEncryption(json, validateJson = true)

        _uiState.update {
            it.copy(
                encryptedPayload = payload,
                decryptedJson = "",
                statusMessage = "Contenido cifrado y guardado con AES-256-GCM + RSA-OAEP",
            )
        }
    }

    fun decrypt() = launch {
        check(crypto.keyPairExists()) { "Primero genera las claves RSA" }
        val payload = store.load(CREDENTIAL_ID) ?: error("No hay ninguna credencial guardada. Cifra primero.")
        val json = crypto.decrypt(payload)

        _uiState.update {
            it.copy(
                decryptedJson = json,
                statusMessage = "Contenido descifrado correctamente con la clave privada del Keystore",
            )
        }
    }

    private fun performEncryption(input: String, validateJson: Boolean): String {
        check(crypto.keyPairExists()) { "Primero genera las claves RSA" }
        require(input.isNotBlank()) { "El contenido no puede estar vacío" }

        if (validateJson && input.trim().startsWith("{")) {
            ValidationUtils.validateJson(input)
        }

        val payload = crypto.encrypt(input)
        store.save(CREDENTIAL_ID, payload)
        return payload
    }

    private fun refreshKeyStatus() {
        launch {
            val rsaExists = crypto.keyPairExists()
            val pub = if (rsaExists) runCatching { crypto.getPublicKeyBase64() }.getOrDefault("") else ""
            val rsaLevel = if (rsaExists) crypto.getSecurityLevel() else SecurityLevel.UNKNOWN

            _uiState.update {
                it.copy(
                    rsaKeyExists = rsaExists,
                    publicKeyBase64 = pub,
                    rsaSecurityLevel = rsaLevel,
                )
            }
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { block() }
                .onFailure { e ->
                    AppLogger.e("rsa-vm", "Error en launch: ${e.message}", e)
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Error: ${e.message}") }
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