package dev.tohure.didblockchainlessdemo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.didblockchainlessdemo.R
import dev.tohure.didblockchainlessdemo.ui.viewmodel.DidUiState
import dev.tohure.didblockchainlessdemo.utils.AppLogger
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun CredentialResultSection(
    state: DidUiState,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        visible = state.decryptedMetadata.isNotBlank() && state.encryptedCredential.isNotBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        SectionCard(title = "✅  Metadatos de Credenciales") {
            val prettyJson = try {
                JSONArray(state.decryptedMetadata).toString(4)
            } catch (e: Exception) {
                AppLogger.e("credential-ui", "CredentialResultSection: $e")
                try {
                    JSONObject(state.decryptedMetadata).toString(4)
                } catch (e2: Exception) {
                    AppLogger.e("credential-ui", "CredentialResultSection: $e2")
                    state.decryptedMetadata // Si falla, mostrar texto original
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
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val context = LocalContext.current
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Encrypted Credential", state.encryptedCredential)
                        clipboard.setPrimaryClip(clip)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lock),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar (Cifrada)", fontSize = 13.sp)
                }

                TextButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.visibility_off),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Ocultar", fontSize = 13.sp)
                }
            }
        }
    }
}