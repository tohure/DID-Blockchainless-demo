package dev.tohure.didblockchainlessdemo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.didblockchainlessdemo.R
import dev.tohure.didblockchainlessdemo.ui.viewmodel.DidUiState
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofJwtSection(
    state: DidUiState,
    onRequest: () -> Unit,
    onClear: () -> Unit
) {
    SectionCard(title = "✍️  Proof JWT (Solicitar credencial)") {
        Text(
            text = "Solicita un nonce al backend, construye el Proof JWT firmado con la clave secp256k1 y muéstralo aquí.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(10.dp))
        ActionButton(
            text = "Solicitar nonce y firmar",
            icon = R.drawable.key,
            enabled = !state.isLoading && state.didKeysExist,
            modifier = Modifier.fillMaxWidth(),
            onClick = onRequest
        )
        AnimatedVisibility(
            visible = state.lastProofJwt.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val tabs = listOf("JWT (Raw)", "Payload (Decoded)")

                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }

                val contentText = if (selectedTabIndex == 0) {
                    state.lastProofJwt
                } else {
                    decodeJwtPayload(state.lastProofJwt)
                }

                Text(
                    text = contentText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    ),
                    maxLines = if (selectedTabIndex == 0) 6 else 20,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        )
                        .padding(10.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Proof JWT", contentText)
                            clipboard.setPrimaryClip(clip)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Copiar", fontSize = 13.sp)
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
                        Text("Limpiar", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun decodeJwtPayload(jwt: String): String {
    return try {
        val parts = jwt.split(".")
        if (parts.size < 2) return "JWT inválido"
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
        JSONObject(payload).toString(4)
    } catch (e: Exception) {
        "Error al decodificar: ${e.message}"
    }
}