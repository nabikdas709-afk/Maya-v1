package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpaceBlack

@Composable
fun VoiceOrbButton(
    modifier: Modifier = Modifier,
    isListening: Boolean,
    isSpeaking: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceOrbPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening || isSpeaking) 1.25f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 800 else if (isSpeaking) 1000 else 2400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val activeColor = when {
        isSpeaking -> ElectricGold
        isListening -> NeonCyan
        else -> NeonCyan
    }

    Box(
        modifier = modifier
            .size(96.dp)
            .testTag("voice_orb_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer Expanding Ripple Ring
        if (isListening || isSpeaking) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.2f))
                    .border(1.5.dp, activeColor.copy(alpha = 0.5f), CircleShape)
            )
        }

        // Inner Tactical Orb
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isSpeaking) ElectricGold else NeonCyan,
                            if (isSpeaking) Color(0xFFB48200) else Color(0xFF00758F),
                            SpaceBlack
                        )
                    )
                )
                .border(2.dp, activeColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White)
                ) {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            val icon = when {
                isListening -> Icons.Default.GraphicEq
                isSpeaking -> Icons.Default.Stop
                else -> Icons.Default.Mic
            }
            Icon(
                imageVector = icon,
                contentDescription = if (isListening) "Stop Listening" else if (isSpeaking) "Stop Speaking" else "Start Voice Command",
                tint = if (isListening || isSpeaking) SpaceBlack else Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
