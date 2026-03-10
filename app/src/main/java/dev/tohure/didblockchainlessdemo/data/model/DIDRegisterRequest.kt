package dev.tohure.didblockchainlessdemo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
{
"client_id": "{{CLIENT_ID}}",
"did": "{{HOLDER_DID}}"
}
 */
@Serializable
data class DIDRegisterRequest(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("did")
    val did: String
)