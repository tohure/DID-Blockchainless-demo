package dev.tohure.didblockchainlessdemo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tohure.didblockchainlessdemo.SecureCredentialsApp
import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel
import dev.tohure.didblockchainlessdemo.data.repository.CredentialRepository
import dev.tohure.didblockchainlessdemo.did.ProofJWTBuilder
import dev.tohure.didblockchainlessdemo.storage.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CredentialViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SecureCredentialsApp
    internal val didKeyManager = app.didKeyManager // DID (secp256k1)
    internal val crypto = app.cryptoManager // cifrado VCs (RSA)
    internal val store = CredentialStore(application)
    internal val repository = CredentialRepository()
    internal val proofBuilder get() = ProofJWTBuilder(didKeyManager)

    internal val _uiState = MutableStateFlow(CredentialUiState())
    val uiState: StateFlow<CredentialUiState> = _uiState.asStateFlow()

    init {
        refreshAllKeyStatuses()
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

    internal fun launchCrypto(block: suspend () -> Unit) {
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
        internal const val CREDENTIAL_ID = "demo_vc"
    }
}