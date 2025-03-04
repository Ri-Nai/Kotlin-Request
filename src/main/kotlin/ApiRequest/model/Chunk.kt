package ApiRequest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Chunk(
    val model: String,
    @SerialName("object") val objectType: String,
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(val index: Int, val delta: Delta, val finish_reason: String?) {
        @Serializable
        data class Delta(val content: String? = null)
    }
}