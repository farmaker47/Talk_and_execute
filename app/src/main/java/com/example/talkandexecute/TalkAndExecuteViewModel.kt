package com.example.talkandexecute

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.talkandexecute.model.SpeechState
import com.google.ai.generativelanguage.v1beta2.GenerateTextRequest
import com.google.ai.generativelanguage.v1beta2.TextPrompt
import com.google.ai.generativelanguage.v1beta2.TextServiceClient
import com.google.ai.generativelanguage.v1beta2.TextServiceSettings
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider
import com.google.api.gax.rpc.FixedHeaderProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale


class TalkAndExecuteViewModel(application: Application) : AndroidViewModel(application) {

    // Directions to set up the PALM API
    // https://developers.generativeai.google/tutorials/text_android_quickstart
    private var client: TextServiceClient

    private fun initializeTextServiceClient(
        apiKey: String
    ): TextServiceClient {
        // (This is a workaround because GAPIC java libraries don't yet support API key auth)
        val transportChannelProvider = InstantiatingGrpcChannelProvider.newBuilder()
            .setHeaderProvider(FixedHeaderProvider.create(hashMapOf("x-goog-api-key" to /* System.getenv("PALM_API_KEY") */ apiKey)))
            .build()

        // Create TextServiceSettings
        val settings = TextServiceSettings.newBuilder()
            .setTransportChannelProvider(transportChannelProvider)
            .setCredentialsProvider(FixedCredentialsProvider.create(null))
            .build()

        // Initialize and return a TextServiceClient
        return TextServiceClient.create(settings)
    }

    private fun createPrompt(
        textContent: String
    ): TextPrompt {
        return TextPrompt.newBuilder()
            .setText(textContent)
            .build()
    }

    private fun generateText(
        request: GenerateTextRequest
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = client.generateText(request)
                val returnedText = response.candidatesList.last()
                speechState = speechState.copy(palmResult = returnedText.output)
            } catch (e: Exception) {
                // There was an error
                speechState = speechState.copy(palmResult = "API Error: ${e.message}")
            }
        }
    }

    private fun createTextRequest(prompt: TextPrompt): GenerateTextRequest {
        return GenerateTextRequest.newBuilder()
            .setModel("models/text-bison-001") // Required, which model to use to generate the result
            .setPrompt(prompt) // Required
            .setTemperature(0.5f) // Optional, controls the randomness of the output
            .setCandidateCount(1) // Optional, the number of generated texts to return
            .build()
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    var speechState by mutableStateOf(SpeechState())

    fun startListening() {
        speechRecognizer?.startListening(recognitionIntent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    init {
        // For PALM API.
        client = initializeTextServiceClient(
            apiKey = "1234567"
        )
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
        recognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")

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

                    //////////////////////////////////////
                    val completeString = "If I say $result you have to answer to me what I mean with one of the below options and no other words, " +
                            "just pick from the below and write only that:\n" +
                            "volume up\n" +
                            "volume down\n" +
                            "unidentified"
                    // For PALM API.
                    // Create the text prompt
                    val prompt = createPrompt(completeString)
                    // Send the first request
                    val request = createTextRequest(prompt)
                    generateText(request)
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