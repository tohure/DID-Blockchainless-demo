package dev.tohure.didblockchainlessdemo.data.network

import dev.tohure.didblockchainlessdemo.data.model.DIDRegisterRequest
import dev.tohure.didblockchainlessdemo.data.model.DIDRegisterResponse
import dev.tohure.didblockchainlessdemo.data.model.IssueVCRequest
import dev.tohure.didblockchainlessdemo.data.model.IssueVCResponse
import dev.tohure.didblockchainlessdemo.data.model.MetaDataResponseItem
import dev.tohure.didblockchainlessdemo.data.model.NonceResponse
import dev.tohure.didblockchainlessdemo.data.model.ValidateVpRequest
import dev.tohure.didblockchainlessdemo.data.model.ValidateVpResponse
import dev.tohure.didblockchainlessdemo.data.model.VerifiableCredentialResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CredentialApi {

    /** Descarga una VC por su ID. */
    @GET("credentials/{id}")
    suspend fun getCredential(
        @Path("id") credentialId: String,
        @Header("Authorization") token: String,
    ): VerifiableCredentialResponse

    /** Registro DID para su posterior validación. */
    @POST("dids/register")
    suspend fun registerDID(
        @Body request: DIDRegisterRequest,
    ): DIDRegisterResponse

    /**
     * Solicita un nonce de un solo uso al backend para incluirlo en el Proof JWT.
     * El backend vincula el nonce al [holderDid] para evitar ataques de replay.
     */
    @GET("credentials/nonce")
    suspend fun getNonce(
        @Query("holder_did") holderDid: String,
    ): NonceResponse

    /** Registro proof con el JWT y DID. */
    @POST("credentials/issue")
    suspend fun registerProof(
        @Body request: IssueVCRequest,
    ): IssueVCResponse

    @GET("credentials")
    suspend fun getMetaDataCredential(
        @Query("holder_did") holderDid: String
    ): List<MetaDataResponseItem>

    /** Validación de credenciales JWT. */
    @POST("credentials/verify")
    suspend fun validateCredentials(
        @Body request: ValidateVpRequest,
    ): ValidateVpResponse
}