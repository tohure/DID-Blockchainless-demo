package dev.tohure.didblockchainlessdemo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.didblockchainlessdemo.R
import dev.tohure.didblockchainlessdemo.ui.viewmodel.CredentialUiState

@Composable
fun CredentialSection(
    state: CredentialUiState,
    onUpdateJson: (String) -> Unit,
    onFetchAndEncrypt: () -> Unit,
    onEncrypt: () -> Unit,
    onDecrypt: () -> Unit
) {
    SectionCard(title = "📄  Credencial verificable (JSON)") {
        OutlinedTextField(
            value = state.jsonInput,
            onValueChange = onUpdateJson,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            label = { Text("JSON de la credencial") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            /*ActionButton(
                text = "Descargar y cifrar",
                icon = R.drawable.download,
                enabled = !state.isLoading && !state.isFetching && state.rsaKeyExists,
                modifier = Modifier.weight(1f),
                onClick = onFetchAndEncrypt
            )*/
            ActionButton(
                text = "Cifrar y guardar",
                icon = R.drawable.lock,
                enabled = !state.isLoading && state.rsaKeyExists,
                modifier = Modifier.weight(1f),
                onClick = onEncrypt
            )
            ActionButton(
                text = "Descifrar",
                icon = R.drawable.lock_open,
                enabled = !state.isLoading && state.rsaKeyExists,
                modifier = Modifier.weight(1f),
                onClick = onDecrypt
            )
        }
    }
}