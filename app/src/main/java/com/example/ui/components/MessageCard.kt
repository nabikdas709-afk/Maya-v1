package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.MayaAction
import com.example.ai.MayaTurn
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageCard(
    modifier: Modifier = Modifier,
    turn: MayaTurn,
    onReplaySpeech: (String) -> Unit
) {
    val isMaya = turn.sender.equals("MAYA", ignoreCase = true)
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(turn.timestamp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMaya) Alignment.Start else Alignment.End
    ) {
        // Sender Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isMaya) "MAYA AI" else "BOSS",
                color = if (isMaya) NeonCyan else ElectricGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = timeStr,
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Message Bubble Box
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMaya) 2.dp else 16.dp,
                        bottomEnd = if (isMaya) 16.dp else 2.dp
                    )
                )
                .background(if (isMaya) SurfaceCardElevated else SurfaceCard)
                .border(
                    width = 1.dp,
                    color = if (isMaya) NeonCyan.copy(alpha = 0.35f) else ElectricGold.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMaya) 2.dp else 16.dp,
                        bottomEnd = if (isMaya) 16.dp else 2.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = turn.text,
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp
                )

                // Action execution pill if action was triggered
                turn.actionExecuted?.let { action ->
                    Spacer(modifier = Modifier.height(10.dp))
                    ActionPill(action = action, feedback = turn.actionFeedback)
                }

                // Controls row for Maya responses
                if (isMaya) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onReplaySpeech(turn.spokenText) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("replay_voice_btn_${turn.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Hear Hindi Voice",
                                tint = NeonCyan,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(turn.text)) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("copy_text_btn_${turn.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Response",
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionPill(
    action: MayaAction,
    feedback: String?
) {
    val (label, color) = when (action) {
        is MayaAction.SetReminder -> "REMINDER SCHEDULED: ${action.title}" to ElectricGold
        is MayaAction.ToggleTorch -> "TORCH ${if (action.enable) "ACTIVATED" else "DEACTIVATED"}" to NeonCyan
        is MayaAction.SearchWeb -> "WEB SEARCH: ${action.query}" to NeonCyan
        is MayaAction.AdjustVolume -> "VOLUME ADJUSTED: ${action.direction.uppercase()}" to MatrixGreen
        is MayaAction.LaunchApp -> "APP LAUNCHED: ${action.appName.uppercase()}" to MatrixGreen
        is MayaAction.CheckBattery -> "BATTERY TELEMETRY RETRIEVED" to MatrixGreen
        is MayaAction.OpenSetting -> "SYSTEM SETTING: ${action.settingName.uppercase()}" to NeonCyan
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = "⚡ $label",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (!feedback.isNullOrBlank()) {
                Text(
                    text = feedback,
                    color = TextSecondary,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}
