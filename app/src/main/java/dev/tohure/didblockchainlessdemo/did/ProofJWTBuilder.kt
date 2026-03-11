package dev.tohure.didblockchainlessdemo.did

import dev.tohure.didblockchainlessdemo.utils.toBase64Url
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

class ProofJWTBuilder(private val didKeyManager: DIDKeyManager) {

    private val MINUTES_EXP = 300 //5 MINUTOS

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
            put("iat", now + MINUTES_EXP)
            put("exp", now + MINUTES_EXP)
            put("nonce", nonce)
            put("credential_type", credentialType)
            put("subject_claims", buildJsonObject {
                subjectClaims.forEach { (k, v) -> put(k, v) }
            })
        }

        val headerB64 = header.toString().encodeToByteArray().toBase64Url()
        val payloadB64 = payload.toString().encodeToByteArray().toBase64Url()
        val signingInput = "$headerB64.$payloadB64"

        val signature = didKeyManager.sign(signingInput.encodeToByteArray()).getOrThrow()
        val sigB64 = signature.toBase64Url()

        return "$signingInput.$sigB64"
    }
}