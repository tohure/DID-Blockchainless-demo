package dev.tohure.didblockchainlessdemo.ui.viewmodel

import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel

data class RsaUiState(
    // ── Almacén de VCs (RSA) ─────────────────────────────────────────
    val rsaKeyExists: Boolean = false,
    val publicKeyBase64: String = "",
    val rsaSecurityLevel: SecurityLevel = SecurityLevel.UNKNOWN,

    // ── Cifrado/Descifrado Manual ────────────────────────────────────
    val jsonInput: String = DEFAULT_JSON,
    val encryptedPayload: String = "",
    val decryptedJson: String = "",

    // ── Estado general ───────────────────────────────────────────────
    val statusMessage: String = "",
    val isLoading: Boolean = false
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