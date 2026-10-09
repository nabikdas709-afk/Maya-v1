package com.example.ai

sealed interface MayaAction {
    data class SetReminder(val title: String, val timeHint: String) : MayaAction
    data class ToggleTorch(val enable: Boolean) : MayaAction
    data class SearchWeb(val query: String) : MayaAction
    data class AdjustVolume(val direction: String) : MayaAction
    data class LaunchApp(val appName: String) : MayaAction
    object CheckBattery : MayaAction
    data class OpenSetting(val settingName: String) : MayaAction
}

data class MayaTurn(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "MAYA"
    val text: String,
    val spokenText: String = text,
    val timestamp: Long = System.currentTimeMillis(),
    val actionExecuted: MayaAction? = null,
    val actionFeedback: String? = null
)
