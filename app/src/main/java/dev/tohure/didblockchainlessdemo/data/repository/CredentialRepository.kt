package dev.tohure.didblockchainlessdemo.data.repository

import dev.tohure.didblockchainlessdemo.data.model.DIDRegisterRequest
import dev.tohure.didblockchainlessdemo.data.model.IssueVCRequest
import dev.tohure.didblockchainlessdemo.data.model.IssueVCResponse
import dev.tohure.didblockchainlessdemo.data.model.MetaDataResponseItem
import dev.tohure.didblockchainlessdemo.data.model.ValidateVpRequest
import dev.tohure.didblockchainlessdemo.data.model.ValidateVpResponse
import dev.tohure.didblockchainlessdemo.data.model.VerifiableCredentialResponse
import dev.tohure.didblockchainlessdemo.data.network.CredentialApi
import dev.tohure.didblockchainlessdemo.data.network.NetworkClient
import dev.tohure.didblockchainlessdemo.utils.AppLogger
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
        }.onFailure { e ->
            AppLogger.e("repository", "Error fetchCredential: ${e.message}", e)
        }

    /**
     * Registra el DID del holder en el backend.
     */
    suspend fun registerDid(did: String, clientId: String): Result<Unit> =
        runCatching {
            api.registerDID(DIDRegisterRequest(clientId = clientId, did = did))
            Unit
        }.onFailure { e ->
            AppLogger.e("repository", "Error registerDid: ${e.message}", e)
        }

    /**
     * Solicita un nonce de un solo uso al backend.
     * Se usa para construir el Proof JWT antes de solicitar la emisión de una VC.
     *
     * @param holderDid DID del holder (did:key:z...) generado en el dispositivo.
     */
    suspend fun fetchNonce(holderDid: String): Result<String> =
        runCatching { api.getNonce(holderDid).nonce }
            .onFailure { e ->
                AppLogger.e("repository", "Error fetchNonce: ${e.message}", e)
            }

    /**
     * Envía el Proof JWT al backend para obtener la credencial.
     */
    suspend fun registerProof(did: String, proof: String): Result<IssueVCResponse> =
        runCatching {
            api.registerProof(IssueVCRequest(holderDid = did, proof = proof))
        }.onFailure { e ->
            AppLogger.e("repository", "Error registerProof: ${e.message}", e)
        }

    /**
     * Obtiene los metadatos de las credenciales asociadas a un DID.
     */
    suspend fun getMetaDataCredential(holderDid: String): Result<List<MetaDataResponseItem>> =
        runCatching {
            api.getMetaDataCredential(holderDid)
        }.onFailure { e ->
            AppLogger.e("repository", "Error getMetaDataCredential: ${e.message}", e)
        }

    /**
     * Valida una Verifiable Presentation (VP) en formato JWT.
     */
    suspend fun validateCredentials(vpJwt: String): Result<ValidateVpResponse> =
        runCatching {
            api.validateCredentials(ValidateVpRequest(vpJwt = vpJwt))
        }.onFailure { e ->
            AppLogger.e("repository", "Error validateCredentials: ${e.message}", e)
        }
}