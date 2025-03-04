package ApiRequest.Api

import ApiRequest.datasource.ChatConfig
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitFactory {
    private val json = Json { ignoreUnknownKeys = true }
    fun createChatApi(
        chatConfig: ChatConfig,
    ): ChatApi {
        val chatClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS) // 禁用读超时
            .addInterceptor {
                val request = it.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${chatConfig.apiKey}")
                    .build()
                it.proceed(request)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(chatConfig.apiUrl)
            .client(chatClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(ChatApi::class.java)
    }
}