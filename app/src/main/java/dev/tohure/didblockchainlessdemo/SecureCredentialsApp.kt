package dev.tohure.didblockchainlessdemo

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class SecureCredentialsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupBouncyCastle()
    }

    private fun setupBouncyCastle() {
        // Android incluye un proveedor BC recortado que no soporta secp256k1.
        // Lo reemplazamos con el BC completo de BouncyCastle antes de cualquier operación cripto.
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
    }
}
