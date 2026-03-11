package dev.tohure.didblockchainlessdemo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
{
"credential": "eyJWT",
"credentialId": "uuid-credential"
}
 */
@Serializable
data class IssueVCResponse(
    @SerialName("credential")
    val credential: String,
    @SerialName("credentialId")
    val credentialId: String
)