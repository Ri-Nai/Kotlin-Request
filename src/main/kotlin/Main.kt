import ApiRequest.Api.RetrofitFactory
import ApiRequest.ChatClient
import ApiRequest.Okhttp3Client
import ApiRequest.RetrofitClient
import ApiRequest.datasource.ChatConfig
import ApiRequest.model.Message
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

fun main() = runBlocking {
    // 从环境变量读取配置（替代安卓的local.properties）
    val config = Properties().apply {
        load(File("local.properties").inputStream())
    }.run {
        ChatConfig(
            apiUrl = this.getProperty("api.url") ?: error("API_URL required"),
            apiKey = this.getProperty("api.key") ?: error("API_KEY required"),
            defaultModel = this.getProperty("api.model") ?: "deepseek-r1",
        )
    }
    val client : ChatClient = run {
        println("请选择客户端：1. Retrofit 2. Okhttp3")
//        Okhttp3Client(config)
        when (readLine()) {
            "1" -> RetrofitClient(RetrofitFactory.createChatApi(config), config)
            else -> Okhttp3Client(config)
        }
    }
    val chatHistory = mutableListOf<Message>()

    while (true) {
        print("\n用户：")
        val input = readLine()?.takeIf { it.isNotBlank() } ?: break

        chatHistory.add(Message("user", input))

        print("AI：")
        val response = StringBuilder()


        client.streamChat(chatHistory.toList()).collect { result ->
            result.onSuccess { chunk ->
                chunk.choices.firstOrNull()?.delta?.content?.let {
                    print(it)
                    response.append(it)
                }
            }.onFailure { e ->
                println("\n[ERROR] ${e.localizedMessage}")
            }
        }

        chatHistory.add(Message("assistant", response.toString()))
    }
}


suspend fun runRetrofitClient(config: ChatConfig) {
    val api = RetrofitFactory.createChatApi(config)
    val client = RetrofitClient(api, config)
    val chatHistory = mutableListOf<Message>()

    while (true) {
        print("\n用户：")
        val input = readLine()?.takeIf { it.isNotBlank() } ?: break

        chatHistory.add(Message("user", input))

        print("AI：")
        val response = StringBuilder()

        client.streamChat(chatHistory.toList()).collect { result ->
            result.onSuccess { chunk ->
                chunk.choices.firstOrNull()?.delta?.content?.let {
                    print(it)
                    response.append(it)
                }
            }.onFailure { e ->
                println("\n[ERROR] ${e.localizedMessage}")
            }
        }

        chatHistory.add(Message("assistant", response.toString()))
    }
}