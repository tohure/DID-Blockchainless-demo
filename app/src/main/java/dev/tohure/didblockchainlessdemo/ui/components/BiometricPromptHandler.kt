package dev.tohure.didblockchainlessdemo.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.tohure.didblockchainlessdemo.utils.AppLogger

@Composable
fun BiometricPromptHandler(
    showPrompt: Boolean,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showPrompt) {
        val context = LocalContext.current
        
        LaunchedEffect(Unit) {
            val activity = context as? FragmentActivity
            if (activity == null) {
                AppLogger.e("biometric", "Context is not FragmentActivity, cannot show prompt")
                onFailure()
                return@LaunchedEffect
            }
            
            val executor = ContextCompat.getMainExecutor(context)
            
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        AppLogger.e("biometric", "Auth Error: $errorCode - $errString")

                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || 
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED) {
                            onDismiss()
                        } else {
                            onFailure()
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        AppLogger.w("biometric", "Auth Failed")
                    }
                })

            // Solo BIOMETRIC_STRONG.
            // si cambia la biometría, la clave muere y no hay fallback a PIN.
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación Requerida")
                .setSubtitle("Confirma tu identidad biométrica")
                .setNegativeButtonText("Cancelar") // Obligatorio si no se usa DEVICE_CREDENTIAL
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setConfirmationRequired(true) // Evita auth accidental con rostro
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }
}