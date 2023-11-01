package com.example.talkandexecute.classification

import android.content.Context
import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import com.example.talkandexecute.AudioClassificationListener
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.core.BaseOptions
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AudioClassificationHelper(
    private val context: Context,
    private val listener: AudioClassificationListener,
    private var currentModel: String = SPEECH_COMMAND_MODEL,
    private var classificationThreshold: Float = THRESHOLD,
    private var overlap: Float = DEFAULT_OVERLAP_VALUE,
    private var numOfResults: Int = DEFAULT_NUM_OF_RESULTS,
    private var currentDelegate: Int = 0,
    private var numThreads: Int = 2
) {
    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var recorder: AudioRecord
    private lateinit var executor: ScheduledThreadPoolExecutor

    private val classifyRunnable = Runnable {
        classifyAudio()
    }

    fun initClassifier() {
        // Set general detection options, e.g. number of used threads
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(numThreads)

        // Use the specified hardware for running the model. Default to CPU.
        // Possible to also use a GPU delegate, but this requires that the classifier be created
        // on the same thread that is using the classifier, which is outside of the scope of this
        // sample's design.
        /*when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }*/

        // Configures a set of parameters for the classifier and what results will be returned.
        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(classificationThreshold)
            .setMaxResults(numOfResults)
            .setBaseOptions(baseOptionsBuilder.build())
            .build()

        try {
            // Create the classifier and required supporting objects
            classifier = AudioClassifier.createFromFileAndOptions(context, currentModel, options)
            tensorAudio = classifier.createInputTensorAudio()
            recorder = classifier.createAudioRecord()
            startAudioClassification()
        } catch (e: IllegalStateException) {
            listener.onError(
                "Audio Classifier failed to initialize. See error logs for details"
            )

            Log.e("AudioClassification", "TFLite failed to load with error: " + e.message)
        }
    }

    private fun startAudioClassification() {
        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        recorder.startRecording()
        executor = ScheduledThreadPoolExecutor(1)

        // Each model will expect a specific audio recording length. This formula calculates that
        // length using the input buffer size and tensor format sample rate.
        // For example, YAMNET expects 0.975 second length recordings.
        // This needs to be in milliseconds to avoid the required Long value dropping decimals.
        val lengthInMilliSeconds = ((classifier.requiredInputBufferSize * 1.0f) /
                classifier.requiredTensorAudioFormat.sampleRate) * 1000
        Log.v("classifier_length", lengthInMilliSeconds.toString())
        Log.v("classifier_buffer_size", classifier.requiredInputBufferSize.toString())
        Log.v("classifier_sample_rate", classifier.requiredTensorAudioFormat.sampleRate.toString())

        val interval = (lengthInMilliSeconds * (1 - overlap)).toLong()
        Log.v("classifier_interval", interval.toString())
        Log.v("classifier_overlap", overlap.toString())

        executor.scheduleAtFixedRate(
            classifyRunnable,
            0,
            interval,
            TimeUnit.MILLISECONDS)
    }

    private fun classifyAudio() {
        tensorAudio.load(recorder)
        var inferenceTime = SystemClock.uptimeMillis()
        val output = classifier.classify(tensorAudio)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        listener.onResult(output[0].categories, inferenceTime)
    }

    fun stopAudioClassification() {
        recorder.stop()
        executor.shutdownNow()
    }

    companion object {
        const val THRESHOLD = 0.9f
        const val DEFAULT_NUM_OF_RESULTS = 1
        const val DEFAULT_OVERLAP_VALUE = 0.75f
        const val SPEECH_COMMAND_MODEL = "speech.tflite"
    }
}

