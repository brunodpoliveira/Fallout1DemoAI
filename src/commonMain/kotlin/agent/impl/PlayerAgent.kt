package agent.impl

import agent.core.*
import korlibs.math.geom.*
import utils.*

class PlayerAgent(
    override val id: String,
    override val name: String = "Player",
    override var faction: String = "Player",
    private val inventory: Inventory,
    private val stats: EntityStats
) : Agent {
    override var position: Point = stats.position

    override fun update() {
        // Player state is driven by input rather than autonomous updates
    }

    override suspend fun processInput(input: AgentInput): AgentOutput =
        AgentOutput(Decision.Accept("Processed"), emptyList())

    override suspend fun decide(context: InteractionContext): Decision =
        Decision.Accept("Player always accepts") // Player decisions are handled by UI

    override fun isAvailableForInteraction(): Boolean = true

    override fun canInteractWith(other: Agent): Boolean = true

    override suspend fun moveTo(target: Point) {
        position = target
    }

    override suspend fun speak(message: String) {
        // Handled by dialog system
    }
}
