package com.example.talkandexecute

import android.app.Application
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.talkandexecute.model.SpeechState
import java.io.File
import java.io.IOException

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
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
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
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mediaRecorder = null
            }
        }
    }

    companion object {

    }
}