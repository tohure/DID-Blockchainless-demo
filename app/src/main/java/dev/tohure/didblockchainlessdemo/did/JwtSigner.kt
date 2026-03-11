package dev.tohure.didblockchainlessdemo.did

import dev.tohure.didblockchainlessdemo.utils.toBase64Url
import kotlinx.serialization.json.JsonObject

/** Tiempo de validez de los JWTs emitidos (segundos). */
internal const val JWT_EXPIRY_SECONDS = 300L

/**
 * Construye y firma un JWT dado su [header] y [payload].
 *
 * Centraliza el patrón `header.b64 + "." + payload.b64 → sign → JWT completo`
 * compartido por [ProofJWTBuilder] y [VpJWTBuilder].
 *
 * @return JWT en el formato estándar `header.payload.signature`.
 * @throws Exception si la firma falla (propagado desde [DIDKeyManager.sign]).
 */
internal fun buildSignedJwt(
    header: JsonObject,
    payload: JsonObject,
    didKeyManager: DIDKeyManager,
): String {
    val headerB64  = header.toString().encodeToByteArray().toBase64Url()
    val payloadB64 = payload.toString().encodeToByteArray().toBase64Url()
    val signingInput = "$headerB64.$payloadB64"

    val signature = didKeyManager.sign(signingInput.encodeToByteArray()).getOrThrow()
    return "$signingInput.${signature.toBase64Url()}"
}
