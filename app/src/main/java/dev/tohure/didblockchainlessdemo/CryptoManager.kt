package dev.tohure.didblockchainlessdemo

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.jvm.java

enum class SecurityLevel {
    STRONGBOX,   // Chip dedicado (mejor)
    TEE,         // Trusted Execution Environment (bueno)
    SOFTWARE,    // Solo software (no recomendado para producción)
    UNKNOWN
}

/**
 * CryptoManager
 *
 * Genera y almacena un par de claves RSA-2048 en el Android Keystore (respaldado por hardware).
 * La clave privada NUNCA sale del Keystore.
 *
 * Esquema de cifrado (híbrido):
 *   1. Genera clave AES-256 efímera.
 *   2. Cifra el JSON con AES-256-GCM.
 *   3. Cifra la clave AES con la clave pública RSA del Keystore.
 *   4. Almacena: [RSA_KEY_SIZE(256 bytes)] + [IV(12 bytes)] + [ciphertext]
 *
 * El descifrado revierte el proceso usando la clave privada del Keystore.
 */
class CryptoManager {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
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
     * Par RSA en Keystore si aún no existe.
     * @return true si se generó, false si ya existía.
     */
    fun generateKeyPairIfNeeded(): Boolean {
        if (keyPairExists()) return false

        // Intento StrongBox primero, si no, TEE
        val usedStrongBox = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            tryGenerateWithStrongBox()
        } else false

        if (!usedStrongBox) {
            generateWithTee()
        }

        return true
    }

    private fun tryGenerateWithStrongBox(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM, KEYSTORE_PROVIDER)
            keyPairGenerator.initialize(buildKeySpec(strongBox = true))
            keyPairGenerator.generateKeyPair()
            true
        } catch (e: StrongBoxUnavailableException) {
            Log.e("tohure", "tryGenerateWithStrongBox: $e")
            false
        }
    }

    private fun generateWithTee() {
        val keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM, KEYSTORE_PROVIDER)
        keyPairGenerator.initialize(buildKeySpec(strongBox = false))
        keyPairGenerator.generateKeyPair()
    }

    private fun buildKeySpec(strongBox: Boolean): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setKeySize(KEY_SIZE_RSA).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
        /** Autenticación del usuario para usar la clave (probando) */
        // .setUserAuthenticationRequired(true)
        // .setUserAuthenticationValidityDurationSeconds(30)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(strongBox)
        }

        return builder.build()
    }

    fun getSecurityLevel(): SecurityLevel {
        val keyStore = loadKeyStore()
        val privateKey = keyStore.getKey(KEY_ALIAS, null) ?: return SecurityLevel.UNKNOWN

        return try {
            val keyFactory = KeyFactory.getInstance(privateKey.algorithm, KEYSTORE_PROVIDER)
            val keyInfo = keyFactory.getKeySpec(privateKey, KeyInfo::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                /** API 31+ tiene securityLevel explícito */
                when (keyInfo.securityLevel) {
                    KeyProperties.SECURITY_LEVEL_STRONGBOX -> SecurityLevel.STRONGBOX
                    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> SecurityLevel.TEE
                    KeyProperties.SECURITY_LEVEL_SOFTWARE -> SecurityLevel.SOFTWARE
                    else -> SecurityLevel.UNKNOWN
                }
            } else {
                /** Esta depreciado pero no importa si el device tiene si o si una versión menor */
                if (keyInfo.isInsideSecureHardware) SecurityLevel.TEE else SecurityLevel.SOFTWARE
            }
        } catch (e: Exception) {
            SecurityLevel.UNKNOWN
        }
    }

    /** Comprueba si el par de claves ya existe en el Keystore. */
    fun keyPairExists(): Boolean {
        val keyStore = loadKeyStore()
        return keyStore.containsAlias(KEY_ALIAS)
    }

    /** Elimina el par de claves del Keystore. */
    fun deleteKeyPair() {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    /**
     * Devuelve la clave pública en formato Base64 (DER/X.509).
     * Puede compartirse libremente.
     */
    fun getPublicKeyBase64(): String {
        val keyStore = loadKeyStore()
        val certificate = keyStore.getCertificate(KEY_ALIAS)
            ?: throw IllegalStateException("Key pair not found. Call generateKeyPairIfNeeded() first.")
        return Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Cifra [plainText] usando cifrado híbrido RSA+AES-GCM.
     * @return Base64 del payload cifrado listo para almacenar/transmitir.
     */
    fun encrypt(plainText: String): String {
        /** Genera clave AES-256 efímera*/
        val aesKey = generateAesKey()

        /** Cifra el texto con AES-256-GCM*/
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val iv = cipher.iv // 12 bytes GCM IV
        val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        /** Cifra la clave AES con la clave pública RSA del Keystore */
        val encryptedAesKey = encryptAesKeyWithRsa(aesKey)

        /** Empaqueta: [encryptedAesKey(256)] + [iv(12)] + [encryptedData] */
        val payload = encryptedAesKey + iv + encryptedData
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    /**
     * Descifra un payload producido por [encrypt].
     * @return El texto plano original.
     */
    fun decrypt(encryptedBase64: String): String {
        val payload = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        /** Extrae componentes del payload */
        val encryptedAesKey = payload.copyOfRange(0, RSA_ENCRYPTED_KEY_SIZE)
        val iv = payload.copyOfRange(RSA_ENCRYPTED_KEY_SIZE, RSA_ENCRYPTED_KEY_SIZE + GCM_IV_LENGTH)
        val encryptedData =
            payload.copyOfRange(RSA_ENCRYPTED_KEY_SIZE + GCM_IV_LENGTH, payload.size)

        /** Descifra la clave AES con la clave privada RSA del Keystore */
        val aesKey = decryptAesKeyWithRsa(encryptedAesKey)

        /** Descifra el dato con AES-256-GCM */
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec)
        val decryptedBytes = cipher.doFinal(encryptedData)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun loadKeyStore(): KeyStore {
        return KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    private fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGen.init(AES_KEY_SIZE)
        return keyGen.generateKey()
    }

    private fun encryptAesKeyWithRsa(aesKey: SecretKey): ByteArray {
        val keyStore = loadKeyStore()
        val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        val oaepParams = OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA1,       // ← clave: MGF1 usa SHA-1, no SHA-256
            PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams)
        return cipher.doFinal(aesKey.encoded)
    }

    private fun decryptAesKeyWithRsa(encryptedAesKey: ByteArray): SecretKey {
        val keyStore = loadKeyStore()
        val privateKey = keyStore.getKey(KEY_ALIAS, null)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        val oaepParams = OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA1,       // ← mismo spec que al cifrar
            PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams)
        val aesKeyBytes = cipher.doFinal(encryptedAesKey)
        return SecretKeySpec(aesKeyBytes, AES_ALGORITHM)
    }
}