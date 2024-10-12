package ai

import com.theokanning.openai.client.*
import com.theokanning.openai.completion.chat.*
import com.theokanning.openai.service.*
import com.theokanning.openai.service.OpenAiService.buildApi
import kotlinx.coroutines.*
import utils.*
import java.time.*

object OpenAIService {
    private const val API_KEY = "YOUR_OPENAI_API_KEY"
    private val api: OpenAiApi
    private val service: OpenAiService
    val msgs: MutableList<ChatMessage> = ArrayList()
    private var hasInjectedInitialPrompt = false
    private const val MAX_RETRIES = 3
    private const val INITIAL_TIMEOUT = 30 // seconds
    private const val MAX_TIMEOUT = 120 // seconds

    private val retryPolicy = RetryPolicy(
        maxAttempts = MAX_RETRIES,
        initialTimeout = INITIAL_TIMEOUT,
        maxTimeout = MAX_TIMEOUT
    )

    init {
        api = buildApi(API_KEY, Duration.ofSeconds(INITIAL_TIMEOUT.toLong()))
        service = OpenAiService(api)
    }

    fun createChatCompletionRequest(messages: List<ChatMessage>, maxTokens: Int): ChatCompletionRequest {
        return ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(messages)
            .temperature(0.9)
            .maxTokens(maxTokens)
            .topP(1.0)
            .frequencyPenalty(0.8)
            .presencePenalty(0.8)
            .build()
    }

    suspend fun sendMessage(chatRequest: ChatCompletionRequest): ChatCompletionResult =
        retryPolicy.execute { service.createChatCompletion(chatRequest) }

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

        return runBlocking {
            try {
                if (!hasInjectedInitialPrompt) {
                    injectInitialPrompt(completeContext)
                }
                msgs.add(UserMessage(playerInput))
                generateResponse()
            } catch (e: Exception) {
                println("Error in getCharacterResponse: ${e.message}")
                "I'm having trouble responding at the moment."
            }
        }
    }

    private suspend fun injectInitialPrompt(completeContext: String): String {
        msgs.add(SystemMessage(completeContext))
        val request = createChatCompletionRequest(msgs, 512)

        return retryPolicy.execute {
            val initialResponse = sendMessage(request)
            val initialChoice = initialResponse.choices.firstOrNull()?.message
            if (initialChoice != null) {
                hasInjectedInitialPrompt = true
                println("Initial response: ${initialChoice.content}")
                msgs.add(initialChoice)
                initialChoice.content
            } else {
                throw Exception("Failed to get initial response")
            }
        }
    }

    private suspend fun generateResponse(): String {
        val request = createChatCompletionRequest(msgs, 1024)

        return retryPolicy.execute {
            val response = sendMessage(request)
            val choice = response.choices.firstOrNull()?.message
            if (choice != null) {
                val npcResponse = choice.content
                println("npcResponse: $npcResponse")
                msgs.add(choice)
                npcResponse
            } else {
                throw Exception("Failed to generate response")
            }
        }
    }
}
