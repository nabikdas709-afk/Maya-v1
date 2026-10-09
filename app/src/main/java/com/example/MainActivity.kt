package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.CoreHudScreen
import com.example.ui.screens.DeviceControlScreen
import com.example.ui.screens.KnowledgeHubScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.MayaTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.TextMuted
import com.example.viewmodel.HudTab
import com.example.viewmodel.MayaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MayaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MayaTheme {
                MayaApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MayaApp(viewModel: MayaViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()

    // Runtime Permission Request for Record Audio and Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            // Audio permission granted
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Back handling: If on secondary tab, return to CORE HUD
    if (currentTab != HudTab.CORE) {
        BackHandler {
            viewModel.setTab(HudTab.CORE)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            MayaBottomBar(
                currentTab = currentTab,
                onSelectTab = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SpaceBlack)
        ) {
            when (currentTab) {
                HudTab.CORE -> CoreHudScreen(viewModel = viewModel)
                HudTab.REMINDERS -> RemindersScreen(viewModel = viewModel)
                HudTab.DEVICE -> DeviceControlScreen(viewModel = viewModel)
                HudTab.KNOWLEDGE -> KnowledgeHubScreen(viewModel = viewModel)
                HudTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavItem(
    val tab: HudTab,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun MayaBottomBar(
    currentTab: HudTab,
    onSelectTab: (HudTab) -> Unit
) {
    val items = listOf(
        NavItem(HudTab.CORE, "CORE", Icons.Default.GraphicEq, "nav_core"),
        NavItem(HudTab.REMINDERS, "TASKS", Icons.Default.NotificationsActive, "nav_reminders"),
        NavItem(HudTab.DEVICE, "DEVICE", Icons.Default.Tune, "nav_device"),
        NavItem(HudTab.KNOWLEDGE, "INTEL", Icons.Default.Psychology, "nav_knowledge"),
        NavItem(HudTab.SETTINGS, "SYSTEM", Icons.Default.Settings, "nav_settings")
    )

    NavigationBar(
        containerColor = SurfaceCard,
        contentColor = NeonCyan,
        tonalElevation = 8.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        items.forEach { item ->
            val isSelected = currentTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) NeonCyan else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) NeonCyan else TextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = NeonCyan.copy(alpha = 0.18f),
                    selectedIconColor = NeonCyan,
                    unselectedIconColor = TextMuted
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
