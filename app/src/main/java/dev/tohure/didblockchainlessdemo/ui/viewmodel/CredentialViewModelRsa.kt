package dev.tohure.didblockchainlessdemo.ui.viewmodel

import dev.tohure.didblockchainlessdemo.ui.viewmodel.CredentialViewModel.Companion.CREDENTIAL_ID
import dev.tohure.didblockchainlessdemo.utils.ValidationUtils
import kotlinx.coroutines.flow.update

fun CredentialViewModel.generateRsaKeys() = launchCrypto {
    val generated = crypto.generateKeyPairIfNeeded()
    val level = crypto.getSecurityLevel()
    val pub = crypto.getPublicKeyBase64()
    val msg = if (generated) "Par RSA-2048 creado en el Keystore en: ${level.name}"
    else "Las claves RSA ya existían"
    _uiState.update {
        it.copy(
            rsaKeyExists = true,
            publicKeyBase64 = pub,
            rsaSecurityLevel = level,
            statusMessage = msg,
        )
    }
}

fun CredentialViewModel.deleteRsaKeys() = launchCrypto {
    crypto.deleteKeyPair()
    store.clear()
    _uiState.update {
        it.copy(
            rsaKeyExists = false,
            publicKeyBase64 = "",
            encryptedPayload = "",
            decryptedJson = "",
            statusMessage = "Claves RSA y credenciales cifradas eliminadas",
        )
    }
}

/**
 * Lógica de cifrado reutilizable y síncrona.
 * Lanza excepciones si falla, para ser capturadas por quien la llame.
 */
internal fun CredentialViewModel.performEncryption(
    input: String,
    validateJson: Boolean = true
): String {
    check(crypto.keyPairExists()) { "Primero genera las claves RSA" }
    require(input.isNotBlank()) { "El contenido no puede estar vacío" }

    if (validateJson && input.trim().startsWith("{")) {
        ValidationUtils.validateJson(input)
    }

    val payload = crypto.encrypt(input)
    store.save(CREDENTIAL_ID, payload)
    return payload
}

fun CredentialViewModel.encrypt() = launchCrypto {
    val json = _uiState.value.jsonInput

    val payload = performEncryption(json)

    _uiState.update {
        it.copy(
            encryptedPayload = payload,
            decryptedJson = "",
            statusMessage = "Contenido cifrado y guardado con AES-256-GCM + RSA-OAEP",
        )
    }
}

fun CredentialViewModel.decrypt() = launchCrypto {
    check(crypto.keyPairExists()) { "Primero genera las claves RSA" }
    val payload =
        store.load(CREDENTIAL_ID) ?: error("No hay ninguna credencial guardada. Cifra primero.")

    val json = crypto.decrypt(payload)

    _uiState.update {
        it.copy(
            decryptedJson = json,
            statusMessage = "Contenido descifrado correctamente con la clave privada del Keystore",
        )
    }
}