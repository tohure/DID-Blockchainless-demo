package dev.tohure.didblockchainlessdemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CredentialUiState(
    val keyExists: Boolean = false,
    val publicKeyBase64: String = "",
    val jsonInput: String = DEFAULT_JSON,
    val encryptedPayload: String = "",
    val decryptedJson: String = "",
    val statusMessage: String = "",
    val isLoading: Boolean = false,
    val securityLevel: SecurityLevel = SecurityLevel.UNKNOWN
) {
    companion object {
        val DEFAULT_JSON = """{
  "@context": ["https://www.w3.org/2018/credentials/v1"],
  "type": ["VerifiableCredential"],
  "issuer": "did:example:123",
  "credentialSubject": {
    "id": "did:example:456",
    "name": "Ada Lovelace",
    "degree": {
      "type": "BachelorDegree",
      "name": "Computer Science"
    }
  }
}""".trimIndent()
    }
}

class CredentialViewModel(application: Application) : AndroidViewModel(application) {

    private val crypto = CryptoManager()
    private val store = CredentialStore(application)

    private val _uiState = MutableStateFlow(CredentialUiState())
    val uiState: StateFlow<CredentialUiState> = _uiState.asStateFlow()

    init {
        refreshKeyStatus()
    }

    fun generateKeys() = launchCrypto("Claves generadas ✓") {
        val generated = crypto.generateKeyPairIfNeeded()
        val level = crypto.getSecurityLevel()
        val msg =
            if (generated) "Par RSA-2048 creado en el Keystore en: ${level.name}" else "Las claves ya existían"
        val pub = crypto.getPublicKeyBase64()
        _uiState.update {
            it.copy(
                keyExists = true, publicKeyBase64 = pub, securityLevel = level, statusMessage = msg
            )
        }
    }

    fun deleteKeys() = launchCrypto("Claves eliminadas") {
        crypto.deleteKeyPair()
        store.clear()
        _uiState.update {
            it.copy(
                keyExists = false,
                publicKeyBase64 = "",
                encryptedPayload = "",
                decryptedJson = "",
                statusMessage = "Claves y credenciales eliminadas del Keystore"
            )
        }
    }

    fun encrypt() = launchCrypto("JSON cifrado ✓") {
        requireKeys()
        val json = _uiState.value.jsonInput
        require(json.isNotBlank()) { "El JSON no puede estar vacío" }
        val payload = crypto.encrypt(json)
        store.save(CREDENTIAL_ID, payload)
        _uiState.update {
            it.copy(
                encryptedPayload = payload,
                decryptedJson = "",
                statusMessage = "JSON cifrado y guardado con AES-256-GCM + RSA-OAEP"
            )
        }
    }

    fun decrypt() = launchCrypto("JSON descifrado ✓") {
        requireKeys()
        val payload =
            store.load(CREDENTIAL_ID) ?: error("No hay ninguna credencial guardada. Cifra primero.")
        val json = crypto.decrypt(payload)
        _uiState.update {
            it.copy(
                decryptedJson = json,
                statusMessage = "JSON descifrado correctamente con la clave privada del Keystore"
            )
        }
    }

    fun updateJsonInput(value: String) {
        _uiState.update { it.copy(jsonInput = value) }
    }

    fun clearDecryptedJson() {
        _uiState.update { it.copy(decryptedJson = "", statusMessage = "Texto limpiado") }
    }

    private fun refreshKeyStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = crypto.keyPairExists()
            val pub =
                if (exists) runCatching { crypto.getPublicKeyBase64() }.getOrDefault("") else ""
            _uiState.update { it.copy(keyExists = exists, publicKeyBase64 = pub) }
        }
    }

    private fun requireKeys() {
        check(crypto.keyPairExists()) { "Primero debes generar las claves" }
    }

    private fun launchCrypto(successHint: String = "", block: suspend () -> Unit) {
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