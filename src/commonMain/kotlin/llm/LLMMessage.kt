package llm

sealed class LLMMessage {
    abstract val content: String
    abstract val role: String
}

data class SystemMessage(
    override val content: String,
    override val role: String = "system"
) : LLMMessage()

data class UserMessage(
    override val content: String,
    override val role: String = "user"
) : LLMMessage()

data class AssistantMessage(
    override val content: String,
    override val role: String = "assistant"
) : LLMMessage()
