package agent.impl

import agent.core.*
import korlibs.datastructure.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import npc.*
import utils.*

class BaseNPC(
    override val id: String,
    override val name: String,
    override var faction: String,
    private val bio: String,
    private val character: View,
    private val pathfinding: Pathfinding,
    private val broadcastLocation: (String, Point) -> Unit,
    private val ldtk: LDTKWorld,
    private val grid: IntIArray2,
    val stats: EntityStats = EntityStats(100, 0, Point(0, 0))
) : Agent {
    override var position: Point = Point(0, 0)
    private val inventory = Inventory(id)
    private var currentState: NPCState = NPCState.Idle
    private var interactionTarget: String? = null

    private val movement = Movement(
        character = character,
        pathfinding = pathfinding,
        npcName = name,
        broadcastLocation = broadcastLocation
    )

    override suspend fun processInput(input: AgentInput): AgentOutput {
        return when(input) {
            is AgentInput.StartConversation -> handleConversationStart(input)
            is AgentInput.ReceiveMessage -> handleMessage(input)
            is AgentInput.RequestItem -> handleItemRequest(input)
            is AgentInput.Observe -> handleObservation(input)
        }
    }

    override suspend fun decide(context: InteractionContext): Decision {
        return when (currentState) {
            NPCState.Idle -> decideFromIdle(context)
            NPCState.InConversation -> decideInConversation(context)
            NPCState.Busy -> Decision.Reject("Currently busy")
        }
    }

    override fun isAvailableForInteraction(): Boolean {
        return currentState == NPCState.Idle
    }

    override fun canInteractWith(other: Agent): Boolean {
        return when {
            other.faction == faction -> true
            faction == "Neutral" -> true
            other.faction == "Neutral" -> true
            else -> false
        }
    }

    override suspend fun moveTo(target: Point) {
        try {
            movement.moveToPoint(target.x, target.y)
            position = target  // Update internal position after movement
        } catch (e: Exception) {
            Logger.error("Movement failed: ${e.message}")
            // Handle movement failure
        }
    }

    suspend fun patrol(points: List<Point>) {
        currentState = NPCState.Busy
        try {
            movement.patrol(points)
        } catch (e: Exception) {
            Logger.error("Patrol failed: ${e.message}")
            currentState = NPCState.Idle
        }
    }

    suspend fun moveToSector(sectorName: String) {
        currentState = NPCState.Busy
        try {
            movement.moveToSector(ldtk, sectorName, grid)
            currentState = NPCState.Idle
        } catch (e: Exception) {
            Logger.error("Sector movement failed: ${e.message}")
            currentState = NPCState.Idle
        }
    }

    override suspend fun speak(message: String) {
        // Basic speech implementation - will be enhanced with dialog system integration
        println("$name: $message")
    }

    private fun handleConversationStart(input: AgentInput.StartConversation): AgentOutput {
        return if (isAvailableForInteraction()) {
            currentState = NPCState.InConversation
            AgentOutput(
                Decision.Accept("Sure, let's talk."),
                listOf(AgentAction.Speak("Hello! What would you like to discuss?"))
            )
        } else {
            AgentOutput(
                Decision.Reject("Not available"),
                emptyList()
            )
        }
    }

    private fun handleMessage(input: AgentInput.ReceiveMessage): AgentOutput {
        return AgentOutput(
            Decision.Accept("Message received"),
            listOf(AgentAction.Speak("I hear what you're saying."))
        )
    }

    private fun handleItemRequest(input: AgentInput.RequestItem): AgentOutput {
        return if (inventory.hasItem(input.itemId)) {
            AgentOutput(
                Decision.Accept("Can give item"),
                listOf(AgentAction.GiveItem(input.itemId, interactionTarget ?: return defaultReject()))
            )
        } else {
            defaultReject()
        }
    }

    private fun handleObservation(input: AgentInput.Observe): AgentOutput {
        return AgentOutput(Decision.Accept("Noted"), emptyList())
    }

    private fun decideFromIdle(context: InteractionContext): Decision {
        return if (canInteractWith(context.initiator)) {
            Decision.Accept("Ready to interact")
        } else {
            Decision.Reject("Cannot interact with ${context.initiator.name}")
        }
    }

    private fun decideInConversation(context: InteractionContext): Decision {
        return if (context.initiator.id == interactionTarget) {
            Decision.Accept("Continuing conversation")
        } else {
            Decision.Reject("Already in conversation")
        }
    }

    private fun defaultReject() = AgentOutput(Decision.Reject("Cannot comply"), emptyList())

    // New: State management helpers
    fun setBusy() {
        currentState = NPCState.Busy
    }

    fun setIdle() {
        currentState = NPCState.Idle
    }

    fun setInConversation(targetId: String) {
        currentState = NPCState.InConversation
        interactionTarget = targetId
    }
}
enum class NPCState {
    Idle,
    InConversation,
    Busy
}
