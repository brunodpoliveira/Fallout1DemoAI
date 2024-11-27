package agent.core

sealed class Decision {
    data class Accept(val response: String) : Decision()
    data class Reject(val reason: String) : Decision()
    data class Counter(val alternativeProposal: String) : Decision()
    object Ignore : Decision()
}
