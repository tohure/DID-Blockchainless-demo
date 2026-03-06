package dev.tohure.didblockchainlessdemo.ui.viewmodel

import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel
import kotlinx.coroutines.flow.update

fun CredentialViewModel.generateDIDKeys() = launchCrypto {
    val generated = didKeyManager.generateKeysIfNeeded()
    val level = didKeyManager.getSecurityLevel()
    val did = didKeyManager.getDID()
    val keyId = didKeyManager.getKeyId()
    val msg = if (generated) "Identidad DID creada (secp256k1) en: ${level.name}"
    else "Las claves DID ya existían"
    _uiState.update {
        it.copy(
            didKeysExist = true,
            did = did,
            keyId = keyId,
            didSecurityLevel = level,
            statusMessage = msg,
        )
    }
}

fun CredentialViewModel.deleteDIDKeys() = launchCrypto {
    didKeyManager.deleteKeys()
    _uiState.update {
        it.copy(
            didKeysExist = false,
            did = "",
            keyId = "",
            didSecurityLevel = SecurityLevel.UNKNOWN,
            lastProofJwt = "",
            statusMessage = "Claves DID eliminadas",
        )
    }
}