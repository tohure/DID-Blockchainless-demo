package dev.tohure.didblockchainlessdemo.data.network

import dev.tohure.didblockchainlessdemo.data.model.NonceResponse
import dev.tohure.didblockchainlessdemo.data.model.VerifiableCredentialResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface CredentialApi {

    /** Descarga una VC por su ID. */
    @GET("credentials/{id}")
    suspend fun getCredential(
        @Path("id") credentialId: String,
        @Header("Authorization") token: String,
    ): VerifiableCredentialResponse

    /**
     * Solicita un nonce de un solo uso al backend para incluirlo en el Proof JWT.
     * Este nonce evita ataques de replay al vincular el Proof JWT a una sesión concreta.
     */
    @GET("credentials/nonce")
    suspend fun getNonce(): NonceResponse
}