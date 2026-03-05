package dev.tohure.didblockchainlessdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tohure.didblockchainlessdemo.ui.theme.DIDBlockchainlessDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DIDBlockchainlessDemoTheme {
                CredentialScreen()
            }
        }
    }
}

@Composable
fun CredentialScreen(vm: CredentialViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { CredentialTopBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SectionCard(title = "🔑  Android Keystore") {
                KeyStatusRow(state.keyExists, state.securityLevel)

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "Generar claves",
                        icon = R.drawable.key,
                        enabled = !state.isLoading,
                        modifier = Modifier.weight(1f),
                        onClick = vm::generateKeys
                    )
                    ActionButton(
                        text = "Eliminar claves",
                        icon = R.drawable.delete,
                        enabled = !state.isLoading && state.keyExists,
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                        onClick = vm::deleteKeys
                    )
                }

                AnimatedVisibility(visible = state.publicKeyBase64.isNotBlank()) {
                    PublicKeyBox(state.publicKeyBase64)
                }
            }

            SectionCard(title = "📄  Credencial verificable (JSON)") {
                OutlinedTextField(
                    value = state.jsonInput,
                    onValueChange = vm::updateJsonInput,
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
                    ActionButton(
                        text = "Cifrar y guardar",
                        icon = R.drawable.lock,
                        enabled = !state.isLoading && state.keyExists,
                        modifier = Modifier.weight(1f),
                        onClick = vm::encrypt
                    )
                    ActionButton(
                        text = "Descifrar",
                        icon = R.drawable.lock_open,
                        enabled = !state.isLoading && state.keyExists,
                        modifier = Modifier.weight(1f),
                        onClick = vm::decrypt
                    )
                }
            }

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
                            android.util.Base64.decode(
                                state.encryptedPayload,
                                android.util.Base64.NO_WRAP
                            ).size
                        } B",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

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
                            .background(
                                Color(0xFF1B2E1B),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                Color(0xFF4CAF50).copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = vm::clearDecryptedJson) {
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

            AnimatedVisibility(visible = state.statusMessage.isNotBlank()) {
                StatusBar(state.statusMessage)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CredentialTopBar() {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Secure Credentials",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Android Keystore · RSA-2048 + AES-256-GCM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun KeyStatusRow(exists: Boolean, securityLevel: SecurityLevel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val color = if (exists) Color(0xFF81C784) else Color(0xFFFF6B6B)
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            text = if (exists) "Par de claves presente en el Keystore"
            else "No hay claves generadas",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
        Text(
            text = when (securityLevel) {
                SecurityLevel.STRONGBOX -> "🔒 StrongBox (chip dedicado)"
                SecurityLevel.TEE -> "🛡 TEE (hardware seguro)"
                SecurityLevel.SOFTWARE -> "⚠️ Solo software"
                SecurityLevel.UNKNOWN -> ""
            },
            style = MaterialTheme.typography.labelSmall,
            color = when (securityLevel) {
                SecurityLevel.STRONGBOX -> Color(0xFF81C784)
                SecurityLevel.TEE -> Color(0xFF4FC3F7)
                else -> Color(0xFFFF6B6B)
            }
        )
    }
}

@Composable
private fun PublicKeyBox(publicKey: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(top = 12.dp)) {
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                painter = painterResource(if (expanded) R.drawable.visibility_off else R.drawable.visibility),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (expanded) "Ocultar clave pública" else "Ver clave pública (Base64)",
                fontSize = 13.sp
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = publicKey,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    @DrawableRes icon: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun StatusBar(message: String) {
    val isError = message.startsWith("Error")
    val bg = if (isError) Color(0xFF3E1A1A) else Color(0xFF1A2E1A)
    val fg = if (isError) Color(0xFFFF8A80) else Color(0xFFA5D6A7)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (isError) "⚠" else "✓", color = fg, fontSize = 14.sp)
        Text(message, style = MaterialTheme.typography.bodySmall, color = fg)
    }
}