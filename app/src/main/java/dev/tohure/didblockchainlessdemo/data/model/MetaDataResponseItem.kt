package dev.tohure.didblockchainlessdemo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
[
{
"credential_id": "urn:uuid:85fd28ce-3477-4137-8f48-6b3ff294b734",
"credential_type": "UniversityDegreeCredential",
"expires_at": "2026-03-10T23:09:39Z",
"issued_at": "2026-03-09T23:09:39Z",
"revoked": false
}
]
 */
@Serializable
data class MetaDataResponseItem(
    @SerialName("credential_id")
    val credentialId: String,
    @SerialName("credential_type")
    val credentialType: String,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("issued_at")
    val issuedAt: String,
    @SerialName("revoked")
    val revoked: Boolean
)