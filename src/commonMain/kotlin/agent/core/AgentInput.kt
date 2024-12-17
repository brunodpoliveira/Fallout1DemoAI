package agent.core

sealed class AgentInput {
    data class StartConversation(
        val targetId: String,
        val message: String
    ) : AgentInput()

    data class ReceiveMessage(
        val fromId: String,
        val message: String
    ) : AgentInput()

    data class RequestItem(
        val fromId: String,
        val itemId: String
    ) : AgentInput()

    data class Observe(
        val event: String,
        val sourceId: String? = null
    ) : AgentInput()
}
