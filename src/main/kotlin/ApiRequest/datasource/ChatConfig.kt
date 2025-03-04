package ApiRequest.datasource

data class ChatConfig(
    val apiUrl: String,
    val apiKey: String,
    val endpointPath: String = "chat/completions",
    val defaultModel: String = "deepseek-r1",
    val defaultTemperature: Double = 0.7
)
