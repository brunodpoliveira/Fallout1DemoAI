package agent.core

import korlibs.math.geom.*

interface Agent {
    val id: String
    val name: String
    var position: Point
    var faction: String

    // Core capabilities
    suspend fun processInput(input: AgentInput): AgentOutput
    suspend fun decide(context: InteractionContext): Decision

    // State checks
    fun isAvailableForInteraction(): Boolean
    fun canInteractWith(other: Agent): Boolean

    // Basic actions
    suspend fun moveTo(target: Point)
    suspend fun speak(message: String)
}
