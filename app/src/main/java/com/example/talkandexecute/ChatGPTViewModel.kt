package com.example.talkandexecute

import android.app.Application
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.talkandexecute.model.SpeechState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
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
                    delay(2000)
                    transcribeAudio(outputFile)
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

    fun transcribeAudio(audioFile: File): String {
        val audioRequestBody = audioFile.asRequestBody("audio/*".toMediaType())

        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, audioRequestBody)
            .addFormDataPart("model", "whisper-1")
            .addFormDataPart("language", "en")
            .build()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/transcriptions")
            .header("Authorization", "Bearer API_KEY")
            .post(formBody)

        return client.newCall(request.build()).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    companion object {

    }
}