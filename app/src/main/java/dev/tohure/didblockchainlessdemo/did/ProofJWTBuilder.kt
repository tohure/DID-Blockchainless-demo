package dev.tohure.didblockchainlessdemo.did

import android.util.Base64
import kotlinx.serialization.json.Json
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
            put("iat", now)
            put("exp", now + 300)
            put("nonce", nonce)
            put("credential_type", credentialType)
            put("subject_claims", buildJsonObject {
                subjectClaims.forEach { (k, v) -> put(k, v) }
            })
        }

        val headerB64 = base64url(Json.encodeToString(header).toByteArray())
        val payloadB64 = base64url(Json.encodeToString(payload).toByteArray())
        val signingInput = "$headerB64.$payloadB64"

        val signature = didKeyManager.sign(signingInput.toByteArray(Charsets.UTF_8)).getOrThrow()
        val sigB64 = base64url(signature)

        return "$signingInput.$sigB64"
    }

    private fun base64url(data: ByteArray): String =
        Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}