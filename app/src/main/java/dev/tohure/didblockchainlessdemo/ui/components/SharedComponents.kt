package dev.tohure.didblockchainlessdemo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.didblockchainlessdemo.R
import dev.tohure.didblockchainlessdemo.crypto.SecurityLevel

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
fun KeyStatusRow(
    exists: Boolean,
    securityLevel: SecurityLevel,
    presentLabel: String = "Claves presentes",
    absentLabel: String = "No hay claves generadas",
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val color = if (exists) Color(0xFF81C784) else Color(0xFFFF6B6B)
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(
            text = if (exists) presentLabel else absentLabel,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
        if (exists) {
            Text(
                text = when (securityLevel) {
                    SecurityLevel.STRONGBOX -> "🔒 StrongBox"
                    SecurityLevel.TEE -> "🛡 TEE"
                    SecurityLevel.SOFTWARE -> "⚠️ Software"
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
}

@Composable
fun ExpandableMonoBox(
    label: String,
    hiddenLabel: String,
    content: String,
    topPadding: Dp = 12.dp,
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
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
        }
    }
}

@Composable
fun ActionButton(
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
fun StatusBar(message: String) {
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