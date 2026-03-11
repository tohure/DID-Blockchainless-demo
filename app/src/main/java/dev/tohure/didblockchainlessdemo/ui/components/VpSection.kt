package dev.tohure.didblockchainlessdemo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.tohure.didblockchainlessdemo.R
import dev.tohure.didblockchainlessdemo.ui.viewmodel.DidUiState

@Composable
fun VpSection(
    state: DidUiState,
    onVerify: () -> Unit
) {
    SectionCard(title = "🛡️  Verifiable Presentation (VP)") {
        Text(
            text = "Genera un VP JWT que envuelve la credencial obtenida y lo envía al backend para su verificación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(10.dp))
        
        ActionButton(
            text = "Verificar VP",
            icon = R.drawable.lock_open,
            enabled = !state.isLoading && state.decryptedMetadata.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = onVerify
        )

        AnimatedVisibility(
            visible = state.validationResponseJson.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    text = "Respuesta de validación:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = state.validationResponseJson,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                )
            }
        }
    }
}