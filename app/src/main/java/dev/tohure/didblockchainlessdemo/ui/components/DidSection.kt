package dev.tohure.didblockchainlessdemo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tohure.didblockchainlessdemo.R
import dev.tohure.didblockchainlessdemo.ui.viewmodel.DidUiState

@Composable
fun DidSection(
    state: DidUiState,
    onGenerate: () -> Unit,
    onDelete: () -> Unit
) {
    SectionCard(title = "🪪  Identidad DID (secp256k1)") {
        KeyStatusRow(
            exists = state.didKeysExist,
            securityLevel = state.didSecurityLevel,
            presentLabel = "Par secp256k1 presente",
            absentLabel = "No hay claves DID"
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                text = "Generar DID",
                icon = R.drawable.key,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f),
                onClick = onGenerate
            )
            ActionButton(
                text = "Eliminar DID",
                icon = R.drawable.delete,
                enabled = !state.isLoading && state.didKeysExist,
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
                onClick = onDelete
            )
        }

        AnimatedVisibility(visible = state.did.isNotBlank()) {
            ExpandableMonoBox(
                label = "Ver DID",
                hiddenLabel = "Ocultar DID",
                content = state.did,
                topPadding = 12.dp
            )
        }
        AnimatedVisibility(visible = state.keyId.isNotBlank()) {
            ExpandableMonoBox(
                label = "Ver key ID (kid)",
                hiddenLabel = "Ocultar key ID",
                content = state.keyId,
                topPadding = 4.dp
            )
        }
    }
}