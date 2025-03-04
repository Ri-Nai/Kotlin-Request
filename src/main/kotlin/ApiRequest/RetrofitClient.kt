package ApiRequest

import ApiRequest.Api.ChatApi
import ApiRequest.datasource.ChatConfig
import ApiRequest.model.Chunk
import ApiRequest.model.Message
import ApiRequest.model.Payload
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okio.IOException

class RetrofitClient(
    private val api: ChatApi,
    override val chatConfig: ChatConfig,
    override val json: Json = Json { ignoreUnknownKeys = true },
) : ChatClient {
    override suspend fun streamChat(messages: List<Message>): Flow<Result<Chunk>> = callbackFlow {
        val response = api.streamChat(
            payload = Payload(
                model = chatConfig.defaultModel,
                messages = messages,
                temperature = chatConfig.defaultTemperature,
                stream = true
            )
        )
        if (!response.isSuccessful) {
            close(IOException("Error streaming chat!"))
            return@callbackFlow
        }

        response.body()?.use { body ->
            body.source().use { source ->
                processSource(source, ::trySendBlocking) { close() }
            }
        } ?: run {
            trySendBlocking(Result.failure(IOException("Empty response body")))
            close()
        }
        close()
    }
}