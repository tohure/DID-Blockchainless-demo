package dev.tohure.didblockchainlessdemo.ui.components

import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.didblockchainlessdemo.R
import dev.tohure.didblockchainlessdemo.ui.viewmodel.RsaUiState

@Composable
fun EncryptedPayloadSection(state: RsaUiState) {
    AnimatedVisibility(
        visible = state.encryptedPayload.isNotBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        SectionCard(title = "🔐  Payload cifrado (Base64)") {
            Text(
                text = state.encryptedPayload,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                ),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            )
            Text(
                text = "Bytes: ${
                    try {
                        Base64.decode(state.encryptedPayload, Base64.NO_WRAP).size
                    } catch (e: Exception) {
                        0
                    }
                } B",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DecryptedJsonSection(
    state: RsaUiState,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        visible = state.decryptedJson.isNotBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        SectionCard(title = "✅  JSON descifrado") {
            Text(
                text = state.decryptedJson,
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
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onClear) {
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