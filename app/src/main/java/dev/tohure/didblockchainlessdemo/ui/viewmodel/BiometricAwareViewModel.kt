package dev.tohure.didblockchainlessdemo.ui.viewmodel

import android.app.Application
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tohure.didblockchainlessdemo.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel base para pantallas que requieren autenticación biométrica (SÓLO HUELLA).
 *
 * Centraliza:
 *  - El manejo de [KeyPermanentlyInvalidatedException] y [UserNotAuthenticatedException].
 *  - Los callbacks del prompt biométrico: [onBiometricSuccess], [onBiometricFailure], [onBiometricPromptDismissed].
 *  - La función [launch] con re-intento automático tras una autenticación exitosa.
 *
 * @param S Tipo inmutable del estado de UI de la subclase.
 */
abstract class BiometricAwareViewModel<S>(application: Application) : AndroidViewModel(application) {

    protected abstract val _uiState: MutableStateFlow<S>

    /** Tag usado en los logs (e.g. "rsa-vm", "did-vm"). */
    protected abstract val vmTag: String

    /** Acción pendiente que se ejecutará tras una autenticación biométrica exitosa. */
    protected var pendingAction: (suspend () -> Unit)? = null

    // ── Transformadores de estado (implementados por la subclase) ──────────────────────────────

    /** Devuelve una copia del estado con [isLoading] actualizado. */
    protected abstract fun S.withLoading(loading: Boolean): S

    /** Devuelve una copia del estado con [showBiometricPrompt] actualizado. */
    protected abstract fun S.withBiometricPrompt(show: Boolean): S

    /** Devuelve una copia del estado con [statusMessage] actualizado. */
    protected abstract fun S.withStatus(message: String): S

    // ── Callbacks biométricos ──────────────────────────────────────────────────────────────────

    open fun onBiometricSuccess() {
        _uiState.update { it.withBiometricPrompt(false) }
        pendingAction?.let { action ->
            pendingAction = null
            launch(action)
        }
    }

    open fun onBiometricFailure() {
        _uiState.update {
            it.withBiometricPrompt(false)
                .withLoading(false)
                .withStatus("Autenticación biométrica fallida")
        }
        pendingAction = null
    }

    open fun onBiometricPromptDismissed() {
        _uiState.update { it.withBiometricPrompt(false).withLoading(false) }
    }

    // ── Launcher con manejo centralizado de errores ────────────────────────────────────────────

    /**
     * Ejecuta [block] en [Dispatchers.IO], gestiona automáticamente:
     * - El estado de carga ([isLoading]).
     * - La invalidación permanente de clave por cambios biométricos.
     * - La necesidad de autenticación biométrica previa al uso de la clave.
     * - Errores genéricos con mensaje en el estado.
     */
    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.withLoading(true) }
            runCatching { block() }
                .onFailure { e ->
                    val cause = e.cause
                    AppLogger.d(vmTag, "Exception caught in launch: $e, Cause: $cause")

                    when {
                        e is KeyPermanentlyInvalidatedException || cause is KeyPermanentlyInvalidatedException -> {
                            AppLogger.e(vmTag, "Clave invalidada permanentemente por cambios biométricos")
                            _uiState.update {
                                it.withLoading(false)
                                    .withStatus("Claves invalidadas por cambios biométricos. Por favor, regenera las claves.")
                            }
                            pendingAction = null
                        }
                        e is UserNotAuthenticatedException || cause is UserNotAuthenticatedException -> {
                            AppLogger.w(vmTag, "Se requiere autenticación biométrica (huella)")
                            pendingAction = block
                            _uiState.update { it.withLoading(false).withBiometricPrompt(true) }
                        }
                        else -> {
                            AppLogger.e(vmTag, "Error en launch: ${e.message}", e)
                            _uiState.update { it.withLoading(false).withStatus("Error: ${e.message}") }
                        }
                    }
                }
                .onSuccess {
                    _uiState.update { it.withLoading(false) }
                }
        }
    }
}
