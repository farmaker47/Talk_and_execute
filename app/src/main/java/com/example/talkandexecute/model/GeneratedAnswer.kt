package com.example.talkandexecute.model

import com.google.gson.annotations.SerializedName

data class GeneratedAnswer(
    @SerializedName("choices")
    val choices: List<Choice>?,

    @SerializedName("created")
    val created: Int?,

    @SerializedName("id")
    val id: String?,

    @SerializedName("model")
    val model: String?,

    @SerializedName("object")
    val `object`: String?,

    @SerializedName("usage")
    val usage: Usage?
)

data class Choice(

    @SerializedName("finish_reason")
    val finish_reason: String,

    @SerializedName("index")
    val index: Int,

    @SerializedName("logprobs")
    val logprobs: Any,

    @SerializedName("text")
    val text: String,

    @SerializedName("message"       )
    var message: Message? = Message(),

    )

data class Message (

    @SerializedName("role"    ) var role    : String? = null,
    @SerializedName("content" ) var content : String? = null

)

data class Usage(

    @SerializedName("completion_tokens")
    val completion_tokens: Int,

    @SerializedName("prompt_tokens")
    val prompt_tokens: Int,

    @SerializedName("total_tokens")
    val total_tokens: Int

)

data class AudioToText (
    @SerializedName("text" ) var text : String = ""
)
