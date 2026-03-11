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
data class ValidateVpResponse(
    @SerialName("credentials")
    val credentials: List<Credential>,
    @SerialName("holder_did")
    val holderDid: String,
    @SerialName("valid")
    val valid: Boolean
)