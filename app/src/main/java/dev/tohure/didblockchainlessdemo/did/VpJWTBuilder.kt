package dev.tohure.didblockchainlessdemo.did

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
            put("iat", now) // momento de emisión (RFC 7519)
            put("exp", now + JWT_EXPIRY_SECONDS)
            put("vp", buildJsonObject {
                putJsonArray("@context") { add("https://www.w3.org/2018/credentials/v1") }
                putJsonArray("type") { add("VerifiablePresentation") }
                putJsonArray("verifiableCredential") {
                    add(verifiableCredentialJwt)
                }
            })
        }

        return buildSignedJwt(header, payload, didKeyManager)
    }
}