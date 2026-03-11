package dev.tohure.didblockchainlessdemo.crypto

/**
 * Objeto de configuración para las operaciones criptográficas.
 */
object CryptoConfig {
    /**
     * Si es `true`, se requerirá autenticación biométrica (huella/rostro) para
     * crear y utilizar las claves almacenadas en el Android Keystore.
     *
     * Además, las claves se invalidarán automáticamente si se añade o elimina
     * una huella o rostro del dispositivo.
     *
     * Si es `false`, las claves se crearán sin este requisito.
     */
    const val USE_BIOMETRICS = true
}