package ApiRequest.Api

import ApiRequest.model.Payload
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ChatApi {
    @Streaming
    @POST("chat/completions")
    suspend fun streamChat(
        @Body payload: Payload
    ) : Response<ResponseBody>
}