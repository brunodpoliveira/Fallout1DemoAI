package agent.core

import korlibs.math.geom.*

sealed class AgentAction {
    data class Move(val destination: Point) : AgentAction()
    data class Speak(val message: String) : AgentAction()
    data class GiveItem(val itemId: String, val targetId: String) : AgentAction()
    data class TakeItem(val itemId: String, val targetId: String) : AgentAction()
    data class StartDialog(val targetId: String, val opening: String) : AgentAction()
    data class LeaveDialog(val reason: String) : AgentAction()
}
