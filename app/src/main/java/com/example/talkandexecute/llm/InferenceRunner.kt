package com.example.talkandexecute.llm

object InferenceRunner {

    private var inferenceCallback: InferenceCallback? = null

    fun setInferenceCallback(callback: InferenceCallback) {
        inferenceCallback = callback
    }

    external fun run(
        checkpoint: String,
        tokenizer: String,
        temperature: Float,
        steps: Int,
        topp: Float,
        prompt: String,
        ompthreads: Int
    )

    external fun stop()

    fun onNewToken(token: String) {
        inferenceCallback?.onNewResult(token)
    }

    interface InferenceCallback {
        fun onNewResult(token: String?)
    }
}
