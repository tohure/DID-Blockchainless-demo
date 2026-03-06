package dev.tohure.didblockchainlessdemo.data.network

import dev.tohure.didblockchainlessdemo.data.model.NonceResponse
import dev.tohure.didblockchainlessdemo.data.model.VerifiableCredentialResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface CredentialApi {

    /** Descarga una VC por su ID. */
    @GET("credentials/{id}")
    suspend fun getCredential(
        @Path("id") credentialId: String,
        @Header("Authorization") token: String,
    ): VerifiableCredentialResponse

    /**
     * Solicita un nonce de un solo uso al backend para incluirlo en el Proof JWT.
     * El backend vincula el nonce al [holderDid] para evitar ataques de replay.
     */
    @GET("credentials/nonce")
    suspend fun getNonce(
        @Query("holder_did") holderDid: String,
    ): NonceResponse
}