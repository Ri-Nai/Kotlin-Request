package ApiRequest

import okhttp3.OkHttpClient
import ApiRequest.datasource.ChatConfig
import ApiRequest.model.Payload
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import ApiRequest.model.Message
import ApiRequest.model.Chunk
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Response
import okhttp3.Call
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking

import java.io.IOException

class Okhttp3Client(
    override val chatConfig: ChatConfig,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    override val json: Json = Json { ignoreUnknownKeys = true },
) : ChatClient {
    // Define the missing media type constant
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()


    private fun createRequest(payload: Payload): Request {
        val jsonBody = Json.encodeToString(payload)
        return Request.Builder()
            .url(chatConfig.apiUrl + chatConfig.endpointPath)
            .addHeader("Authorization", "Bearer ${chatConfig.apiKey}")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    // 增强错误处理
    override suspend fun streamChat(messages: List<Message>): Flow<Result<Chunk>> = callbackFlow {
        val payload = Payload(
            model = chatConfig.defaultModel,
            messages = messages,
            temperature = chatConfig.defaultTemperature,
            stream = true
        )

        val call = okHttpClient.newCall(createRequest(payload))
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body?.use { body ->
                    body.source().use { source ->
                        processSource(source, ::trySendBlocking) { close() }
                    }
                } ?: run {
                    trySendBlocking(Result.failure(IOException("Empty response body")))
                    close()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                trySendBlocking(Result.failure(e))
                close()
            }
        })

        awaitClose { call.cancel() }
    }
}