package llm

interface LLMService {
    suspend fun chat(messages: List<LLMMessage>, maxTokens: Int = 2048): String
    suspend fun embedding(text: String): List<Float>
    suspend fun moderation(text: String): Boolean
}
