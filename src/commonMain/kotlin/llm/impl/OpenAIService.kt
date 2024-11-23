package llm.impl

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import kotlinx.coroutines.*
import llm.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import utils.*

class OpenAIService(private val config: LLMConfig) : LLMService {
    private val client = OkHttpClient()
    private val jsonMapper: ObjectMapper = jacksonObjectMapper()
    private val baseUrl = "https://api.openai.com/v1"

    init {
        config.validate()
    }

    override suspend fun chat(messages: List<LLMMessage>, maxTokens: Int): String = withContext(Dispatchers.IO) {
        val requestBody = jsonMapper.writeValueAsString(mapOf(
            "model" to config.chatModel,
            "messages" to messages.map {
                mapOf("role" to it.role, "content" to it.content)
            },
            "max_tokens" to maxTokens
        ))

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey()}")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("OpenAI request failed: ${response.code}")

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            jsonMapper.readTree(responseBody)
                .path("choices")
                .first()
                .path("message")
                .path("content")
                .asText()
        }
    }

    override suspend fun embedding(text: String): List<Float> = withContext(Dispatchers.IO) {
        val requestBody = jsonMapper.writeValueAsString(mapOf(
            "model" to config.embeddingModel,
            "input" to text.replace("\n", " ")
        ))

        val request = Request.Builder()
            .url("$baseUrl/embeddings")
            .addHeader("Authorization", "Bearer ${config.apiKey()}")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("OpenAI request failed: ${response.code}")

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            jsonMapper.readTree(responseBody)
                .path("data")
                .first()
                .path("embedding")
                .map { it.floatValue() }
        }
    }

    override suspend fun moderation(text: String): Boolean = withContext(Dispatchers.IO) {
        val requestBody = jsonMapper.writeValueAsString(mapOf(
            "input" to text
        ))

        val request = Request.Builder()
            .url("$baseUrl/moderations")
            .addHeader("Authorization", "Bearer ${config.apiKey()}")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Logger.warn("Moderation request failed: ${response.code}")
                return@withContext true // Default to safe if request fails
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            !jsonMapper.readTree(responseBody)
                .path("results")
                .first()
                .path("flagged")
                .asBoolean()
        }
    }
}
