package dev.tohure.didblockchainlessdemo.crypto

/**
 * Objeto de configuración para las operaciones criptográficas.
 */
object CryptoConfig {
    /**
     * Si es `true`, las claves del Keystore requieren autenticación biométrica
     * con **huella digital fuerte (clase 3 / BIOMETRIC_STRONG)** antes de ser usadas.
     *
     * Implicaciones:
     *  - **Sin fallback**: no se acepta PIN, patrón ni contraseña del dispositivo.
     *  - **Sin face de baja seguridad**: solo sensores de huella certificados.
     *  - **Invalidación automática**: si se añade o elimina una huella, las claves
     *    se invalidan y el usuario debe regenerarlas.
     *
     * Si es `false`, las claves se crean sin requisito de autenticación.
     */
    const val USE_BIOMETRICS = true
}