package com.example.talkandexecute

import android.app.Application
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.talkandexecute.model.GeneratedAnswer
import com.example.talkandexecute.model.SpeechState
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatGPTViewModel(application: Application) : AndroidViewModel(application) {

    var speechState by mutableStateOf(SpeechState())

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording: Boolean = false
    val outputFile = File(application.filesDir, "recording.mp3")

    fun startListening() {
        if (!isRecording) {
            try {
                mediaRecorder = MediaRecorder().apply {
                    // Initialization.
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(outputFile.absolutePath)

                    // Prepare and start recording.
                    prepare()
                    start()
                    isRecording = true
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopListening() {
        if (isRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                isRecording = false

                viewModelScope.launch(Dispatchers.Default) {
                    val transcribedText = transcribeAudio(outputFile)
                    speechState = try {
                        speechState.copy(speechResult = transcribedText)
                    } catch (e: Exception) {
                        // There was an error
                        speechState.copy(speechResult = "API Error: ${e.message}")
                    }
                    speechState = try {
                        val returnedText =  createChatCompletion(transcribedText)
                        speechState.copy(palmResult = returnedText)
                    } catch (e: Exception) {
                        // There was an error
                        speechState.copy(palmResult = "API Error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mediaRecorder = null
            }
        }
    }

    private var loggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
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
            .header("Authorization", "Bearer $API_KEY")
            .post(formBody)

        return client.newCall(request.build()).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    private fun createChatCompletion(prompt: String): String {

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val completeString = "I will say $prompt. What can you do to help me?\n" +
                "Pick one from the below options and write only the number:\n" +
                "1 volume up\n" +
                "2 volume down\n" +
                "3 unidentified"

        val messagesArray = JSONArray()
        messagesArray.put(JSONObject().put("role", "system").put("content", "You are a helpful assistant inside a car."))
        // messagesArray.put(JSONObject().put("role", "user").put("content", "Who won the world series in 2020?"))
        // messagesArray.put(JSONObject().put("role", "assistant").put("content", "The Los Angeles Dodgers won the World Series in 2020."))
        messagesArray.put(JSONObject().put("role", "user").put("content", completeString))

        val json = JSONObject()
            .put("model", "gpt-3.5-turbo")
            .put("messages", messagesArray)

        val requestBody = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $API_KEY")
            .post(requestBody)
            .build()

        val gson = Gson()

        client.newCall(request).execute().use { response ->
            val chatCompletionResponse = gson.fromJson(response.body?.string() ?: "", GeneratedAnswer::class.java)
            return chatCompletionResponse.choices?.get(0)?.message?.content.toString()
        }
    }

    companion object {

    }
}