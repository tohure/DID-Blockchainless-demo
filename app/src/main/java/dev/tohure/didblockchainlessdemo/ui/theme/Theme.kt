package dev.tohure.didblockchainlessdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DIDBlockchainlessDemoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF003549),
            primaryContainer = Color(0xFF004D61),
            secondary = Color(0xFF80CBC4),
            surface = Color(0xFF0D1B2A),
            surfaceVariant = Color(0xFF1A2E40),
            background = Color(0xFF061424),
            onBackground = Color(0xFFE1F5FE),
            onSurface = Color(0xFFCFE8F4),
            error = Color(0xFFFF6B6B),
            outline = Color(0xFF2A4A62)
        ),
        content = content
    )
}