package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class MayaVoiceEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private val tag = "MayaVoiceEngine"

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow("Maya Systems Ready")
    val voiceStatusMessage: StateFlow<String> = _voiceStatusMessage.asStateFlow()

    private val _availableFemaleVoices = MutableStateFlow<List<String>>(emptyList())
    val availableFemaleVoices: StateFlow<List<String>> = _availableFemaleVoices.asStateFlow()

    // Configurable voice parameters
    var pitch: Float = 1.15f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }

    var languageTag: String = "hi-IN"

    private var speakingWaveJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize ToneGenerator or TTS: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            setupHindiFemaleVoice()
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    startSpeakingWaveSimulation()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopSpeakingWaveSimulation()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopSpeakingWaveSimulation()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    stopSpeakingWaveSimulation()
                }
            })
            _voiceStatusMessage.value = "Maya Voice Core: Active (Hindi Female)"
        } else {
            _voiceStatusMessage.value = "Voice synthesizer initialization notice"
            Log.e(tag, "TTS init error: $status")
        }
    }

    private fun setupHindiFemaleVoice() {
        val tts = textToSpeech ?: return
        val hindiLocale = Locale.forLanguageTag("hi-IN")
        val result = tts.setLanguage(hindiLocale)

        val femaleVoiceNames = mutableListOf<String>()

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to Indian English or Default
            tts.setLanguage(Locale.forLanguageTag("en-IN"))
        }

        try {
            val allVoices = tts.voices
            if (allVoices != null) {
                // Find Hindi female voice
                var selectedVoice: Voice? = null
                for (voice in allVoices) {
                    val vName = voice.name.lowercase()
                    val isHindi = voice.locale.language == "hi" || vName.contains("hi-in") || vName.contains("hin")
                    val isFemale = vName.contains("female") || vName.contains("f0") ||
                            vName.contains("fem") || vName.contains("hie") || vName.contains("hia")

                    if (isHindi) {
                        femaleVoiceNames.add(voice.name)
                        if (isFemale && selectedVoice == null) {
                            selectedVoice = voice
                        }
                    }
                }

                // If specific female Hindi found, apply it
                if (selectedVoice != null) {
                    tts.voice = selectedVoice
                    _voiceStatusMessage.value = "Maya Voice: ${selectedVoice.name}"
                } else {
                    // Try setting first Hindi voice found
                    allVoices.firstOrNull { it.locale.language == "hi" }?.let {
                        tts.voice = it
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Voice query not supported on this engine: ${e.message}")
        }

        _availableFemaleVoices.value = femaleVoiceNames
        tts.setPitch(pitch)
        tts.setSpeechRate(speechRate)
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (!isTtsInitialized || textToSpeech == null) {
            _voiceStatusMessage.value = "Voice synthesizer warming up..."
            onFinished?.invoke()
            return
        }

        stopSpeaking()
        val utteranceId = "maya_utterance_${System.currentTimeMillis()}"

        // Clean out action brackets or markdown asterisks so speech sounds natural
        val cleanSpeech = text
            .replace(Regex("\\[ACTION:[^\\]]*\\]"), "")
            .replace(Regex("[*#_`~]"), "")
            .trim()

        if (cleanSpeech.isNotEmpty()) {
            textToSpeech?.speak(
                cleanSpeech,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
            )
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
        stopSpeakingWaveSimulation()
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
            return
        }

        stopSpeaking()
        playJarvisChime(ToneGenerator.TONE_PROP_BEEP)

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _voiceStatusMessage.value = "Maya Listening... (Hindi/English)"
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        _audioRmsLevel.value = (rmsdB.coerceIn(0f, 10f) / 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _audioRmsLevel.value = 0f
                        _voiceStatusMessage.value = "Processing command..."
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioRmsLevel.value = 0f
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_NETWORK -> "Network required for speech."
                            else -> "Mic recognition paused."
                        }
                        _voiceStatusMessage.value = msg
                        onError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioRmsLevel.value = 0f
                        playJarvisChime(ToneGenerator.TONE_PROP_ACK)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim() ?: ""
                        if (spokenText.isNotEmpty()) {
                            onResult(spokenText)
                        } else {
                            onError("Empty speech")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim()
                        if (!partial.isNullOrEmpty()) {
                            _voiceStatusMessage.value = "Heard: $partial"
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye Boss, Maya sun rahi hai...")
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognizer: ${e.message}")
            _isListening.value = false
            onError("Mic error: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            _audioRmsLevel.value = 0f
        } catch (e: Exception) {
            Log.e(tag, "Error stopping recognizer: ${e.message}")
        }
    }

    private fun startSpeakingWaveSimulation() {
        stopSpeakingWaveSimulation()
        speakingWaveJob = coroutineScope.launch(Dispatchers.Default) {
            while (isActive && _isSpeaking.value) {
                // Generate dynamic reactive wave levels
                val randomAmp = Random.nextFloat() * 0.7f + 0.3f
                _audioRmsLevel.value = randomAmp
                delay(80)
            }
            _audioRmsLevel.value = 0f
        }
    }

    private fun stopSpeakingWaveSimulation() {
        speakingWaveJob?.cancel()
        speakingWaveJob = null
        _audioRmsLevel.value = 0f
    }

    fun playJarvisChime(toneType: Int) {
        try {
            toneGenerator?.startTone(toneType, 120)
        } catch (e: Exception) {
            Log.e(tag, "Tone error: ${e.message}")
        }
    }

    fun cleanup() {
        stopSpeaking()
        stopListening()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
        toneGenerator?.release()
    }
}
