package ai

import com.theokanning.openai.client.*
import com.theokanning.openai.completion.chat.*
import com.theokanning.openai.service.*
import com.theokanning.openai.service.OpenAiService.buildApi
import kotlinx.coroutines.*
import java.time.*
import java.util.concurrent.*
import kotlin.math.*

object OpenAIService {
    private const val API_KEY = "YOUR_OPENAI_API_KEY"
    private val api: OpenAiApi
    private val service: OpenAiService
    val msgs: MutableList<ChatMessage> = ArrayList()
    private var hasInjectedInitialPrompt = false
    private const val MAX_RETRIES = 3
    private const val INITIAL_TIMEOUT = 30 // seconds
    private const val MAX_TIMEOUT = 120 // seconds

    init {
        api = buildApi(API_KEY, Duration.ofSeconds(INITIAL_TIMEOUT.toLong()))
        service = OpenAiService(api)
    }

    private fun handleTimeoutError(elapsedTime: Long): Boolean {
        return elapsedTime > INITIAL_TIMEOUT * 1000
    }

    suspend fun sendMessage(chatRequest: ChatCompletionRequest): ChatCompletionResult = withContext(Dispatchers.IO) {
        var currentTimeout = INITIAL_TIMEOUT
        repeat(MAX_RETRIES) { attempt ->
            try {
                val startTime = System.currentTimeMillis()
                val response = withTimeout(currentTimeout * 1000L) {
                    service.createChatCompletion(chatRequest)
                }
                val elapsedTime = System.currentTimeMillis() - startTime

                if (handleTimeoutError(elapsedTime)) {
                    println("Request timed out (Attempt ${attempt + 1}/$MAX_RETRIES)")
                    if (attempt == MAX_RETRIES - 1) {
                        throw TimeoutException("Request failed after $MAX_RETRIES attempts")
                    }
                    delay(calculateBackoff(attempt))
                    currentTimeout = (currentTimeout * 2).coerceAtMost(MAX_TIMEOUT)
                } else {
                    return@withContext response
                }
            } catch (e: Exception) {
                println("An error occurred (Attempt ${attempt + 1}/$MAX_RETRIES): ${e.message}")
                if (attempt == MAX_RETRIES - 1) {
                    throw e
                }
                delay(calculateBackoff(attempt))
                currentTimeout = (currentTimeout * 2).coerceAtMost(MAX_TIMEOUT)
            }
        }
        throw RuntimeException("Request failed after $MAX_RETRIES attempts")
    }

    private fun calculateBackoff(attempt: Int): Long {
        return (2.0.pow(attempt.toDouble()) * 1000).toLong().coerceAtMost(30000)
    }

    fun getCharacterResponse(npcName: String, factionName: String?, characterBio: String, playerInput: String): String {
        println("Player input: $playerInput")

        val factionContext = factionName?.let { Director.getFactionContext(it) } ?: ""
        val completeContext = """
            Bio: $characterBio
            General Context: ${Director.getContext()}
            Faction Context: $factionContext
            NPC Context: ${Director.getNPCContext(npcName)}
            DO NOT talk about non-existent characters, items, and locations
        """.trimIndent()

        if (!hasInjectedInitialPrompt) {
            // Initial context injection
            msgs.add(SystemMessage(completeContext))

            val initialChatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(msgs)
                .temperature(0.9)
                .maxTokens(512)
                .topP(1.0)
                .frequencyPenalty(0.8)
                .presencePenalty(0.8)
                .build()

            try {
                val initialResponse = service.createChatCompletion(initialChatCompletionRequest)
                val initialChoice = initialResponse.choices.firstOrNull()?.message as? AssistantMessage
                if (initialChoice != null) {
                    hasInjectedInitialPrompt = true
                    println("Initial response: ${initialChoice.content}")
                    msgs.add(initialChoice)
                    return initialChoice.content
                }
            } catch (e: Exception) {
                println("Error during initial prompt: ${e.message}")
                return "I'm having trouble responding at the moment."
            }
        }

        // Add the new user message
        msgs.add(UserMessage(playerInput))

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(msgs)
            .temperature(0.9)
            .maxTokens(1024)
            .topP(1.0)
            .frequencyPenalty(0.8)
            .presencePenalty(0.8)
            .build()

        try {
            val response = service.createChatCompletion(chatCompletionRequest)
            val choice = response.choices.firstOrNull()?.message as? AssistantMessage
            if (choice != null) {
                val npcResponse = choice.content
                println("npcResponse: $npcResponse")
                msgs.add(choice)
                return npcResponse
            }
        } catch (e: Exception) {
            println("Error during response generation: ${e.message}")
            return "I'm having trouble responding at the moment."
        }

        return "I don't have a response at the moment."
    }

    fun resetConversation() {
        msgs.clear()
        hasInjectedInitialPrompt = false
    }
}
