package ai

import com.theokanning.openai.completion.*
import com.theokanning.openai.service.*

object OpenAIService {
    //TODO add prompt so the NPC can think throughg its dialogue and decide what to do
    //Ex: "I should GO TO [NPC], I should ATTACK [NPC], etc
    private const val API_KEY = ""
    private val service = OpenAiService(API_KEY)
    private val summarizerService = SummarizerService()

    fun createCharacter(bio: String): String {
        return bio
    }

    fun getCharacterResponse(characterBio: String, playerInput: String): String {
        val prompt = """
            Bio: $characterBio
            Context: ${Director.getContext()}
            Player: $playerInput
            NPC: 
        """.trimIndent()

        val completionRequest = CompletionRequest.builder()
            .prompt(prompt)
            .model("gpt-3.5-turbo")
            .maxTokens(200)
            .build()

        val completion = service.createCompletion(completionRequest).choices[0].text.trim()

        val conversation = "Player: $playerInput\nNPC: $completion"
        val summary = summarizerService.summarizeConversation(conversation)

        Director.updateContext(summary)

        return completion
    }
}
