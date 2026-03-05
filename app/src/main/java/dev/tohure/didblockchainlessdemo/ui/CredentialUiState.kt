package dev.tohure.didblockchainlessdemo.ui

import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel

data class CredentialUiState(
    // ── Identidad DID (secp256k1) ────────────────────────────────────
    val didKeysExist: Boolean = false,
    val did: String = "",
    val keyId: String = "",
    val didSecurityLevel: SecurityLevel = SecurityLevel.UNKNOWN,

    // ── Almacén de VCs (RSA) ─────────────────────────────────────────
    val rsaKeyExists: Boolean = false,
    val publicKeyBase64: String = "",
    val rsaSecurityLevel: SecurityLevel = SecurityLevel.UNKNOWN,

    // ── Credencial ───────────────────────────────────────────────────
    val jsonInput: String = DEFAULT_JSON,
    val encryptedPayload: String = "",
    val decryptedJson: String = "",

    // ── Flujo de emisión (nonce → ProofJWT) ──────────────────────────
    val lastProofJwt: String = "",

    // ── Estado general ───────────────────────────────────────────────
    val statusMessage: String = "",
    val isLoading: Boolean = false,
    val isFetching: Boolean = false,
) {
    companion object {
        val DEFAULT_JSON = """
        {
          "@context": ["https://www.w3.org/2018/credentials/v1"],
          "type": ["VerifiableCredential"],
          "issuer": "did:example:123",
          "credentialSubject": {
            "id": "did:example:456",
            "name": "Ada Lovelace",
            "degree": {
              "type": "BachelorDegree",
              "name": "Computer Science"
            }
          }
        }""".trimIndent()
    }
}