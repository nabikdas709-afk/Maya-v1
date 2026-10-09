package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MayaViewModel

@Composable
fun DeviceControlScreen(
    viewModel: MayaViewModel,
    modifier: Modifier = Modifier
) {
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val volumeState by viewModel.volumeState.collectAsState()
    val batteryState by viewModel.batteryState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.2f))
                    .border(1.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "DEVICE CONTROL CENTER",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Hardware Integration & Systems Hub",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Flashlight / Torch Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("torch_control_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isTorchOn) ElectricGold else SurfaceCardBorder
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isTorchOn) ElectricGold.copy(alpha = 0.25f) else SurfaceCardElevated)
                            .border(
                                1.dp,
                                if (isTorchOn) ElectricGold else SurfaceCardBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Flashlight",
                            tint = if (isTorchOn) ElectricGold else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "FLASHLIGHT / TORCH",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isTorchOn) "ACTIVE • ILLUMINATING" else "OFFLINE • STANDBY",
                            color = if (isTorchOn) ElectricGold else TextMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Switch(
                    checked = isTorchOn,
                    onCheckedChange = { viewModel.setTorchState(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpaceBlack,
                        checkedTrackColor = ElectricGold,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.testTag("torch_toggle_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Volume Controls Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("volume_control_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUDIO VOLUME MASTER",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${volumeState.currentVolume} / ${volumeState.maxVolume}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = volumeState.currentVolume.toFloat(),
                    onValueChange = { viewModel.setDirectVolume(it.toInt()) },
                    valueRange = 0f..volumeState.maxVolume.toFloat(),
                    steps = volumeState.maxVolume - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = SurfaceCardElevated
                    ),
                    modifier = Modifier.testTag("volume_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VolumeButton(
                        text = "Mute",
                        icon = Icons.AutoMirrored.Filled.VolumeMute,
                        onClick = { viewModel.deviceController.adjustVolume("mute") },
                        modifier = Modifier.weight(1f)
                    )
                    VolumeButton(
                        text = "Down",
                        icon = Icons.AutoMirrored.Filled.VolumeDown,
                        onClick = { viewModel.deviceController.adjustVolume("down") },
                        modifier = Modifier.weight(1f)
                    )
                    VolumeButton(
                        text = "Up",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        onClick = { viewModel.deviceController.adjustVolume("up") },
                        modifier = Modifier.weight(1f)
                    )
                    VolumeButton(
                        text = "Max",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        onClick = { viewModel.deviceController.adjustVolume("max") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Battery Telemetry Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("battery_telemetry_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "POWER & THERMAL TELEMETRY",
                    color = MatrixGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryItem(
                        icon = if (batteryState.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        label = "CHARGE",
                        value = "${batteryState.levelPercent}%",
                        color = MatrixGreen
                    )
                    TelemetryItem(
                        icon = Icons.Default.Thermostat,
                        label = "THERMALS",
                        value = "${batteryState.temperatureCelsius}°C",
                        color = if (batteryState.temperatureCelsius > 40f) NeonCoral else NeonCyan
                    )
                    TelemetryItem(
                        icon = Icons.Default.FlashOn,
                        label = "STATE",
                        value = if (batteryState.isCharging) "CHARGING" else "DRAINING",
                        color = ElectricGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Quick App Launchers Grid
        Text(
            text = "TACTICAL APP LAUNCHERS",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        val apps = listOf(
            AppLauncherData("Camera", Icons.Default.CameraAlt, "camera"),
            AppLauncherData("YouTube", Icons.Default.PlayCircle, "youtube"),
            AppLauncherData("WhatsApp", Icons.AutoMirrored.Filled.Chat, "whatsapp"),
            AppLauncherData("Maps", Icons.Default.Map, "maps"),
            AppLauncherData("Calculator", Icons.Default.Calculate, "calculator"),
            AppLauncherData("Clock", Icons.Default.Alarm, "clock"),
            AppLauncherData("Phone", Icons.Default.Phone, "phone"),
            AppLauncherData("Settings", Icons.Default.Settings, "settings")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            apps.take(4).forEach { app ->
                AppLauncherTile(app = app, onClick = { viewModel.launchApp(app.identifier) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            apps.drop(4).forEach { app ->
                AppLauncherTile(app = app, onClick = { viewModel.launchApp(app.identifier) }, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. System Settings Quick Links
        Text(
            text = "SYSTEM SETTINGS SHORTCUTS",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsShortcutButton(
                title = "Wi-Fi",
                icon = Icons.Default.Wifi,
                onClick = { viewModel.deviceController.openSystemSettings("wifi") },
                modifier = Modifier.weight(1f)
            )
            SettingsShortcutButton(
                title = "Bluetooth",
                icon = Icons.Default.Bluetooth,
                onClick = { viewModel.deviceController.openSystemSettings("bluetooth") },
                modifier = Modifier.weight(1f)
            )
            SettingsShortcutButton(
                title = "Display",
                icon = Icons.Default.DisplaySettings,
                onClick = { viewModel.deviceController.openSystemSettings("display") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

data class AppLauncherData(
    val name: String,
    val icon: ImageVector,
    val identifier: String
)

@Composable
fun AppLauncherTile(
    app: AppLauncherData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag("app_launcher_${app.identifier}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = app.icon,
                contentDescription = app.name,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.name,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun VolumeButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceCardElevated,
            contentColor = TextPrimary
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 4.dp),
        modifier = modifier.testTag("vol_btn_${text.lowercase()}")
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TelemetryItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = TextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SettingsShortcutButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCardElevated)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag("setting_shortcut_${title.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
