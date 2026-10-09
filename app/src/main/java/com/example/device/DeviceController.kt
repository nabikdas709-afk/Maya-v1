package com.example.device

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryTelemetry(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val temperatureCelsius: Float = 28.5f,
    val chargingSource: String = "Battery",
    val health: String = "Good"
)

data class VolumeState(
    val currentVolume: Int = 5,
    val maxVolume: Int = 15,
    val isMuted: Boolean = false
)

class DeviceController(private val context: Context) {

    private val tag = "DeviceController"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _batteryState = MutableStateFlow(BatteryTelemetry())
    val batteryState: StateFlow<BatteryTelemetry> = _batteryState.asStateFlow()

    private val _volumeState = MutableStateFlow(VolumeState())
    val volumeState: StateFlow<VolumeState> = _volumeState.asStateFlow()

    init {
        refreshBatteryTelemetry()
        refreshVolumeState()
    }

    fun toggleTorch(forceState: Boolean? = null): Boolean {
        return try {
            val cm = cameraManager ?: return false
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val characteristics = cm.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull() ?: return false

            val targetState = forceState ?: !_isTorchOn.value
            cm.setTorchMode(cameraId, targetState)
            _isTorchOn.value = targetState
            vibrateFeedback(if (targetState) HapticType.CONFIRM else HapticType.CLICK)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to toggle torch: ${e.message}")
            false
        }
    }

    fun refreshBatteryTelemetry(): BatteryTelemetry {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val source = when (chargePlug) {
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC Power"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    else -> if (isCharging) "Charging" else "Discharging"
                }
                val rawTemp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                val tempC = if (rawTemp > 0) rawTemp / 10f else 30.0f
                val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 85

                val telemetry = BatteryTelemetry(
                    levelPercent = percent,
                    isCharging = isCharging,
                    temperatureCelsius = tempC,
                    chargingSource = source,
                    health = "Nominal"
                )
                _batteryState.value = telemetry
                return telemetry
            }
        } catch (e: Exception) {
            Log.e(tag, "Error reading battery: ${e.message}")
        }
        return _batteryState.value
    }

    fun refreshVolumeState() {
        audioManager?.let { am ->
            val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            _volumeState.value = VolumeState(
                currentVolume = current,
                maxVolume = max,
                isMuted = current == 0
            )
        }
    }

    fun adjustVolume(direction: String): String {
        val am = audioManager ?: return "Audio system unavailable"
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        when (direction.lowercase()) {
            "up", "raise", "increase" -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            }
            "down", "lower", "decrease" -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            }
            "mute", "silent" -> {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            }
            "max", "full" -> {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
            }
        }
        refreshVolumeState()
        vibrateFeedback(HapticType.CLICK)
        return "Media Volume set to ${_volumeState.value.currentVolume} of ${_volumeState.value.maxVolume}"
    }

    fun setDirectVolume(level: Int) {
        audioManager?.let { am ->
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val clamped = level.coerceIn(0, max)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, AudioManager.FLAG_SHOW_UI)
            refreshVolumeState()
        }
    }

    fun openWebSearch(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val url = "https://www.google.com/search?q=" + Uri.encode(query)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun launchApp(appName: String): String {
        val pm = context.packageManager
        val cleanName = appName.lowercase().trim()

        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm"
        )

        knownPackages[cleanName]?.let { pkg ->
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "Launching $cleanName"
            }
        }

        when (cleanName) {
            "camera" -> {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return if (tryStartActivity(intent)) "Opening Camera" else "Camera not accessible"
            }
            "calculator" -> {
                val intent = Intent().apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_APP_CALCULATOR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(intent)) return "Opening Calculator"
            }
            "clock", "alarm", "timer" -> {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(intent)) return "Opening Clock"
            }
            "settings" -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(intent)) return "Opening System Settings"
            }
            "phone", "dialer" -> {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(intent)) return "Opening Phone Dialer"
            }
        }

        // Search installed apps by label
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in apps) {
                val label = resolveInfo.loadLabel(pm).toString().lowercase()
                if (label.contains(cleanName) || cleanName.contains(label)) {
                    val pkg = resolveInfo.activityInfo.packageName
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return "Launching ${resolveInfo.loadLabel(pm)}"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error searching apps: ${e.message}")
        }

        return "Could not find app: $appName"
    }

    fun openSystemSettings(action: String): Boolean {
        val intentAction = when (action.lowercase()) {
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(intentAction).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryStartActivity(intent)
    }

    private fun tryStartActivity(intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun vibrateFeedback(type: HapticType = HapticType.CLICK) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.CLICK -> VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticType.CONFIRM -> VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 50), -1)
                    HapticType.ALERT -> VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (e: Exception) {
            Log.e(tag, "Haptic error: ${e.message}")
        }
    }
}

enum class HapticType {
    CLICK,
    CONFIRM,
    ALERT
}
