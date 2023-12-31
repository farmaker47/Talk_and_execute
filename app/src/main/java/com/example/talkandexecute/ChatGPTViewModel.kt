package com.example.talkandexecute

import android.app.Application
import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.talkandexecute.classification.AudioClassificationHelper
import com.example.talkandexecute.model.AudioToText
import com.example.talkandexecute.model.GeneratedAnswer
import com.example.talkandexecute.model.SpeechState
import com.example.talkandexecute.recorder.Recorder
import com.example.talkandexecute.whisperengine.IWhisperEngine
import com.example.talkandexecute.whisperengine.WhisperEngine
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.support.label.Category
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatGPTViewModel(application: Application) : AndroidViewModel(application) {

    var speechState by mutableStateOf(SpeechState())

    private var mediaRecorder: MediaRecorder = MediaRecorder()
    private var isRecording: Boolean = false
    private var numberOfBackgroundLabel = 0
    private val outputFile = File(application.filesDir, RECORDING_FILE)
    private val outputFileWav = File(application.filesDir, RECORDING_FILE_WAV)
    private val audioClassificationListener = object : AudioClassificationListener {
        override fun onResult(results: List<Category>, inferenceTime: Long) {
            Log.v("speech_result", "$results $inferenceTime")
            if (results.isNotEmpty()) {
                if (results[0].index == 7) {
                    numberOfBackgroundLabel = 0
                    // startListening()
                } else if (results[0].index == 0) {
                    numberOfBackgroundLabel++
                }
            } else {
                numberOfBackgroundLabel++
            }

            if (isRecording) {
                // Log.v("speech_number", "$numberOfBackgroundLabel")
                if (numberOfBackgroundLabel > 10) {
                    numberOfBackgroundLabel = 0
                    // stopListening()
                }
            }
        }

        override fun onError(error: String) {
            Log.v("speech_result", error)
        }
    }
    private val whisperEngine: IWhisperEngine = WhisperEngine(application)
    private val recorder: Recorder = Recorder(application)
    private val audioClassificationHelper =
        AudioClassificationHelper(context = application, listener = audioClassificationListener)
    val generativeModel = GenerativeModel(
        // For text-only input, use the gemini-pro model
        modelName = "gemini-pro",
        // Access your API key as a Build Configuration variable (see "Set up your API key" above)
        apiKey = GEMINI_API_KEY
    )
    private var transcribedText = ""

    init {
        audioClassificationHelper.initClassifier()
        whisperEngine.initialize(MODEL_PATH, getFilePath(VOCAB_PATH, application), false)
        recorder.setFilePath(getFilePath(RECORDING_FILE_WAV, application))
    }

    fun startRecordingWav() {
        recorder.start()
    }

    fun stopRecordingWav() {
        recorder.stop()

        try {
            viewModelScope.launch(Dispatchers.Default) {
                // Offline speech to text
                transcribedText = whisperEngine.transcribeFile(outputFileWav.absolutePath)
                speechState = try {
                    speechState.copy(speechResult = transcribedText)
                } catch (e: IOException) {
                    // There was an error
                    speechState.copy(speechResult = "API Error: ${e.message}")
                }
                resetRecording()
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, e.toString())
            // Handle the exception -> state machine is not in a valid state
            resetRecording()
        } catch (e: IllegalStateException) {
            Log.e(TAG, e.toString())
            resetRecording()
        }
    }

    fun pickAPI(geminiAPI: Boolean) {
        try {
            viewModelScope.launch(Dispatchers.Default) {
                speechState = if (!geminiAPI) {
                    val returnedText = createChatCompletion(transcribedText)
                    speechState.copy(geminiResult = returnedText)
                } else {
                    val completeString =
                        "I say $transcribedText. As an assistant how can you help me?\n" +
                                "Pick one from the options below if it is related to volume and write only the two words:\n" +
                                "volume up\n" +
                                "volume down\n" +
                                "if it is not related to volume answer the below two words: \n" +
                                "volume stable"
                    Log.v("viewmodel", completeString)
                    val returnedText = generativeModel.generateContent(completeString)
                    speechState.copy(geminiResult = returnedText.text!!)
                }
            }
        } catch (e: IOException) {
            // There was an error
            resetRecording()
            speechState = speechState.copy(geminiResult = "API Error: ${e.message}")
        }
    }

    fun startListening() {
        // Log.v("speech_start", "start")
        if (!isRecording) {
            isRecording = true
            numberOfBackgroundLabel = 0
            // Log.v("speech_start", "true")
            try {
                mediaRecorder.apply {
                    // Initialization.
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    // path to recording, if you want to hear it.
                    setOutputFile(outputFile.absolutePath)
                }
                mediaRecorder.prepare()
                mediaRecorder.start()
            } catch (e: IllegalStateException) {
                Log.e(TAG, e.toString())
                // Handle the exception -> MediaRecorder is not in the initialized state
                resetRecording()
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
                // Handle the exception -> failed to prepare MediaRecorder
                resetRecording()
            }
        }
    }

    fun stopListening() {
        // Log.v("speech_stop", "stop")
        if (isRecording) {
            // Log.v("speech_stop", "true")
            try {
                mediaRecorder.stop()
                mediaRecorder.reset()
                isRecording = false

                viewModelScope.launch(Dispatchers.Default) {
                    val transcribedText = transcribeAudio(outputFile)
                    speechState = try {
                        speechState.copy(speechResult = transcribedText)
                    } catch (e: IOException) {
                        // There was an error
                        speechState.copy(speechResult = "API Error: ${e.message}")
                    }
                    speechState = try {
                        val returnedText = createChatCompletion(transcribedText)
                        resetRecording()
                        speechState.copy(geminiResult = returnedText)
                    } catch (e: IOException) {
                        // There was an error
                        resetRecording()
                        speechState.copy(geminiResult = "API Error: ${e.message}")
                    }
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, e.toString())
                // Handle the exception -> state machine is not in a valid state
                resetRecording()
            } catch (e: IllegalStateException) {
                Log.e(TAG, e.toString())
                resetRecording()
            }
        }
    }

    private fun resetRecording() {
        mediaRecorder.reset()
        isRecording = false
        numberOfBackgroundLabel = 0
    }

    private var loggingInterceptor =
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS) // Set connection timeout to 30 seconds
        .readTimeout(30, TimeUnit.SECONDS)    // Set read timeout to 30 seconds
        .build()

    private fun transcribeAudio(audioFile: File): String {
        val audioRequestBody = audioFile.asRequestBody("audio/*".toMediaType())

        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, audioRequestBody)
            .addFormDataPart("model", "whisper-1")
            .addFormDataPart("language", "en")
            .build()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/transcriptions")
            .header("Authorization", "Bearer $CHAT_GPT_API_KEY")
            .post(formBody)

        val gson = Gson()
        return client.newCall(request.build()).execute().use { response ->
            val audioCompletionResponse =
                gson.fromJson(response.body?.string() ?: "", AudioToText::class.java)
            audioCompletionResponse.text
        }
    }

    private fun createChatCompletion(prompt: String): String {

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val completeString = "I say $prompt. As an assistant how can you help me?\n" +
                "Pick one from the options below if it is related to volume and write only the two words:\n" +
                "volume up\n" +
                "volume down\n" +
                "if it is not related to volume answer the below two words: \n" +
                "volume stable"

        val messagesArray = JSONArray()
        messagesArray.put(
            JSONObject().put("role", "system")
                .put("content", "You are a helpful assistant inside a car.")
        )
        // messagesArray.put(JSONObject().put("role", "user").put("content", "Who won the world series in 2020?"))
        // messagesArray.put(JSONObject().put("role", "assistant").put("content", "The Los Angeles Dodgers won the World Series in 2020."))
        messagesArray.put(JSONObject().put("role", "user").put("content", completeString))

        val json = JSONObject()
            .put("model", "gpt-3.5-turbo") // new gpt-3.5-turbo-1106
            .put("messages", messagesArray)

        val requestBody = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $CHAT_GPT_API_KEY")
            .post(requestBody)
            .build()

        val gson = Gson()

        client.newCall(request).execute().use { response ->
            val chatCompletionResponse =
                gson.fromJson(response.body?.string() ?: "", GeneratedAnswer::class.java)
            return chatCompletionResponse.choices?.get(0)?.message?.content.toString()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder.release()
        audioClassificationHelper.stopAudioClassification()
    }

    companion object {

        private const val MODEL_PATH = "whisper_tiny_english_14.tflite"
        private const val VOCAB_PATH = "filters_vocab_en.bin"
        private const val RECORDING_FILE = "recording.mp3"
        private const val RECORDING_FILE_WAV = "recording.wav"
    }

    // Returns file path for vocab .bin file
    private fun getFilePath(assetName: String, context: Context): String? {
        val outfile = File(context.filesDir, assetName)
        if (!outfile.exists()) {
            Log.d(TAG, "File not found - " + outfile.absolutePath)
        }
        Log.d(TAG, "Returned asset path: " + outfile.absolutePath)
        return outfile.absolutePath
    }
}

interface AudioClassificationListener {
    fun onError(error: String)
    fun onResult(results: List<Category>, inferenceTime: Long)
}
