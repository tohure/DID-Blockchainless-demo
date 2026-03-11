package dev.tohure.didblockchainlessdemo.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import dev.tohure.didblockchainlessdemo.ui.navigation.AppNavHost
import dev.tohure.didblockchainlessdemo.ui.theme.DIDBlockchainlessDemoTheme

class MainActivity : FragmentActivity() {
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