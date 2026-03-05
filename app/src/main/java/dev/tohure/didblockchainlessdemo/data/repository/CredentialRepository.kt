package dev.tohure.didblockchainlessdemo.data.repository

import dev.tohure.didblockchainlessdemo.data.model.VerifiableCredentialResponse
import dev.tohure.didblockchainlessdemo.data.network.CredentialApi
import dev.tohure.didblockchainlessdemo.data.network.NetworkClient
import kotlinx.serialization.json.Json

class CredentialRepository(
    private val api: CredentialApi = NetworkClient.credentialApi
) {
    /**
     * Descarga la credencial y la retorna como String JSON serializado.
     */
    suspend fun fetchCredential(id: String, token: String): Result<String> =
        runCatching {
            val response = api.getCredential(id, "Bearer $token")
            Json.encodeToString(VerifiableCredentialResponse.serializer(), response)
        }

    /**
     * Solicita un nonce de un solo uso al backend.
     * Se usa para construir el Proof JWT antes de solicitar la emisión de una VC.
     */
    suspend fun fetchNonce(): Result<String> =
        runCatching { api.getNonce().nonce }
}