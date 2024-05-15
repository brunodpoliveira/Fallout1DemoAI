import com.theokanning.openai.completion.chat.*
import com.theokanning.openai.service.*
import java.time.*


object OpenAIService {
    private val apiKey = System.getenv("API_KEY") ?: System.getProperty("apiKey")
    ?: error("API_KEY is not defined")
    private val service = OpenAiService(apiKey, Duration.ofSeconds(30))

    private const val RAYZE_BIO = """
        Rayze is the leader of the Crypts gang. He is a tough and strategic individual who values control over the town's power generator. 
        He is cunning and manipulative, often trying to outsmart his rivals. He is known for his leather armor and his tactical prowess.
        He's a man of few words and prefers to get straight to the point. His demeanor can seem cold, but he cares deeply about his gang.
    """

    fun getChatResponse(userInput: String, npcBio: String = RAYZE_BIO): String {
        val message = ChatMessage("system", "$npcBio\nUser: $userInput")
        val request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(listOf(message))
            .temperature(0.7)
            .maxTokens(100)
            .build()

        val response = service.createChatCompletion(request)
        return response.choices.firstOrNull()?.message?.content ?: "Sorry, I couldn't understand that."
    }
}
