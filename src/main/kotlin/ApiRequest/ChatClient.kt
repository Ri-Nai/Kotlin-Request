package ApiRequest

import ApiRequest.datasource.ChatConfig
import ApiRequest.model.Chunk
import ApiRequest.model.Message
import kotlinx.coroutines.flow.Flow
import okio.BufferedSource

interface ChatClient {
    val chatConfig: ChatConfig
    val json: kotlinx.serialization.json.Json
    suspend fun streamChat(messages: List<Message>): Flow<Result<Chunk>>
    fun processSource(
        source: BufferedSource,
        trySendBlocking: (Result<Chunk>) -> Unit,
        close: () -> Unit
    ) {
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val content = line.removePrefix("data: ")
                    if (content == "[DONE]") {
                        break
                    }
                    try {
                        val chunk = json.decodeFromString<Chunk>(content)
                        trySendBlocking(Result.success(chunk))
                    } catch (e: Exception) {
                        trySendBlocking(Result.failure(Exception("Failed to parse: $content", e)))
                    }
                }
            }
        } catch (e: Exception) {
            trySendBlocking(Result.failure(e))
            close()
        } finally {
            close()
        }
    }
}
