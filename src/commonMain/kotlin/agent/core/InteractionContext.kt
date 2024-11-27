package agent.core

import korlibs.math.geom.*

data class InteractionContext(
    val initiator: Agent,
    val target: Agent,
    val input: AgentInput,
    val currentLocation: Point
)
