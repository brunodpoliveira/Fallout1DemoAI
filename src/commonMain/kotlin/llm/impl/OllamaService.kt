package llm.impl

import llm.*
import utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class OllamaService(private val config: LLMConfig) : LLMService {
    private val client = OkHttpClient()
    private val jsonMapper: ObjectMapper = jacksonObjectMapper()
    private val baseUrl = System.getProperty("ollama.host", "http://localhost:11434")

    init {
        config.validate()
    }

    override suspend fun chat(messages: List<LLMMessage>, maxTokens: Int): String = withContext(Dispatchers.IO) {
        val requestBody = jsonMapper.writeValueAsString(
            mapOf(
                "model" to config.chatModel,
                "messages" to messages.map {
                    mapOf("role" to it.role, "content" to it.content)
                },
                "stream" to false
            )
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                chain.proceed(request)
            }
            .build()

        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .header("Connection", "Keep-Alive")
            .build()

        try {
            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {
                    val error = response.body?.string() ?: "Unknown error"
                    throw Exception("Llama3 chat request failed: $error")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response")

                return@use jsonMapper.readTree(responseBody)
                    .path("message")
                    .path("content")
                    .asText()
            }
        } catch (e: java.net.SocketTimeoutException) {
            Logger.error("Timeout error during chat request: ${e.message}")
            throw Exception("Request timed out. Please check your server or increase the timeout settings.")
        } catch (e: Exception) {
            Logger.error("Error during chat request: ${e.message}. Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    override suspend fun embedding(text: String): List<Float> = withContext(Dispatchers.IO) {
        val requestBody = jsonMapper.writeValueAsString(mapOf(
            "model" to config.embeddingModel,
            "prompt" to text.replace("\n", " ")
        ))

        val request = Request.Builder()
            .url("$baseUrl/api/embeddings")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string() ?: "Unknown error"
                throw Exception("Llama3 embedding request failed: $error")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            jsonMapper.readTree(responseBody)
                .path("embedding")
                .map { it.floatValue() }
        }
    }

    override suspend fun moderation(text: String): Boolean = true
    fun pullModel(modelName: String) {
        val requestBody = jsonMapper.writeValueAsString(mapOf(
            "name" to modelName
        ))

        val request = Request.Builder()
            .url("$baseUrl/api/pull")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string() ?: "Unknown error"
                throw Exception("Failed to pull model: $error")
            }
            Logger.debug("Successfully pulled model: $modelName")
        }
    }
}
