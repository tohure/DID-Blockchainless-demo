package dev.tohure.didblockchainlessdemo.ui

import android.os.Bundle
import android.util.Base64
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
import androidx.compose.foundation.shape.CircleShape
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
import dev.tohure.didblockchainlessdemo.R
import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel
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
            if (state.isLoading || state.isFetching) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // ── Sección 1: Identidad DID (secp256k1) ─────────────────
            SectionCard(title = "🪪  Identidad DID (secp256k1)") {
                KeyStatusRow(
                    exists = state.didKeysExist,
                    securityLevel = state.didSecurityLevel,
                    presentLabel = "Par secp256k1 presente",
                    absentLabel  = "No hay claves DID"
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "Generar DID",
                        icon = R.drawable.key,
                        enabled = !state.isLoading,
                        modifier = Modifier.weight(1f),
                        onClick = vm::generateDIDKeys
                    )
                    ActionButton(
                        text = "Eliminar DID",
                        icon = R.drawable.delete,
                        enabled = !state.isLoading && state.didKeysExist,
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                        onClick = vm::deleteDIDKeys
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

            // ── Sección 2: Proof JWT (nonce → firma) ── ───────────────
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
                    enabled = !state.isLoading && !state.isFetching && state.didKeysExist,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { vm.requestCredentialWithNonce() }
                )
                AnimatedVisibility(
                    visible = state.lastProofJwt.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit  = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = state.lastProofJwt,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            ),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        )
                        TextButton(onClick = vm::clearProofJwt) {
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

            // ── Sección 3: Keystore RSA (cifrado de VCs) ──────────────
            SectionCard(title = "🔑  Android Keystore (RSA — cifrado VCs)") {
                KeyStatusRow(
                    exists = state.rsaKeyExists,
                    securityLevel = state.rsaSecurityLevel,
                    presentLabel = "Par RSA-2048 presente en el Keystore",
                    absentLabel  = "No hay claves RSA generadas"
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "Generar claves",
                        icon = R.drawable.key,
                        enabled = !state.isLoading,
                        modifier = Modifier.weight(1f),
                        onClick = vm::generateRsaKeys
                    )
                    ActionButton(
                        text = "Eliminar claves",
                        icon = R.drawable.delete,
                        enabled = !state.isLoading && state.rsaKeyExists,
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                        onClick = vm::deleteRsaKeys
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

            // ── Sección 4: Credencial verificable ─────────────────────
            SectionCard(title = "📄  Credencial verificable (JSON)") {
                OutlinedTextField(
                    value = state.jsonInput,
                    onValueChange = vm::updateJsonInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 13.sp
                    ),
                    label  = { Text("JSON de la credencial") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "Descargar y cifrar",
                        icon = R.drawable.download,
                        enabled = !state.isLoading && !state.isFetching && state.rsaKeyExists,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            vm.fetchAndEncrypt(
                                credentialId = "id-de-la-credencial",
                                token = "token-del-usuario"
                            )
                        }
                    )
                    ActionButton(
                        text = "Cifrar y guardar",
                        icon = R.drawable.lock,
                        enabled = !state.isLoading && state.rsaKeyExists,
                        modifier = Modifier.weight(1f),
                        onClick = vm::encrypt
                    )
                    ActionButton(
                        text = "Descifrar",
                        icon = R.drawable.lock_open,
                        enabled = !state.isLoading && state.rsaKeyExists,
                        modifier = Modifier.weight(1f),
                        onClick = vm::decrypt
                    )
                }
            }

            // ── Payload cifrado ───────────────────────────────────────
            AnimatedVisibility(
                visible = state.encryptedPayload.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically()
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
                            Base64.decode(state.encryptedPayload, Base64.NO_WRAP).size
                        } B",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // ── JSON descifrado ───────────────────────────────────────
            AnimatedVisibility(
                visible = state.decryptedJson.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically()
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

            // ── Barra de estado ───────────────────────────────────────
            AnimatedVisibility(visible = state.statusMessage.isNotBlank()) {
                StatusBar(state.statusMessage)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Componentes privados ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CredentialTopBar() {
    TopAppBar(
        title = {
            Column {
                Text("DID Demo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "did:key · secp256k1 · RSA-2048 + AES-256-GCM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor    = MaterialTheme.colorScheme.surface,
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
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text     = title,
                style    = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun KeyStatusRow(
    exists: Boolean,
    securityLevel: SecurityLevel,
    presentLabel: String = "Claves presentes",
    absentLabel:  String = "No hay claves generadas",
) {
    Row(
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val color = if (exists) Color(0xFF81C784) else Color(0xFFFF6B6B)
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(
            text  = if (exists) presentLabel else absentLabel,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
        if (exists) {
            Text(
                text = when (securityLevel) {
                    SecurityLevel.STRONGBOX -> "🔒 StrongBox"
                    SecurityLevel.TEE       -> "🛡 TEE"
                    SecurityLevel.SOFTWARE  -> "⚠️ Software"
                    SecurityLevel.UNKNOWN   -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (securityLevel) {
                    SecurityLevel.STRONGBOX -> Color(0xFF81C784)
                    SecurityLevel.TEE       -> Color(0xFF4FC3F7)
                    else                    -> Color(0xFFFF6B6B)
                }
            )
        }
    }
}

/**
 * Caja de texto monoespaciado con toggle mostrar/ocultar.
 */
@Composable
private fun ExpandableMonoBox(
    label: String,
    hiddenLabel: String,
    content: String,
    topPadding: androidx.compose.ui.unit.Dp = 12.dp,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(top = topPadding)) {
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
            Text(if (expanded) hiddenLabel else label, fontSize = 13.sp)
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text     = content,
                style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
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
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(44.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape    = RoundedCornerShape(10.dp),
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
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (isError) "⚠" else "✓", color = fg, fontSize = 14.sp)
        Text(message, style = MaterialTheme.typography.bodySmall, color = fg)
    }
}