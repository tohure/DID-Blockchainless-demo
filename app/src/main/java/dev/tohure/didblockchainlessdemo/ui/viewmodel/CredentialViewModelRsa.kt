package dev.tohure.didblockchainlessdemo.ui.viewmodel

import android.util.Log
import dev.tohure.didblockchainlessdemo.ui.viewmodel.CredentialViewModel.Companion.CREDENTIAL_ID
import kotlinx.coroutines.flow.update
import org.json.JSONObject

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

fun CredentialViewModel.encrypt() = launchCrypto {
    check(crypto.keyPairExists()) { "Primero genera las claves RSA" }
    val json = _uiState.value.jsonInput
    require(json.isNotBlank()) { "El JSON no puede estar vacío" }

    try {
        JSONObject(json)
    } catch (e: Exception) {
        Log.e("tohure-did", "encrypt: $e")
        throw IllegalArgumentException("El texto no es un JSON válido")
    }

    val payload = crypto.encrypt(json)
    store.save(CREDENTIAL_ID, payload)
    _uiState.update {
        it.copy(
            encryptedPayload = payload,
            decryptedJson = "",
            statusMessage = "JSON cifrado y guardado con AES-256-GCM + RSA-OAEP",
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
            statusMessage = "JSON descifrado correctamente con la clave privada del Keystore",
        )
    }
}