package dev.tohure.didblockchainlessdemo.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.tohure.didblockchainlessdemo.crypto.KeystoreHelper.TAG
import dev.tohure.didblockchainlessdemo.ui.viewmodel.CredentialUiState
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun CredentialResultSection(
    state: CredentialUiState
) {
    AnimatedVisibility(
        visible = state.decryptedJson.isNotBlank() && state.encryptedPayload.isNotBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        SectionCard(title = "✅  Metadatos de Credenciales") {
            val prettyJson = try {
                JSONArray(state.decryptedJson).toString(4)
            } catch (e: Exception) {
                Log.e(TAG, "CredentialResultSection: $e")
                try {
                    JSONObject(state.decryptedJson).toString(4)
                } catch (e2: Exception) {
                    Log.e(TAG, "CredentialResultSection: $e2")
                    state.decryptedJson // Si falla, mostrar texto original
                }
            }

            Text(
                text = prettyJson,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF81C784)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B2E1B), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }
    }
}