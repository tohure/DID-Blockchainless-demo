package dev.tohure.didblockchainlessdemo.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    }
}