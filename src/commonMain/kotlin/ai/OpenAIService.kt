package ai

import com.theokanning.openai.completion.chat.*
import com.theokanning.openai.service.*

object OpenAIService {
    private const val API_KEY = "YOUR_OPENAI_API_KEY"
    private val service = OpenAiService(API_KEY)
    val msgs: MutableList<ChatMessage> = ArrayList()
    private var hasInjectedInitialPrompt = false
    private const val TIMEOUT = 15 * 1000L

    private fun handleTimeoutError(elapsedTime: Long, timeout: Long): Boolean {
        if (elapsedTime > timeout) {
            println("Server took too long to respond.")
            return true
        }
        return false
    }

    fun sendMessage(chatRequest: ChatCompletionRequest): ChatCompletionResult {
        val startTime = System.currentTimeMillis()
        val response = service.createChatCompletion(chatRequest)
        val elapsedTime = System.currentTimeMillis() - startTime
        if (handleTimeoutError(elapsedTime, TIMEOUT)) {
            println("Server took too long to respond.")
        }
        return response
    }

    fun getCharacterResponse(npcName: String, factionName: String?, characterBio: String, playerInput: String): String {
        val message = ChatMessage("user", playerInput)
        println("Player input: $playerInput")

        val factionContext = factionName?.let { Director.getFactionContext(it) } ?: ""
        val completeContext = """
            Bio: $characterBio
            General Context: ${Director.getContext()}
            Faction Context: $factionContext
            NPC Context: ${Director.getNPCContext(npcName)}
            Personality: [OCEAN] 
            Openness: LOW|MEDIUM|HIGH (adjust as necessary).
            Conscientiousness: LOW|MEDIUM|HIGH (adjust as necessary), 
            Extroversion: LOW|MEDIUM|HIGH (adjust as necessary), 
            Agreeableness: LOW|MEDIUM|HIGH (adjust as necessary), 
            Neuroticism: LOW|MEDIUM|HIGH (adjust as necessary), 
            Player: $playerInput
            DO NOT talk about non-existent characters, items, and locations
            NPC: 
        """.trimIndent()

        if (!hasInjectedInitialPrompt) {
            val initialAssistantMessage = ChatMessage("assistant", completeContext)
            msgs.add(initialAssistantMessage)

            val initialChatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(msgs)
                .temperature(.9)
                .maxTokens(512)
                .topP(1.0)
                .frequencyPenalty(.8)
                .presencePenalty(.8)
                .build()

            val initialHttpResponse = sendMessage(initialChatCompletionRequest)
            val initialChoices = initialHttpResponse.choices.mapNotNull { it.message }

            if (initialChoices.isNotEmpty()) {
                hasInjectedInitialPrompt = true
                println("Initial response: ${initialChoices[0].content}")
                msgs.add(initialChoices[0])
                return initialChoices[0].content
            }
        } else {
            msgs.add(message)
            val chatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(msgs)
                .temperature(.9)
                .maxTokens(1024)
                .topP(1.0)
                .frequencyPenalty(.8)
                .presencePenalty(.8)
                .build()

            val httpResponse = sendMessage(chatCompletionRequest)
            val choices = httpResponse.choices.mapNotNull { it.message }

            if (choices.isNotEmpty()) {
                val npcResponse = choices[0].content
                println("npcResponse: $npcResponse")
                msgs.add(choices[0])
                return npcResponse
            } else {
                return "I don't have a response at the moment."
            }
        }
        return "I don't have a response at the moment."
    }

    fun resetConversation() {
        msgs.clear()
        hasInjectedInitialPrompt = false
    }
}
