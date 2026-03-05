package dev.tohure.didblockchainlessdemo.ui.viewmodel

import androidx.lifecycle.viewModelScope
import dev.tohure.didblockchainlessdemo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun CredentialViewModel.requestCredentialWithNonce(
    issuerUrl: String = BuildConfig.BASE_URL,
    credentialType: String = "UniversityDegreeCredential",
    subjectClaims: Map<String, String> = mapOf(
        "givenName" to "Juan",
        "familyName" to "Perez",
        "email" to "juan@example.com"
    ),
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

fun CredentialViewModel.fetchAndEncrypt(credentialId: String, token: String) {
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