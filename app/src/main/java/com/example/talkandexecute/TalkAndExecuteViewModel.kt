package com.example.talkandexecute

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.talkandexecute.model.SpeechState
import java.util.Locale


class TalkAndExecuteViewModel(application: Application) : AndroidViewModel(application) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    var speechState by mutableStateOf(SpeechState())
        private set

    fun startListening() {
        speechRecognizer?.startListening(recognitionIntent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    init {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
        recognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Called when the recognizer is ready to listen
                speechState = speechState.copy(speechResult = "Listening...")
            }

            override fun onBeginningOfSpeech() {
                // Called when the user starts speaking
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Called when the RMS (Root Mean Square) changes
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Called when audio data is received
            }

            override fun onEndOfSpeech() {
                // Called when the user stops speaking
            }

            override fun onError(error: Int) {
                // Called when an error occurs during recognition
            }

            override fun onResults(results: Bundle?) {
                // Called when recognition results are available
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val result = matches[0]
                    speechState = speechState.copy(speechResult = result)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Called when partial recognition results are available
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Called for various events during recognition
            }
        })
    }

    companion object {

    }
}