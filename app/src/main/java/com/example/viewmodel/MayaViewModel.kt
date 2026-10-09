package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.MayaAction
import com.example.ai.MayaBrain
import com.example.ai.MayaTurn
import com.example.data.database.AppDatabase
import com.example.data.database.ReminderEntity
import com.example.device.DeviceController
import com.example.device.HapticType
import com.example.voice.MayaVoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HudTab {
    CORE,
    REMINDERS,
    DEVICE,
    KNOWLEDGE,
    SETTINGS
}

class MayaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val reminderDao = db.reminderDao()

    val deviceController = DeviceController(application)
    val voiceEngine = MayaVoiceEngine(application, viewModelScope)
    val brain = MayaBrain()

    private val _currentTab = MutableStateFlow(HudTab.CORE)
    val currentTab: StateFlow<HudTab> = _currentTab.asStateFlow()

    private val _conversations = MutableStateFlow<List<MayaTurn>>(emptyList())
    val conversations: StateFlow<List<MayaTurn>> = _conversations.asStateFlow()

    val reminders: StateFlow<List<ReminderEntity>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isListening = voiceEngine.isListening
    val isSpeaking = voiceEngine.isSpeaking
    val audioRmsLevel = voiceEngine.audioRmsLevel
    val voiceStatusMessage = voiceEngine.voiceStatusMessage

    val batteryState = deviceController.batteryState
    val isTorchOn = deviceController.isTorchOn
    val volumeState = deviceController.volumeState

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    init {
        // Initial Maya Greeting
        viewModelScope.launch {
            val welcomeText = "Namaste Boss! Main Maya hoon, aapki JARVIS-style AI assistant. Main mobile control, reminders, web search aur high knowledge tasks ke liye taiyar hoon. Boliye, main aapki kya madad kar sakti hoon?"
            val welcomeTurn = MayaTurn(
                sender = "MAYA",
                text = welcomeText,
                spokenText = welcomeText
            )
            _conversations.value = listOf(welcomeTurn)
        }
    }

    fun setTab(tab: HudTab) {
        _currentTab.value = tab
        deviceController.vibrateFeedback(HapticType.CLICK)
    }

    fun setTextInput(text: String) {
        _textInput.value = text
    }

    fun toggleVoiceListening() {
        if (isListening.value) {
            voiceEngine.stopListening()
        } else {
            deviceController.vibrateFeedback(HapticType.CLICK)
            voiceEngine.startListening(
                onResult = { query ->
                    processUserCommand(query)
                },
                onError = {
                    // handled by voice engine status
                }
            )
        }
    }

    fun submitTextCommand() {
        val query = _textInput.value.trim()
        if (query.isNotEmpty()) {
            _textInput.value = ""
            processUserCommand(query)
        }
    }

    fun processUserCommand(query: String) {
        if (query.isBlank()) return

        val userTurn = MayaTurn(
            sender = "USER",
            text = query
        )
        _conversations.value = _conversations.value + userTurn
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                // Build history
                val history = _conversations.value.map { it.sender to it.text }
                val (replyText, action) = brain.think(query, history)

                var actionFeedback: String? = null

                // Execute device action if triggered
                action?.let { act ->
                    actionFeedback = executeAction(act)
                }

                val mayaTurn = MayaTurn(
                    sender = "MAYA",
                    text = replyText,
                    spokenText = replyText,
                    actionExecuted = action,
                    actionFeedback = actionFeedback
                )

                _conversations.value = _conversations.value + mayaTurn
                _isProcessing.value = false

                // Speak response in female Hindi voice
                voiceEngine.speak(replyText)
            } catch (e: Exception) {
                _isProcessing.value = false
                val errorTurn = MayaTurn(
                    sender = "MAYA",
                    text = "Boss, command process karte waqt anapekshit issue aaya: ${e.message}"
                )
                _conversations.value = _conversations.value + errorTurn
            }
        }
    }

    private suspend fun executeAction(action: MayaAction): String {
        return when (action) {
            is MayaAction.SetReminder -> {
                val targetMillis = System.currentTimeMillis() + 15 * 60 * 1000 // 15 mins default
                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(targetMillis))
                val reminder = ReminderEntity(
                    title = action.title,
                    targetTimeMillis = targetMillis,
                    formattedDateTime = dateStr,
                    category = "Voice Task"
                )
                reminderDao.insertReminder(reminder)
                deviceController.vibrateFeedback(HapticType.CONFIRM)
                "Reminder '$action.title' scheduled for $dateStr"
            }
            is MayaAction.ToggleTorch -> {
                val ok = deviceController.toggleTorch(action.enable)
                if (ok) "Torch state updated to ${if (action.enable) "ON" else "OFF"}" else "Camera flash hardware error"
            }
            is MayaAction.SearchWeb -> {
                deviceController.openWebSearch(action.query)
                "Launched web search for: ${action.query}"
            }
            is MayaAction.AdjustVolume -> {
                deviceController.adjustVolume(action.direction)
            }
            is MayaAction.LaunchApp -> {
                deviceController.launchApp(action.appName)
            }
            is MayaAction.CheckBattery -> {
                val b = deviceController.refreshBatteryTelemetry()
                "Battery is at ${b.levelPercent}%, ${if (b.isCharging) "Charging" else "Discharging"}"
            }
            is MayaAction.OpenSetting -> {
                deviceController.openSystemSettings(action.settingName)
                "Opened ${action.settingName} system settings"
            }
        }
    }

    fun speakText(text: String) {
        voiceEngine.speak(text)
    }

    fun stopSpeaking() {
        voiceEngine.stopSpeaking()
    }

    fun addManualReminder(title: String, timeOffsetMinutes: Int = 30) {
        viewModelScope.launch {
            val target = System.currentTimeMillis() + (timeOffsetMinutes * 60 * 1000L)
            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(target))
            val entity = ReminderEntity(
                title = title,
                targetTimeMillis = target,
                formattedDateTime = dateStr,
                category = "Scheduled"
            )
            reminderDao.insertReminder(entity)
            deviceController.vibrateFeedback(HapticType.CONFIRM)
            voiceEngine.speak("Boss, reminder '$title' add ho gaya hai.")
        }
    }

    fun toggleReminderStatus(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderDao.updateStatus(reminder.id, !reminder.isCompleted)
            deviceController.vibrateFeedback(HapticType.CLICK)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
            deviceController.vibrateFeedback(HapticType.CLICK)
        }
    }

    fun setTorchState(enable: Boolean) {
        deviceController.toggleTorch(enable)
    }

    fun setDirectVolume(level: Int) {
        deviceController.setDirectVolume(level)
    }

    fun launchApp(appName: String) {
        deviceController.launchApp(appName)
    }

    fun openWebSearch(query: String) {
        deviceController.openWebSearch(query)
    }

    fun updateVoicePitch(newPitch: Float) {
        voiceEngine.pitch = newPitch
    }

    fun updateVoiceSpeed(newSpeed: Float) {
        voiceEngine.speechRate = newSpeed
    }

    fun updateCustomApiKey(key: String) {
        brain.customApiKey = key
    }

    fun testHindiVoice() {
        voiceEngine.speak("Boss, Maya ka Hindi voice synthesizer nominal hai aur perfect work kar raha hai!")
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.cleanup()
    }
}
