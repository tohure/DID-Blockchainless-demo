package dev.tohure.didblockchainlessdemo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.tohure.didblockchainlessdemo.ui.navigation.AppNavHost
import dev.tohure.didblockchainlessdemo.ui.theme.DIDBlockchainlessDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DIDBlockchainlessDemoTheme {
                AppNavHost()
            }
        }
    }
}