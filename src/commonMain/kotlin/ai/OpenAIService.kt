package ai

import com.theokanning.openai.completion.chat.*
import com.theokanning.openai.service.*

object OpenAIService {
    //TODO add prompt so the NPC can think through its dialogue and decide what to do
    //Ex: "I should GO TO [NPC], I should ATTACK [NPC], etc
    private const val API_KEY = ""
    private val service = OpenAiService(API_KEY)
    val msgs: MutableList<ChatMessage> = ArrayList()
    private var thisSessionMsgCounter = 0
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
            println("OpenAiHttpException: Server timeout")
        }
        return response
    }

    fun getCharacterResponse(characterBio: String, playerInput: String): String {
        val message = ChatMessage("user", playerInput)
        val initialPrompt = "Bio: $characterBio\nContext: ${Director.getContext()}\nPlayer: $playerInput\nNPC: \n"
        println("initialPrompt: $initialPrompt")

        if (thisSessionMsgCounter == 0) {
            val initialAssistantMessage = ChatMessage("assistant", initialPrompt)
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
                thisSessionMsgCounter += 1
                println(initialChoices[0].content.toString())
                msgs.add(initialChoices[0])
                return initialChoices[0].content
            }
        }

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
}
