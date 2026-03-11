package dev.tohure.didblockchainlessdemo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tohure.didblockchainlessdemo.R
import dev.tohure.didblockchainlessdemo.ui.components.BiometricPromptHandler
import dev.tohure.didblockchainlessdemo.ui.components.CredentialSection
import dev.tohure.didblockchainlessdemo.ui.components.DecryptedJsonSection
import dev.tohure.didblockchainlessdemo.ui.components.EncryptedPayloadSection
import dev.tohure.didblockchainlessdemo.ui.components.RsaSection
import dev.tohure.didblockchainlessdemo.ui.components.StatusBar
import dev.tohure.didblockchainlessdemo.ui.viewmodel.RsaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RsaScreen(
    vm: RsaViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    if (state.showBiometricPrompt) {
        BiometricPromptHandler(
            showPrompt = true,
            onSuccess = vm::onBiometricSuccess,
            onFailure = vm::onBiometricFailure,
            onDismiss = vm::onBiometricPromptDismissed
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RSA Cifrado", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "RSA-2048 · AES-256-GCM · OAEP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Volver",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state.statusMessage.isNotBlank()) {
                StatusBar(
                    message = state.statusMessage,
                    isLoading = state.isLoading
                )
            }

            RsaSection(
                state = state,
                onGenerate = vm::generateRsaKeys,
                onDelete = vm::deleteRsaKeys
            )

            CredentialSection(
                state = state,
                onUpdateJson = vm::updateJsonInput,
                onFetchAndEncrypt = {
                    vm.fetchAndEncrypt(
                        credentialId = "id-de-la-credencial",
                        token = "token-del-usuario"
                    )
                },
                onEncrypt = vm::encrypt,
                onDecrypt = vm::decrypt
            )

            EncryptedPayloadSection(state = state)

            DecryptedJsonSection(
                state = state,
                onClear = vm::clearDecryptedJson
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}