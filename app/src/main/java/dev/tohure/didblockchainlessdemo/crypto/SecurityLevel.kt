package dev.tohure.didblockchainlessdemo.crypto

enum class SecurityLevel {
    STRONGBOX,   // Chip dedicado (mejor)
    TEE,         // Trusted Execution Environment (bueno)
    SOFTWARE,    // Solo software (no recomendado para producción)
    UNKNOWN
}