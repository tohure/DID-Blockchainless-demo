package dev.tohure.didblockchainlessdemo.did

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import dev.tohure.didblockchainlessdemo.crypto.KeystoreHelper
import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.util.BigIntegers
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import javax.crypto.SecretKey

/**
 * DIDKeyManager
 *
 * Gestiona el par de claves secp256k1 que define la identidad DID del dispositivo:
 *  - Genera el par secp256k1 con BouncyCastle.
 *  - Cifra la clave privada con AES-256-GCM, cuya clave AES vive en el AndroidKeyStore
 *    (StrongBox si disponible, TEE como fallback).
 *  - Deriva el DID según el método did:key (multicodec secp256k1-pub + Base58btc).
 *  - Firma datos con ES256K (RFC 6979 — nonce determinístico).
 */
class DIDKeyManager(context: Context) {

    private val prefs = context.getSharedPreferences("did_key_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val WRAP_KEY_ALIAS = "DIDWrapKey"
        private const val PREF_ENC_PRIV = "enc_private_key"
        private const val PREF_IV = "aes_gcm_iv"
        private const val PREF_PUB_HEX = "public_key_hex"

        // Prefijo multicodec secp256k1-pub (varint 0xe7 0x01)
        private val SECP256K1_MULTICODEC = byteArrayOf(0xe7.toByte(), 0x01.toByte())
    }

    /**
     * Genera el par secp256k1 si todavía no existe.
     * @return true si se generaron claves nuevas, false si ya existían.
     */
    fun generateKeysIfNeeded(): Boolean {
        if (keysExist()) return false

        val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val kpg = KeyPairGenerator.getInstance("ECDSA", "BC")
        kpg.initialize(spec, SecureRandom())
        val pair = kpg.generateKeyPair()

        val ecPub  = pair.public  as ECPublicKey
        val ecPriv = pair.private as ECPrivateKey

        // Clave privada — escalar S como 32 bytes big-endian
        val privBytes = BigIntegers.asUnsignedByteArray(32, ecPriv.d)
        // Clave pública — punto comprimido (33 bytes)
        val pubBytes  = ecPub.q.getEncoded(true)

        // Cifrar clave privada con AES-GCM (la clave AES vive en Keystore)
        val wrapKey = getOrCreateWrapKey()
        val cipher  = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrapKey)
        val iv      = cipher.iv
        val encPriv = cipher.doFinal(privBytes)

        // Limpiar clave privada de RAM inmediatamente
        privBytes.fill(0)

        prefs.edit {
            putString(PREF_ENC_PRIV, Base64.encodeToString(encPriv, Base64.NO_WRAP))
            putString(PREF_IV,       Base64.encodeToString(iv,      Base64.NO_WRAP))
            putString(PREF_PUB_HEX,  pubBytes.joinToString("") { "%02x".format(it) })
        }
        return true
    }

    fun keysExist(): Boolean = prefs.contains(PREF_PUB_HEX)

    fun deleteKeys() {
        prefs.edit { clear() }
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (ks.containsAlias(WRAP_KEY_ALIAS)) ks.deleteEntry(WRAP_KEY_ALIAS)
    }

    /** Algoritmo did:key para secp256k1:
        [0xe7,0x01] + pubKey(33 bytes) → base58btc → "z" + encoded → "did:key:z..."*/
    fun getDID(): String {
        val pubBytes = getPublicKeyBytes()
        val multikey = SECP256K1_MULTICODEC + pubBytes
        val encoded  = "z" + Base58.encode(multikey)
        return "did:key:$encoded"
    }

    /**
     * Devuelve el key ID para el header JWT:
     *   did:key:zQ3sh...#zQ3sh...
     */
    fun getKeyId(): String {
        val did      = getDID()
        val fragment = did.removePrefix("did:key:")
        return "$did#$fragment"
    }

    /**
     * Firma [data] usando ES256K.
     * Internamente hashea con SHA-256 y devuelve la firma en formato R‖S (64 bytes).
     * La clave privada se limpia de RAM en el bloque finally.
     */
    fun sign(data: ByteArray): ByteArray {
        val privBytes = loadPrivateKey()
        return try {
            val spec   = ECNamedCurveTable.getParameterSpec("secp256k1")
            val domain = ECDomainParameters(spec.curve, spec.g, spec.n, spec.h)

            // RFC 6979: nonce determinístico (evita vulnerabilidades por aleatoriedad)
            val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
            signer.init(true, ECPrivateKeyParameters(BigInteger(1, privBytes), domain))

            val hash = sha256(data)
            val sig  = signer.generateSignature(hash)

            // Formato R‖S (64 bytes, sin DER)
            val r = BigIntegers.asUnsignedByteArray(32, sig[0])
            val s = BigIntegers.asUnsignedByteArray(32, sig[1])
            r + s
        } finally {
            privBytes.fill(0) // limpiar de RAM
        }
    }

    fun getSecurityLevel(): SecurityLevel {
        val ks  = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val key = ks.getKey(WRAP_KEY_ALIAS, null) ?: return SecurityLevel.UNKNOWN
        return KeystoreHelper.querySecurityLevel(
            keystoreProvider = KEYSTORE_PROVIDER,
            keyAlgorithm     = KeyProperties.KEY_ALGORITHM_AES,
            key              = key,
        )
    }

    private fun getOrCreateWrapKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!ks.containsAlias(WRAP_KEY_ALIAS)) {
            val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            KeystoreHelper.withBestSecurity(
                strongBoxBlock = {
                    kg.init(buildWrapKeySpec(strongBox = true))
                    kg.generateKey()
                },
                fallbackBlock = {
                    kg.init(buildWrapKeySpec(strongBox = false))
                    kg.generateKey()
                }
            )
        }
        return ks.getKey(WRAP_KEY_ALIAS, null) as SecretKey
    }

    private fun buildWrapKeySpec(strongBox: Boolean): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            WRAP_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(strongBox)
        }
        return builder.build()
    }

    private fun loadPrivateKey(): ByteArray {
        val encPrivStr = prefs.getString(PREF_ENC_PRIV, null)
            ?: error("Clave privada cifrada no encontrada. Genera las claves primero.")
        val ivStr = prefs.getString(PREF_IV, null)
            ?: error("IV no encontrado. Las claves pueden estar corruptas.")

        val encPriv = Base64.decode(encPrivStr, Base64.NO_WRAP)
        val iv      = Base64.decode(ivStr,      Base64.NO_WRAP)
        val wrapKey = getOrCreateWrapKey()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, wrapKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encPriv)
    }

    private fun getPublicKeyBytes(): ByteArray {
        val hex = prefs.getString(PREF_PUB_HEX, null)
            ?: error("Clave pública no encontrada. Genera las claves primero.")
        // Decodificación hex manual — compatible con API 27+ (HexFormat.parseHex requiere API 34)
        require(hex.length % 2 == 0) { "Hex inválido: longitud impar (${hex.length})" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    private fun sha256(data: ByteArray): ByteArray {
        val digest = SHA256Digest()
        digest.update(data, 0, data.size)
        val out = ByteArray(32)
        digest.doFinal(out, 0)
        return out
    }
}