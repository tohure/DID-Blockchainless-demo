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
data class Subject(
    @SerialName("id")
    val id: String,
    @SerialName("email")
    val email: String,
    @SerialName("familyName")
    val familyName: String,
    @SerialName("givenName")
    val givenName: String
)