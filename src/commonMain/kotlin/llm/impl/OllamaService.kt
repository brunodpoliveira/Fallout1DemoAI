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
        val requestBody = jsonMapper.writeValueAsString(mapOf(
            "model" to config.chatModel,
            "messages" to messages.map {
                mapOf("role" to it.role, "content" to it.content)
            }
        ))

        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string() ?: "Unknown error"
                if (response.code == 404 && error.contains("try pulling")) {
                    throw ModelNotAvailableException("Model needs to be pulled: $error")
                }
                throw Exception("Ollama request failed: $error")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            jsonMapper.readTree(responseBody)
                .path("response")
                .asText()
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
            if (!response.isSuccessful) throw Exception("Ollama request failed: ${response.code}")

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            jsonMapper.readTree(responseBody)
                .path("embedding")
                .map { it.floatValue() }
        }
    }

    override suspend fun moderation(text: String): Boolean = true // Ollama doesn't have moderation

    fun pullModel(modelName: String) {
        val requestBody = jsonMapper.writeValueAsString(mapOf(
            "name" to modelName
        ))

        val request = Request.Builder()
            .url("$baseUrl/api/pull")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to pull model: ${response.code}")
            Logger.debug("Successfully pulled model: $modelName")
        }
    }
}
