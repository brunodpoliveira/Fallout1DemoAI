package ai

import com.theokanning.openai.completion.*
import com.theokanning.openai.service.*

class SummarizerService {
    private val apiKey = ""
    private val service = OpenAiService(apiKey)

    fun summarizeConversation(conversation: String): String {
        val prompt = """
            Summarize the following conversation in a concise way:
            
            Conversation: $conversation
            
            Summary:
        """.trimIndent()

        val completionRequest = CompletionRequest.builder()
            .prompt(prompt)
            .model("gpt-3.5-turbo")
            .maxTokens(200)
            .build()

        val completion = service.createCompletion(completionRequest).choices[0].text.trim()
        return completion
    }
}
