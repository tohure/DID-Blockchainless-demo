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
import dev.tohure.didblockchainlessdemo.ui.viewmodel.RsaUiState

@Composable
fun RsaSection(
    state: RsaUiState,
    onGenerate: () -> Unit,
    onDelete: () -> Unit
) {
    SectionCard(title = "🔑  Android Keystore (RSA — cifrado VCs)") {
        KeyStatusRow(
            exists = state.rsaKeyExists,
            securityLevel = state.rsaSecurityLevel,
            presentLabel = "Par RSA-2048 presente en el Keystore",
            absentLabel = "No hay claves RSA generadas"
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                text = "Generar claves",
                icon = R.drawable.key,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f),
                onClick = onGenerate
            )
            ActionButton(
                text = "Eliminar claves",
                icon = R.drawable.delete,
                enabled = !state.isLoading && state.rsaKeyExists,
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
                onClick = onDelete
            )
        }
        AnimatedVisibility(visible = state.publicKeyBase64.isNotBlank()) {
            ExpandableMonoBox(
                label = "Ver clave pública RSA (Base64)",
                hiddenLabel = "Ocultar clave pública",
                content = state.publicKeyBase64,
                topPadding = 12.dp
            )
        }
    }
}