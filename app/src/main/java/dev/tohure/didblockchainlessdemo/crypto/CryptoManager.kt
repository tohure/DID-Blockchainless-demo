package dev.tohure.didblockchainlessdemo.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.security.spec.MGF1ParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * CryptoManager
 *
 * Genera y almacena un par de claves RSA-2048 en el Android Keystore (respaldado por hardware).
 * La clave privada NUNCA sale del Keystore.
 *
 * Esquema de cifrado (híbrido):
 *   1. Genera clave AES-256 efímera.
 *   2. Cifra el JSON con AES-256-GCM.
 *   3. Cifra la clave AES con la clave pública RSA del Keystore (OAEP SHA-256/MGF1-SHA1).
 *   4. Almacena: [RSA_KEY_SIZE(256 bytes)] + [IV(12 bytes)] + [ciphertext]
 *
 * El descifrado revierte el proceso usando la clave privada del Keystore.
 */
class CryptoManager {

    companion object {
        private const val KEY_ALIAS = "VerifiableCredentialKey"

        private const val RSA_ALGORITHM = "RSA"
        private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        private const val KEY_SIZE_RSA = 2048

        private const val AES_ALGORITHM = "AES"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12   // bytes
        private const val GCM_TAG_LENGTH = 128 // bits

        // Tamaño fijo del bloque RSA cifrado = KEY_SIZE_RSA / 8 = 256 bytes
        private const val RSA_ENCRYPTED_KEY_SIZE = KEY_SIZE_RSA / 8
    }

    /**
     * Genera el par RSA en Keystore si aún no existe.
     * @return true si se generó, false si ya existía.
     */
    @Synchronized
    fun generateKeyPairIfNeeded(): Boolean {
        if (keyPairExists()) return false
        KeystoreHelper.withBestSecurity { isStrongBox ->
            val kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM, KeystoreHelper.KEYSTORE_PROVIDER)
            kpg.initialize(buildKeySpec(strongBox = isStrongBox))
            kpg.generateKeyPair()
        }
        return true
    }

    private fun buildKeySpec(strongBox: Boolean): KeyGenParameterSpec {
        return KeystoreHelper.buildKeyGenSpec(
            alias = KEY_ALIAS,
            purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            isStrongBox = strongBox
        ) {
            setKeySize(KEY_SIZE_RSA)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
        }
    }

    fun getSecurityLevel(): SecurityLevel {
        return KeystoreHelper.getAsymmetricKeySecurityLevel(KEY_ALIAS, RSA_ALGORITHM)
    }

    fun keyPairExists(): Boolean = KeystoreHelper.keyStore.containsAlias(KEY_ALIAS)

    fun deleteKeyPair() {
        val keyStore = KeystoreHelper.keyStore
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    /**
     * Devuelve la clave pública en formato Base64 (DER/X.509).
     * Puede compartirse libremente.
     */
    fun getPublicKeyBase64(): String {
        val certificate = KeystoreHelper.keyStore.getCertificate(KEY_ALIAS)
            ?: error("Par de claves no encontrado. Llama a generateKeyPairIfNeeded() primero.")
        return Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Cifra [plainText] usando cifrado híbrido RSA+AES-GCM.
     * @return Base64 del payload cifrado listo para almacenar/transmitir.
     */
    fun encrypt(plainText: String): String {
        val aesKey = generateAesKey()

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val encryptedAesKey = encryptAesKeyWithRsa(aesKey)

        // Empaqueta: [encryptedAesKey(256)] + [iv(12)] + [encryptedData]
        val payload = encryptedAesKey + iv + encryptedData
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    /**
     * Descifra un payload producido por [encrypt].
     * @return El texto plano original.
     */
    fun decrypt(encryptedBase64: String): String {
        val payload = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        val encryptedAesKey = payload.copyOfRange(0, RSA_ENCRYPTED_KEY_SIZE)
        val iv = payload.copyOfRange(RSA_ENCRYPTED_KEY_SIZE, RSA_ENCRYPTED_KEY_SIZE + GCM_IV_LENGTH)
        val encryptedData = payload.copyOfRange(RSA_ENCRYPTED_KEY_SIZE + GCM_IV_LENGTH, payload.size)

        val aesKey = decryptAesKeyWithRsa(encryptedAesKey)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(encryptedData), Charsets.UTF_8)
    }

    private fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGen.init(AES_KEY_SIZE)
        return keyGen.generateKey()
    }

    private fun encryptAesKeyWithRsa(aesKey: SecretKey): ByteArray {
        val publicKey = KeystoreHelper.keyStore.getCertificate(KEY_ALIAS).publicKey
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        val oaepParams = OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams)
        return cipher.doFinal(aesKey.encoded)
    }

    private fun decryptAesKeyWithRsa(encryptedAesKey: ByteArray): SecretKey {
        val privateKey = KeystoreHelper.keyStore.getKey(KEY_ALIAS, null)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        val oaepParams = OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams)
        return SecretKeySpec(cipher.doFinal(encryptedAesKey), AES_ALGORITHM)
    }
}