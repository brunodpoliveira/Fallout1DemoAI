package ai

import utils.Logger
import utils.LogLevel
import com.theokanning.openai.client.*
import com.theokanning.openai.completion.chat.*
import com.theokanning.openai.service.*
import com.theokanning.openai.service.OpenAiService.buildApi
import kotlinx.coroutines.*
import utils.*
import java.io.*
import java.time.*
import java.util.Properties

object OpenAIService {
    private val API_KEY: String = getApiKey()
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
        val environment = Logger.environment
        Logger.setLogLevel(if (environment == "prod") LogLevel.ERROR else LogLevel.DEBUG)
        Logger.debug("Initializing OpenAIService in $environment environment")

        api = buildApi(API_KEY, Duration.ofSeconds(INITIAL_TIMEOUT.toLong()))
        service = OpenAiService(api)
        Logger.debug("OpenAI API and service initialized")
    }

    private fun getApiKey(): String {
        Logger.debug("Attempting to retrieve API key...")

        // Try to load from classpath
        try {
            val inputStream = OpenAIService::class.java.classLoader.getResourceAsStream("config.properties")
            if (inputStream != null) {
                Logger.debug("Found config.properties in classpath")
                val properties = Properties()
                properties.load(inputStream)
                val apiKey = properties.getProperty("open.api.key")
                if (apiKey != null) {
                    Logger.debug("API key found in classpath config.properties")
                    return apiKey
                } else {
                    Logger.warn("open.api.key property not found in classpath config.properties")
                }
            } else {
                Logger.warn("config.properties not found in classpath")
            }
        } catch (e: Exception) {
            Logger.error("Error reading config.properties from classpath: ${e.message}")
        }

        // Try to load from current working directory
        val currentDir = System.getProperty("user.dir")
        Logger.debug("Current working directory: $currentDir")
        val configFile = File(currentDir, "config.properties")
        if (configFile.exists()) {
            Logger.debug("Found config.properties in current working directory")
            try {
                val properties = Properties()
                configFile.inputStream().use { properties.load(it) }
                val apiKey = properties.getProperty("open.api.key")
                if (apiKey != null) {
                    Logger.debug("API key found in current directory config.properties")
                    return apiKey
                } else {
                    Logger.warn("open.api.key property not found in current directory config.properties")
                }
            } catch (e: Exception) {
                Logger.error("Error reading config.properties from current directory: ${e.message}")
            }
        } else {
            Logger.warn("config.properties not found in current working directory")
        }

        // If we've reached this point, we couldn't find the API key
        val errorMessage = "API key not found. Please ensure config.properties is present and contains the open.api.key property."
        Logger.error(errorMessage)
        throw IllegalStateException(errorMessage)
    }

    fun createChatCompletionRequest(messages: List<ChatMessage>, maxTokens: Int): ChatCompletionRequest {
        Logger.debug("Creating chat completion request with ${messages.size} messages and $maxTokens max tokens")
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

    suspend fun sendMessage(chatRequest: ChatCompletionRequest): ChatCompletionResult {
        Logger.debug("Sending chat completion request")
        return retryPolicy.execute {
            try {
                val result = service.createChatCompletion(chatRequest)
                Logger.debug("Received chat completion response")
                result
            } catch (e: Exception) {
                Logger.error("Error in sendMessage: ${e.message}")
                throw e
            }
        }
    }

    fun getCharacterResponse(npcName: String, factionName: String?, characterBio: String, playerInput: String): String {
        Logger.debug("Getting character response for NPC: $npcName, Faction: $factionName")
        Logger.debug("Player input: $playerInput")

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
                    Logger.debug("Injecting initial prompt")
                    injectInitialPrompt(completeContext)
                }
                msgs.add(UserMessage(playerInput))
                generateResponse()
            } catch (e: Exception) {
                Logger.error("Error in getCharacterResponse: ${e.message}")
                "I'm having trouble responding at the moment."
            }
        }
    }

    private suspend fun injectInitialPrompt(completeContext: String): String {
        Logger.debug("Injecting initial prompt")
        msgs.add(SystemMessage(completeContext))
        val request = createChatCompletionRequest(msgs, 512)

        return retryPolicy.execute {
            val initialResponse = sendMessage(request)
            val initialChoice = initialResponse.choices.firstOrNull()?.message
            if (initialChoice != null) {
                hasInjectedInitialPrompt = true
                Logger.debug("Initial response: ${initialChoice.content}")
                msgs.add(initialChoice)
                initialChoice.content
            } else {
                val errorMessage = "Failed to get initial response"
                Logger.error(errorMessage)
                throw Exception(errorMessage)
            }
        }
    }

    private suspend fun generateResponse(): String {
        Logger.debug("Generating response")
        val request = createChatCompletionRequest(msgs, 1024)

        return retryPolicy.execute {
            val response = sendMessage(request)
            val choice = response.choices.firstOrNull()?.message
            if (choice != null) {
                val npcResponse = choice.content
                Logger.debug("NPC Response: $npcResponse")
                msgs.add(choice)
                npcResponse
            } else {
                val errorMessage = "Failed to generate response"
                Logger.error(errorMessage)
                throw Exception(errorMessage)
            }
        }
    }

    fun resetConversation() {
        Logger.debug("Resetting conversation")
        msgs.clear()
        hasInjectedInitialPrompt = false
        Logger.debug("Conversation reset: message history cleared and initial prompt flag reset")
    }
}
