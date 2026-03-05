package dev.tohure.didblockchainlessdemo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifiableCredentialResponse(
    @SerialName("@context") val context: List<String>,
    val type: List<String>,
    val issuer: String,
    val credentialSubject: CredentialSubject
)

@Serializable
data class CredentialSubject(
    val id: String,
    val name: String,
    val degree: Degree? = null
)

@Serializable
data class Degree(
    val type: String,
    val name: String
)