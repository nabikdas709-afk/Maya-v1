package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MayaBrain {

    private val tag = "MayaBrain"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    var customApiKey: String? = null

    private fun getActiveApiKey(): String {
        val userKey = customApiKey?.trim()
        if (!userKey.isNullOrEmpty()) return userKey
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    private val systemInstruction = """
        You are Maya (माया), an ultra-advanced, JARVIS-style futuristic AI assistant operating on an Android mobile device.
        
        IDENTITY & PERSONA:
        - Name: Maya (माया).
        - Persona: Loyal, witty, respectful, highly knowledgeable, proactive, and sharp. 
        - Voice style: Respectful Indian Hindi female tone. Address the user respectfully as 'Boss' or 'Sir/Ma'am' (e.g. "Ji Boss, main Maya hoon", "Bilkul Boss!").
        - Primary Language: Natural conversational Hindi (and Hinglish). If the user asks in Hindi, reply in fluent, polite Hindi. If the user asks in English, reply in articulate English with your signature Maya warmth.
        - Knowledge: Broad and deep expertise across science, technology, astronomy, mathematics, programming, history, daily tasks, health, and current concepts.
        
        MOBILE DEVICE CONTROL & ACTIONS:
        You can execute actions on the user's Android phone by appending special action tags at the end of your response:
        - To set a reminder: [ACTION:REMINDER title="Clean room" time="15 minutes"]
        - To turn torch/flashlight on or off: [ACTION:TORCH state="on"] or [ACTION:TORCH state="off"]
        - To search the web: [ACTION:SEARCH query="ISRO latest launch"]
        - To adjust media volume: [ACTION:VOLUME level="up"|"down"|"mute"|"max"]
        - To launch an app: [ACTION:APP name="camera"|"youtube"|"whatsapp"|"maps"|"settings"|"calculator"|"clock"|"phone"]
        - To check battery: [ACTION:BATTERY]
        - To open system settings: [ACTION:SETTING name="wifi"|"bluetooth"|"display"]
        
        GUIDELINES:
        1. Keep responses punchy, confident, and direct like a sci-fi tactical AI. Avoid unnecessary disclaimers.
        2. When an action is requested, confirm it concisely and include the tag.
        3. For knowledge questions, explain clearly, accurately, and interestingly.
    """.trimIndent()

    suspend fun think(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>
    ): Pair<String, MayaAction?> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()

        // Check if API key is valid / configured
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext runLocalFallback(userPrompt)
        }

        try {
            // Using gemini-3.5-flash as mandated in skill for basic/conversational text
            val modelName = "gemini-3.5-flash"
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val rootJson = JSONObject()

            // System instruction
            val sysInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                }
                put("parts", partsArray)
            }
            rootJson.put("systemInstruction", sysInstructionObj)

            // Contents array
            val contentsArray = JSONArray()

            // Include last 6 turns for context
            val recentTurns = conversationHistory.takeLast(6)
            for ((role, text) in recentTurns) {
                val turnObj = JSONObject().apply {
                    put("role", if (role.equals("USER", ignoreCase = true)) "user" else "model")
                    val partsArr = JSONArray().apply {
                        put(JSONObject().put("text", text))
                    }
                    put("parts", partsArr)
                }
                contentsArray.put(turnObj)
            }

            // Current prompt
            val currentTurn = JSONObject().apply {
                put("role", "user")
                val partsArr = JSONArray().apply {
                    put(JSONObject().put("text", userPrompt))
                }
                put("parts", partsArr)
            }
            contentsArray.put(currentTurn)

            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("maxOutputTokens", 800)
            }
            rootJson.put("generationConfig", genConfig)

            val request = Request.Builder()
                .url(endpoint)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w(tag, "Gemini API error ${response.code}: $responseBody")
                return@withContext runLocalFallback(userPrompt, apiError = "Gemini server status: ${response.code}")
            }

            val resJson = JSONObject(responseBody)
            val candidates = resJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val rawReply = firstPart?.optString("text", "") ?: ""

            if (rawReply.isBlank()) {
                return@withContext runLocalFallback(userPrompt)
            }

            // Parse response & extract actions
            parseReplyAndAction(rawReply)
        } catch (e: Exception) {
            Log.e(tag, "Error querying Gemini API: ${e.message}", e)
            runLocalFallback(userPrompt, apiError = e.message)
        }
    }

    private fun parseReplyAndAction(rawReply: String): Pair<String, MayaAction?> {
        var action: MayaAction? = null

        // Detect action tags
        val reminderRegex = Regex("\\[ACTION:REMINDER\\s+title=\"([^\"]+)\"\\s+time=\"([^\"]+)\"\\]")
        val torchRegex = Regex("\\[ACTION:TORCH\\s+state=\"([^\"]+)\"\\]")
        val searchRegex = Regex("\\[ACTION:SEARCH\\s+query=\"([^\"]+)\"\\]")
        val volumeRegex = Regex("\\[ACTION:VOLUME\\s+level=\"([^\"]+)\"\\]")
        val appRegex = Regex("\\[ACTION:APP\\s+name=\"([^\"]+)\"\\]")
        val batteryRegex = Regex("\\[ACTION:BATTERY\\]")
        val settingRegex = Regex("\\[ACTION:SETTING\\s+name=\"([^\"]+)\"\\]")

        reminderRegex.find(rawReply)?.let { match ->
            action = MayaAction.SetReminder(match.groupValues[1], match.groupValues[2])
        }
        torchRegex.find(rawReply)?.let { match ->
            val state = match.groupValues[1].equals("on", ignoreCase = true)
            action = MayaAction.ToggleTorch(state)
        }
        searchRegex.find(rawReply)?.let { match ->
            action = MayaAction.SearchWeb(match.groupValues[1])
        }
        volumeRegex.find(rawReply)?.let { match ->
            action = MayaAction.AdjustVolume(match.groupValues[1])
        }
        appRegex.find(rawReply)?.let { match ->
            action = MayaAction.LaunchApp(match.groupValues[1])
        }
        if (batteryRegex.containsMatchIn(rawReply)) {
            action = MayaAction.CheckBattery
        }
        settingRegex.find(rawReply)?.let { match ->
            action = MayaAction.OpenSetting(match.groupValues[1])
        }

        // Clean text for speech and display
        val cleanReply = rawReply
            .replace(reminderRegex, "")
            .replace(torchRegex, "")
            .replace(searchRegex, "")
            .replace(volumeRegex, "")
            .replace(appRegex, "")
            .replace(batteryRegex, "")
            .replace(settingRegex, "")
            .trim()

        return Pair(cleanReply.ifEmpty { "Ji Boss, aadesh pura ho gaya!" }, action)
    }

    private fun runLocalFallback(prompt: String, apiError: String? = null): Pair<String, MayaAction?> {
        val lower = prompt.lowercase().trim()

        // 1. Torch control
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("light") || lower.contains("roshni")) {
            val turnOn = !lower.contains("off") && !lower.contains("band")
            val action = MayaAction.ToggleTorch(turnOn)
            val reply = if (turnOn) {
                "Ji Boss, torch on kar di gayi hai!"
            } else {
                "Ji Boss, torch off kar di gayi hai!"
            }
            return Pair(reply, action)
        }

        // 2. Reminders
        if (lower.contains("reminder") || lower.contains("yaad") || lower.contains("remind")) {
            val title = extractReminderTitle(prompt)
            val action = MayaAction.SetReminder(title, "in 15 minutes")
            val reply = "Ji Boss! '$title' ka reminder set kar diya gaya hai. Main samay par aapko soochit karungi!"
            return Pair(reply, action)
        }

        // 3. Web Search
        if (lower.contains("search") || lower.contains("google") || lower.contains("dhoondo") || lower.contains("khojo")) {
            val query = prompt
                .replace(Regex("(?i)^(maya|search|google|dhoondo|khojo|for|web)\\s+"), "")
                .trim()
                .ifEmpty { prompt }
            val action = MayaAction.SearchWeb(query)
            val reply = "Ji Boss, web par '$query' search kiya ja raha hai!"
            return Pair(reply, action)
        }

        // 4. Volume control
        if (lower.contains("volume") || lower.contains("sound") || lower.contains("awaaz")) {
            val dir = when {
                lower.contains("mute") || lower.contains("chup") || lower.contains("band") -> "mute"
                lower.contains("kam") || lower.contains("down") || lower.contains("ghatao") -> "down"
                lower.contains("tez") || lower.contains("badhao") || lower.contains("up") -> "up"
                lower.contains("full") || lower.contains("max") -> "max"
                else -> "up"
            }
            val action = MayaAction.AdjustVolume(dir)
            val reply = "Ji Boss, volume adjust kar diya gaya hai!"
            return Pair(reply, action)
        }

        // 5. Battery
        if (lower.contains("battery") || lower.contains("charge") || lower.contains("charging")) {
            return Pair("Ji Boss, battery diagnostics check kar rahi hoon...", MayaAction.CheckBattery)
        }

        // 6. Apps
        val apps = listOf("youtube", "whatsapp", "camera", "maps", "calculator", "settings", "clock", "phone")
        for (app in apps) {
            if (lower.contains(app) || (app == "camera" && lower.contains("photo"))) {
                return Pair("Ji Boss, $app khol rahi hoon!", MayaAction.LaunchApp(app))
            }
        }

        // 7. Settings
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            return Pair("Ji Boss, Wi-Fi settings open kar rahi hoon.", MayaAction.OpenSetting("wifi"))
        }
        if (lower.contains("bluetooth")) {
            return Pair("Ji Boss, Bluetooth settings open kar rahi hoon.", MayaAction.OpenSetting("bluetooth"))
        }

        // 8. Greetings & Knowledge
        if (lower.contains("namaste") || lower.contains("hello") || lower.contains("hi") || lower.contains("kaise ho") || lower.contains("who are you") || lower.contains("kon ho")) {
            val reply = "Namaste Boss! Main Maya hoon, aapki futuristic JARVIS AI assistant. Main aapke mobile control, reminders, web search aur kisi bhi vishay par sahayata ke liye taiyar hoon. Boliye, kya aadesh hai?"
            return Pair(reply, null)
        }

        // Fallback knowledge response
        val defaultText = buildString {
            append("Ji Boss, maine aapka sandesh samjha: \"$prompt\". ")
            if (apiError != null) {
                append("Local neural core se execute kiya gaya hai. Agar aap deep Gemini AI reasoning chahte hain, toh Settings mein jaakar apni free Gemini API key verify kar sakte hain!")
            } else {
                append("Quantum Core active hai. Boliye Boss, agla aadesh kya hai?")
            }
        }
        return Pair(defaultText, null)
    }

    private fun extractReminderTitle(prompt: String): String {
        return prompt
            .replace(Regex("(?i)(maya|reminder|set|karo|lagao|remind|me|to|for|about|yaad dilao)"), "")
            .trim()
            .ifEmpty { "Zaroori Kaam (Task)" }
    }
}
