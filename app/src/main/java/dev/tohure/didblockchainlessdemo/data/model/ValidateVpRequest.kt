package dev.tohure.didblockchainlessdemo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
{
"vp_jwt": "{{VP_JWT}}"
}
 */
@Serializable
data class ValidateVpRequest(
    @SerialName("vp_jwt")
    val vpJwt: String
)