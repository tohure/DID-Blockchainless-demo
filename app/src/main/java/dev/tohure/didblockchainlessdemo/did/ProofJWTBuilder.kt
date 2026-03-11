package dev.tohure.didblockchainlessdemo.did

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

class ProofJWTBuilder(private val didKeyManager: DIDKeyManager) {

    fun build(
        issuerUrl: String, nonce: String, credentialType: String, subjectClaims: Map<String, String>
    ): String {
        val did = didKeyManager.getDID()
        val kid = didKeyManager.getKeyId()
        val now = Instant.now().epochSecond

        val header = buildJsonObject {
            put("alg", "ES256K")
            put("typ", "openid4vci-proof+jwt")
            put("kid", kid)
        }

        val payload = buildJsonObject {
            put("iss", did)
            put("aud", issuerUrl)
            put("iat", now) // momento de emisión (RFC 7519)
            put("exp", now + JWT_EXPIRY_SECONDS)
            put("nonce", nonce)
            put("credential_type", credentialType)
            put("subject_claims", buildJsonObject {
                subjectClaims.forEach { (k, v) -> put(k, v) }
            })
        }

        return buildSignedJwt(header, payload, didKeyManager)
    }
}