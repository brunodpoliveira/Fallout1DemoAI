package agent.core

sealed class AgentInput {
    data class StartConversation(val message: String) : AgentInput()
    data class ReceiveMessage(val message: String, val fromId: String) : AgentInput()
    data class RequestItem(val itemId: String) : AgentInput()
    data class Observe(val event: String) : AgentInput()
}
