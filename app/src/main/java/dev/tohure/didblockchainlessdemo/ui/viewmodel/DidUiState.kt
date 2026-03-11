package dev.tohure.didblockchainlessdemo.ui.viewmodel

import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel

data class DidUiState(
    // ── Identidad DID (secp256k1) ────────────────────────────────────
    val didKeysExist: Boolean = false,
    val did: String = "",
    val keyId: String = "",
    val didSecurityLevel: SecurityLevel = SecurityLevel.UNKNOWN,

    // ── Flujo de emisión (nonce → ProofJWT) ──────────────────────────
    val lastProofJwt: String = "",
    
    // ── Credencial y Metadatos ───────────────────────────────────────
    val encryptedCredential: String = "", // Payload cifrado (AES-GCM)
    val decryptedMetadata: String = "",   // JSON de metadatos (para mostrar)

    // ── Validación ───────────────────────────────────────────────────
    val validationResponseJson: String = "",

    // ── Estado general ───────────────────────────────────────────────
    val statusMessage: String = "",
    val isLoading: Boolean = false
)