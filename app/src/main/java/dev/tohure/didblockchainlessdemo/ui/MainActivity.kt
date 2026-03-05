package dev.tohure.didblockchainlessdemo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tohure.didblockchainlessdemo.ui.components.CredentialSection
import dev.tohure.didblockchainlessdemo.ui.components.DecryptedJsonSection
import dev.tohure.didblockchainlessdemo.ui.components.DidSection
import dev.tohure.didblockchainlessdemo.ui.components.EncryptedPayloadSection
import dev.tohure.didblockchainlessdemo.ui.components.ProofJwtSection
import dev.tohure.didblockchainlessdemo.ui.components.RsaSection
import dev.tohure.didblockchainlessdemo.ui.components.StatusBar
import dev.tohure.didblockchainlessdemo.ui.theme.DIDBlockchainlessDemoTheme
import dev.tohure.didblockchainlessdemo.ui.viewmodel.CredentialViewModel
import dev.tohure.didblockchainlessdemo.ui.viewmodel.decrypt
import dev.tohure.didblockchainlessdemo.ui.viewmodel.deleteDIDKeys
import dev.tohure.didblockchainlessdemo.ui.viewmodel.deleteRsaKeys
import dev.tohure.didblockchainlessdemo.ui.viewmodel.encrypt
import dev.tohure.didblockchainlessdemo.ui.viewmodel.fetchAndEncrypt
import dev.tohure.didblockchainlessdemo.ui.viewmodel.generateDIDKeys
import dev.tohure.didblockchainlessdemo.ui.viewmodel.generateRsaKeys
import dev.tohure.didblockchainlessdemo.ui.viewmodel.requestCredentialWithNonce

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

            DidSection(
                state = state,
                onGenerate = vm::generateDIDKeys,
                onDelete = vm::deleteDIDKeys
            )

            ProofJwtSection(
                state = state,
                onRequest = { vm.requestCredentialWithNonce() },
                onClear = vm::clearProofJwt
            )

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
                Text("DID Demo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "did:key · secp256k1 · RSA-2048 + AES-256-GCM",
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