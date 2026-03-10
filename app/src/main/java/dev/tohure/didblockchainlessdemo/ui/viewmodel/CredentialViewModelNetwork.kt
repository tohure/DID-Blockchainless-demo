package dev.tohure.didblockchainlessdemo.ui.viewmodel

import androidx.lifecycle.viewModelScope
import dev.tohure.didblockchainlessdemo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
                isFetching = true, statusMessage = "Iniciando proceso..."
            )
        }

        runCatching {
            check(didKeyManager.keysExist()) { "Primero genera las claves DID" }

            val did = didKeyManager.getDID()
            val clientId =
                subjectClaims["email"] ?: error("El email es requerido para registrar el DID")

            _uiState.update { it.copy(statusMessage = "Registrando DID...") }
            repository.registerDid(did, clientId).getOrThrow()

            _uiState.update { it.copy(statusMessage = "Solicitando nonce...") }
            val nonce = repository.fetchNonce(holderDid = did).getOrThrow()

            val proofJwt = proofBuilder.build(issuerUrl, nonce, credentialType, subjectClaims)

            _uiState.update {
                it.copy(
                    lastProofJwt = proofJwt,
                    statusMessage = "Enviando Proof JWT..."
                )
            }

            repository.registerProof(did, proofJwt).getOrThrow()

            _uiState.update { it.copy(statusMessage = "Obteniendo metadatos...") }
            val metadata = repository.getMetaDataCredential(did).getOrThrow()
            val metadataJson = Json.encodeToString(metadata)

            val payload = performEncryption(metadataJson, false)

            Triple(proofJwt, metadataJson, payload)

        }.onSuccess { (proofJwt, metadataJson, payload) ->
            _uiState.update {
                it.copy(
                    isFetching = false,
                    lastProofJwt = proofJwt,
                    jsonInput = metadataJson,
                    decryptedJson = metadataJson,
                    encryptedPayload = payload,
                    statusMessage = "Metadatos recibidos y cifrados correctamente",
                )
            }
        }.onFailure { e ->
            _uiState.update {
                it.copy(
                    isFetching = false,
                    statusMessage = "Error: ${e.message ?: "Error desconocido"}",
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

        runCatching {
            val json = repository.fetchCredential(credentialId, token).getOrThrow()
            _uiState.update { it.copy(jsonInput = json) }

            val payload = performEncryption(json, false)
            payload

        }.onSuccess { payload ->
            _uiState.update {
                it.copy(
                    isFetching = false,
                    encryptedPayload = payload,
                    decryptedJson = "",
                    statusMessage = "JSON cifrado y guardado con AES-256-GCM + RSA-OAEP",
                )
            }
        }.onFailure { e ->
            _uiState.update {
                it.copy(
                    isFetching = false, statusMessage = "Error: ${e.message ?: "Error desconocido"}"
                )
            }
        }
    }
}