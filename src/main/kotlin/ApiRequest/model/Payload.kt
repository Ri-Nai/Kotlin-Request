package ApiRequest.model

import kotlinx.serialization.Serializable

@Serializable
data class Payload(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    val stream: Boolean = false
)
