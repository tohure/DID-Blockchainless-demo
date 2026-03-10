package dev.tohure.didblockchainlessdemo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
{
"holderDid": "{{HOLDER_DID}}",
"proof": "{{PROOF_JWT}}"
}
 */
@Serializable
data class IssueVCRequest(
    @SerialName("holderDid")
    val holderDid: String,
    @SerialName("proof")
    val proof: String
)