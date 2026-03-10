package dev.tohure.didblockchainlessdemo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
{
"valid": true,
"holder_did": "did:key:zQ3sh...",
"credentials": [
{
"credential_id": "urn:uuid:...",
"type": "UniversityDegreeCredential",
"subject": { "givenName": "Ana", "familyName": "García", "email": "ana@example.com" },
"expires_at": "2027-..."
}
]
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
    @SerialName("type")
    val type: String
)