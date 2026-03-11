package dev.tohure.didblockchainlessdemo.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import dev.tohure.didblockchainlessdemo.utils.AppLogger
import java.security.KeyStore
import javax.crypto.SecretKeyFactory

/**
 * KeystoreHelper
 *
 * Centraliza la lógica compartida entre CryptoManager y DIDKeyManager:
 *  - Fallback StrongBox → TEE al inicializar una clave.
 *  - Consulta del nivel de seguridad de una clave existente.
 *  - Construcción de KeyGenParameterSpec con soporte StrongBox.
 */
internal object KeystoreHelper {

    private const val TAG = "keystore"
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

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
                AppLogger.w(TAG, "StrongBox no disponible, usando TEE: ${e.message}", e)
            }
        }
        return fallbackBlock()
    }

    /**
     * Construye un KeyGenParameterSpec.Builder con la configuración de StrongBox aplicada si es necesario.
     */
    fun buildKeyGenSpec(
        alias: String,
        purposes: Int,
        isStrongBox: Boolean,
        block: KeyGenParameterSpec.Builder.() -> Unit
    ): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(alias, purposes)
        builder.block()
        
        AppLogger.d(TAG, "Configurando clave $alias. USE_BIOMETRICS=${CryptoConfig.USE_BIOMETRICS}")

        if (CryptoConfig.USE_BIOMETRICS) {
            builder.setUserAuthenticationRequired(true)

            // API 30+ (Android 11): Configuración explícita de autenticación fuerte
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AppLogger.d(TAG, "Aplicando setUserAuthenticationParameters (API 30+)")

                builder.setUserAuthenticationParameters(
                    10, // Duración
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                )
            } else {
                // API < 30: Configuración legacy
                AppLogger.d(TAG, "Aplicando setUserAuthenticationRequired (API < 30)")
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(10)
            }
            
            // Invalida la clave si se añade una nueva huella/rostro
            builder.setInvalidatedByBiometricEnrollment(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(isStrongBox)
        }
        return builder.build()
    }

    /**
     * Devuelve el [SecurityLevel] de una clave asimétrica (RSA/EC) por su alias.
     */
    fun getAsymmetricKeySecurityLevel(alias: String, algorithm: String): SecurityLevel {
        return try {
            val key = keyStore.getKey(alias, null) ?: return SecurityLevel.UNKNOWN
            val factory = java.security.KeyFactory.getInstance(algorithm, KEYSTORE_PROVIDER)
            querySecurityLevel(key, factory)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error al consultar nivel de clave asimétrica: ${e.message}", e)
            SecurityLevel.UNKNOWN
        }
    }

    /**
     * Devuelve el [SecurityLevel] de una clave simétrica (AES) por su alias.
     */
    fun getSymmetricKeySecurityLevel(alias: String, algorithm: String): SecurityLevel {
        return try {
            val key = keyStore.getKey(alias, null) ?: return SecurityLevel.UNKNOWN
            val factory = SecretKeyFactory.getInstance(algorithm, KEYSTORE_PROVIDER)
            querySecurityLevel(key, factory)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error al consultar nivel de clave simétrica: ${e.message}", e)
            SecurityLevel.UNKNOWN
        }
    }

    private fun querySecurityLevel(key: java.security.Key, factory: Any): SecurityLevel {
        val keyInfo: KeyInfo = when (factory) {
            is java.security.KeyFactory -> factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
            is SecretKeyFactory -> factory.getKeySpec(key as javax.crypto.SecretKey, KeyInfo::class.java) as KeyInfo
            else -> throw IllegalArgumentException("Tipo de factory no soportado")
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
    }
}