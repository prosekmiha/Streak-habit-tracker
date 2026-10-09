package com.streak.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.streak.app.ui.theme.InkOnPastel

val PillShape = RoundedCornerShape(20.dp)

// Glavni gumb (polna sirina, zaobljen): primarna barva ali bel na barvnem ozadju
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.fillMaxWidth().height(56.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

// Bel gumb z modrim besedilom (na barvnih ozadjih, npr. onboarding)
@Composable
fun WhitePillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    PillButton(text, onClick, modifier, enabled, containerColor = Color.White, contentColor = InkOnPastel)

// Podcrtana povezava (npr. "Obnovi iz varnostne kopije")
@Composable
fun LinkButton(text: String, onClick: () -> Unit, color: Color, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.defaultMinSize(minHeight = 48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
