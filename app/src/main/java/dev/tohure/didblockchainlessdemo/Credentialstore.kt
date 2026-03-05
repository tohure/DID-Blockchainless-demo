package dev.tohure.didblockchainlessdemo

import android.content.Context
import androidx.core.content.edit

/**
 * CredentialStore
 *
 * Persiste payloads cifrados en SharedPreferences.
 * El contenido ya viene cifrado por CryptoManager, por lo que
 * SharedPreferences es suficiente aquí. Para mayor defensa en
 * profundidad se puede migrar a EncryptedSharedPreferences de
 * Jetpack Security (ver comentario al final).
 */
class CredentialStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "secure_credentials"
        private const val KEY_PREFIX = "vc_"
    }

    /** Guardo una credencial cifrada bajo [id]. */
    fun save(id: String, encryptedPayload: String) {
        prefs.edit { putString("$KEY_PREFIX$id", encryptedPayload) }
    }

    /** Recupero el payload cifrado para [id], o null si no existe. */
    fun load(id: String): String? = prefs.getString("$KEY_PREFIX$id", null)

    /** Elimino la credencial con [id]. */
    fun delete(id: String) {
        prefs.edit { remove("$KEY_PREFIX$id") }
    }

    /** Listo todas las claves almacenadas (sin prefijo). */
    fun listIds(): List<String> =
        prefs.all.keys
            .filter { it.startsWith(KEY_PREFIX) }
            .map { it.removePrefix(KEY_PREFIX) }

    /** Limpio todo el almacén. */
    fun clear() {
        prefs.edit { clear() }
    }
}
