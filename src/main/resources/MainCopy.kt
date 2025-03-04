import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.runBlocking
import java.util.*
import java.io.IOException
import java.io.File

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class Payload(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    val stream: Boolean = false
)

@Serializable
data class Chunk(
    val model: String,
    @SerialName("object")
    val objectType: String,
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(val index: Int, val delta: Delta, val finish_reason: String?) {
        @Serializable
        data class Delta(val content: String? = null) // 改为可空类型并设置默认值
    }

}

class DeepSeekClient(
    private val apiUrl: String,
    private val apiKey: String
) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json".toMediaType()
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // 基础请求构造

    private fun buildRequest(payload: Payload): Request {
        val jsonBody = jsonParser.encodeToString(payload)

        return Request.Builder()
            .url(apiUrl)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()
    }

    // 同步请求方法
    fun streamRequest(payload: Payload): Flow<String> = callbackFlow {
        val request = buildRequest(payload.copy(stream = true))
        val call = client.newCall(request) // 保存call引用

        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    response.body?.source()?.use { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.startsWith("data: ")) {
                                val jsonData = line.substring(6).trim()
                                when {
                                    jsonData == "[DONE]" -> close()
                                    else -> trySend(jsonData).onFailure { close(it) }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    close(e)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }
        })

        awaitClose { call.cancel() } // 确保取消请求
    }

    // 示例JSON解析方法（需根据实际响应结构调整）
    private fun parseContentFromJson(json: String): String {
        // 使用你喜欢的JSON解析库（例如kotlinx.serialization）
        // 这里简单返回原始数据用于演示
        return json
    }
}

// 使用示例
fun main() = runBlocking {
    val config = Properties().apply {
        load(File("local.properties").inputStream())
    }
    val client = DeepSeekClient(
        config.getProperty("api.url"),
        config.getProperty("api.key")
    )
    val messages = mutableListOf<Message>()
    val scanner = Scanner(System.`in`)

    while (true) {
        print("\n用户：")
        val userInput = scanner.nextLine().takeIf { it.isNotBlank() } ?: break

        // 添加用户消息到历史
        messages.add(Message(role = "user", content = userInput))

        // 构建请求
        val payload = Payload(
            model = "deepseek-r1",
            messages = messages.toList(),
            temperature = 0.7,
            stream = true
        )

        print("助理：")
        val responseContent = StringBuilder()

        client.streamRequest(
            Payload(
                model = "deepseek-r1",
                messages = messages,
                temperature = 0.7,
                stream = true
            )
        ).collect { chunk ->
            val parsed = Json.decodeFromString<Chunk>(chunk)
            parsed.choices[0].delta.content?.let {
                print(it)
                responseContent.append(it)
            }
        }
        messages.add(Message("assistant", responseContent.toString()))

    }
    println("对话结束")
}