package dev.tohure.didblockchainlessdemo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
{
"credential_id":"urn:uuid:55e80395-b4ad-468b-92ee-d65dcbc71cd7",
"credential_type":"UniversityDegreeCredential",
"subject":{
"id":"did:key:zQ3shtsqCASKiQnhtTVpYax7XWPHDtDfdsAJF1xhJa5nE3Rqr",
"givenName":"Juan",
"familyName":"Perez",
"email":"juan@example.com"
},
"expires_at":"2026-03-11T21:30:59Z",
"revoked":false
}
 */
@Serializable
data class Credential(
    @SerialName("credential_id")
    val credentialId: String,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("subject")
    val subject: Subject,
    @SerialName("credential_type")
    val credentialType: String,
    @SerialName("revoked")
    val revoked: Boolean,
)