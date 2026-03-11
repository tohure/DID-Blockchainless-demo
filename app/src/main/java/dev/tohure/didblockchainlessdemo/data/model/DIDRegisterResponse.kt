package dev.tohure.didblockchainlessdemo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
{
"did": "did:key:zQer2354gfdgdf",
"client_id": "user@example.com",
"active": true,
"registered_at": "2026-03-09T20:48:34.031907834Z"
}
 */
@Serializable
data class DIDRegisterResponse(
    @SerialName("active")
    val active: Boolean,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("did")
    val did: String,
    @SerialName("registered_at")
    val registeredAt: String
)