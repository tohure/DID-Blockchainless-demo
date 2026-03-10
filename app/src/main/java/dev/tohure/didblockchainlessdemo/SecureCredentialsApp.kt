package dev.tohure.didblockchainlessdemo

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import dev.tohure.didblockchainlessdemo.crypto.CryptoManager
import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel
import dev.tohure.didblockchainlessdemo.did.DIDKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class SecureCredentialsApp : Application() {

    val didKeyManager: DIDKeyManager by lazy { DIDKeyManager(this) }
    val cryptoManager: CryptoManager by lazy { CryptoManager() }

    override fun onCreate() {
        super.onCreate()
        setupBouncyCastle()

        // Inicializar claves en background para no bloquear el hilo principal
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            didKeyManager.generateKeysIfNeeded()
            cryptoManager.generateKeyPairIfNeeded()

            val level = didKeyManager.getSecurityLevel()
            if (level == SecurityLevel.SOFTWARE) {
                Log.w("tohure-did", "Wrap key sin respaldo hardware")
            }
        }
    }

    private fun setupBouncyCastle() {
        // Android incluye un proveedor BC recortado que no soporta secp256k1.
        // Lo reemplazamos con el BC completo de BouncyCastle antes de cualquier operación cripto.
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
    }
}
