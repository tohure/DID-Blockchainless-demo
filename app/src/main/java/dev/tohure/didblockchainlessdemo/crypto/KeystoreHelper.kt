package dev.tohure.didblockchainlessdemo.crypto

import android.os.Build
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log

/**
 * KeystoreHelper
 *
 * Centraliza la lógica compartida entre CryptoManager y DIDKeyManager:
 *  - Fallback StrongBox → TEE al inicializar una clave.
 *  - Consulta del nivel de seguridad de una clave existente.
 */
internal object KeystoreHelper {

    private const val TAG = "KeystoreHelper"

    /**
     * Ejecuta [strongBoxBlock] (requiere API 28+).
     * Si StrongBox no está disponible, cae automáticamente a [fallbackBlock].
     *
     * @return el resultado del bloque que pudo ejecutarse.
     */
    fun <T> withBestSecurity(
        strongBoxBlock: () -> T,
        fallbackBlock: () -> T,
    ): T {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return strongBoxBlock()
            } catch (e: StrongBoxUnavailableException) {
                Log.w(TAG, "StrongBox no disponible, usando TEE: ${e.message}")
            }
        }
        return fallbackBlock()
    }

    /**
     * Devuelve el [SecurityLevel] de la clave [keyAlias] en el AndroidKeyStore.
     * Funciona tanto para claves AES (SecretKeyFactory) como RSA/EC (KeyFactory).
     *
     * @param keystoreProvider proveedor del Keystore (normalmente "AndroidKeyStore").
     * @param keyAlgorithm     algoritmo de la clave (ej. "AES", "RSA").
     * @param key              la clave cuyo nivel de seguridad se quiere consultar.
     */
    fun querySecurityLevel(
        keystoreProvider: String,
        keyAlgorithm: String,
        key: java.security.Key,
    ): SecurityLevel {
        return try {
            val keyInfo: KeyInfo = if (keyAlgorithm == KeyProperties.KEY_ALGORITHM_AES) {
                val factory = javax.crypto.SecretKeyFactory.getInstance(keyAlgorithm, keystoreProvider)
                factory.getKeySpec(key as javax.crypto.SecretKey, KeyInfo::class.java) as KeyInfo
            } else {
                val factory = java.security.KeyFactory.getInstance(keyAlgorithm, keystoreProvider)
                factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (keyInfo.securityLevel) {
                    KeyProperties.SECURITY_LEVEL_STRONGBOX -> SecurityLevel.STRONGBOX
                    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> SecurityLevel.TEE
                    KeyProperties.SECURITY_LEVEL_SOFTWARE -> SecurityLevel.SOFTWARE
                    else -> SecurityLevel.UNKNOWN
                }
            } else {
                @Suppress("DEPRECATION")
                if (keyInfo.isInsideSecureHardware) SecurityLevel.TEE else SecurityLevel.SOFTWARE
            }
        } catch (e: Exception) {
            Log.e(TAG, "querySecurityLevel: ${e.message}")
            SecurityLevel.UNKNOWN
        }
    }
}
