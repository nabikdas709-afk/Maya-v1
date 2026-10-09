package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcReactorCore
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicArcReactor(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    audioRms: Float = 0f,
    isListening: Boolean = false,
    isSpeaking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorRotation")

    // Slow clockwise rotation for outer ring
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterRing"
    )

    // Counter-clockwise rotation for middle segmented ring
    val middleAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MiddleRing"
    )

    // Breathing pulse for core
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CorePulse"
    )

    // Activity intensity
    val activityMultiplier = when {
        isSpeaking -> 1.8f
        isListening -> 1.5f
        else -> 1.0f
    }
    val effectiveRms = (audioRms * activityMultiplier).coerceIn(0f, 2.5f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.9f

            // 1. Outer Glow Aura
            val auraColor = when {
                isSpeaking -> ElectricGold.copy(alpha = 0.25f + effectiveRms * 0.15f)
                isListening -> NeonCyan.copy(alpha = 0.3f + effectiveRms * 0.2f)
                else -> NeonCyan.copy(alpha = 0.12f)
            }
            drawCircle(
                color = auraColor,
                radius = baseRadius * 0.98f,
                center = center
            )

            // 2. Outer Track Ring with dashes
            rotate(outerAngle, pivot = center) {
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.5f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f, 6f, 10f), 0f)
                    )
                )

                // 4 Tactical Cardinal Marks on Outer Ring
                val cardinalLen = 14f
                for (i in 0 until 4) {
                    val angleRad = Math.toRadians((i * 90.0)).toFloat()
                    val start = Offset(
                        center.x + (baseRadius - cardinalLen) * cos(angleRad),
                        center.y + (baseRadius - cardinalLen) * sin(angleRad)
                    )
                    val end = Offset(
                        center.x + (baseRadius + 6f) * cos(angleRad),
                        center.y + (baseRadius + 6f) * sin(angleRad)
                    )
                    drawLine(
                        color = ElectricGold,
                        start = start,
                        end = end,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Audio-Reactive Spectrum Waveform Ring
            val waveRadius = baseRadius * 0.82f
            val barsCount = 36
            for (i in 0 until barsCount) {
                val barAngle = (i * (360f / barsCount)) + middleAngle
                val rad = Math.toRadians(barAngle.toDouble()).toFloat()

                // Calculate reactive bar height
                val variance = kotlin.math.abs(sin(i * 0.6f + (if (isSpeaking) outerAngle else middleAngle) * 0.05f))
                val barLength = 8f + (variance * 28f * (0.3f + effectiveRms))

                val start = Offset(
                    center.x + (waveRadius - barLength / 2f) * cos(rad),
                    center.y + (waveRadius - barLength / 2f) * sin(rad)
                )
                val end = Offset(
                    center.x + (waveRadius + barLength / 2f) * cos(rad),
                    center.y + (waveRadius + barLength / 2f) * sin(rad)
                )

                val barColor = if (i % 3 == 0) ElectricGold else NeonCyan
                drawLine(
                    color = barColor.copy(alpha = (0.4f + effectiveRms * 0.5f).coerceAtMost(1f)),
                    start = start,
                    end = end,
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }

            // 4. Middle Arc Segment Ring (Rotates Counter-Clockwise)
            rotate(middleAngle, pivot = center) {
                val midRadius = baseRadius * 0.62f
                // 3 segmented arcs
                for (arc in 0 until 3) {
                    val startAngle = arc * 120f + 15f
                    drawArc(
                        color = NeonCyan.copy(alpha = 0.8f),
                        startAngle = startAngle,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(center.x - midRadius, center.y - midRadius),
                        size = androidx.compose.ui.geometry.Size(midRadius * 2, midRadius * 2),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
            }

            // 5. Inner Core Ring
            val innerRadius = baseRadius * 0.42f * (corePulse + effectiveRms * 0.1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ArcReactorCore,
                        NeonCyan.copy(alpha = 0.8f),
                        NeonCyan.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = innerRadius * 1.3f
                ),
                radius = innerRadius,
                center = center
            )

            // Inner crisp border
            drawCircle(
                color = ElectricGold,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }

        // Center HUD Emblem / Text
        Text(
            text = "MAYA",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
    }
}
