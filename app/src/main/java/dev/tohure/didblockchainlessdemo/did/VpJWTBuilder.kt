package dev.tohure.didblockchainlessdemo.did

import dev.tohure.didblockchainlessdemo.utils.toBase64Url
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import java.time.Instant

/**
 * Construye un Verifiable Presentation (VP) en formato JWT.
 *
 * El JWT resultante contiene un claim `vp` que a su vez contiene la credencial
 * verificable que se quiere presentar.
 */
class VpJWTBuilder(private val didKeyManager: DIDKeyManager) {

    fun build(
        verifiableCredentialJwt: String,
        audience: String
    ): String {
        val now = Instant.now().epochSecond

        val header = buildJsonObject {
            put("alg", "ES256K")
            put("typ", "JWT")
            put("kid", didKeyManager.getKeyId())
        }

        val payload = buildJsonObject {
            put("iss", didKeyManager.getDID())
            put("aud", audience)
            put("iat", now + 700)
            put("exp", now + 700)
            put("vp", buildJsonObject {
                putJsonArray("@context") { add("https://www.w3.org/2018/credentials/v1") }
                putJsonArray("type") { add("VerifiablePresentation") }
                putJsonArray("verifiableCredential") {
                    add(verifiableCredentialJwt)
                }
            })
        }

        val headerB64 = header.toString().encodeToByteArray().toBase64Url()
        val payloadB64 = payload.toString().encodeToByteArray().toBase64Url()
        val signingInput = "$headerB64.$payloadB64"
        
        val signature = didKeyManager.sign(signingInput.encodeToByteArray()).getOrThrow()
        val signatureB64 = signature.toBase64Url()

        return "$headerB64.$payloadB64.$signatureB64"
    }
}