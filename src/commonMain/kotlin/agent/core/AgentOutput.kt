package agent.core

data class AgentOutput(
    val decision: Decision,
    val actions: List<AgentAction>
)
