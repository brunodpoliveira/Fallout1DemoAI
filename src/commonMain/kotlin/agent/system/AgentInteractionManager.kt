package agent.system

import agent.core.*
import agent.impl.*
import dialog.*
import utils.*

class AgentInteractionManager(
    private val dialogManager: DialogManager
) {
    private val activeInteractions = mutableMapOf<String, Interaction>()
    var agentManager: AgentManager? = null

    data class Interaction(
        val initiator: Agent,
        val target: Agent,
        val startTime: Long,
        var state: InteractionState,
        var context: InteractionContext? = null
    )

    enum class InteractionState {
        INITIATING,
        IN_PROGRESS,
        ENDING
    }

    fun initializeAgentManager(manager: AgentManager) {
        agentManager = manager
    }

    fun update() {
        val currentTime = System.currentTimeMillis()
        activeInteractions.values.toSet().forEach { interaction ->
            if (interaction.state == InteractionState.INITIATING &&
                currentTime - interaction.startTime > 5000) {
                endInteraction(interaction.initiator.id)
                return@forEach
            }
            agentManager?.getAgent(interaction.target.id)?.update()
        }
    }

    suspend fun initiateInteraction(initiatorId: String, targetId: String): Boolean {
        val agents = agentManager ?: run {
            Logger.error("AgentManager not initialized")
            return false
        }

        Logger.debug("Initiating interaction: $initiatorId -> $targetId")
        val initiator = agents.getAgent(initiatorId)
        val target = agents.getAgent(targetId)

        Logger.debug("Initiator: ${initiator?.name} (${initiator?.faction})")
        Logger.debug("Target: ${target?.name} (${target?.faction})")

        if (target == null) {
            if (targetId.contains("Chest", ignoreCase = true)) {
                return true
            }
            Logger.debug("Target $targetId not found")
            return false
        }

        if (isAgentInInteraction(initiatorId) || isAgentInInteraction(targetId)) {
            Logger.debug("One of the agents is already in an interaction")
            return false
        }

        Logger.debug("Checking if ${initiator?.name} can interact with ${target.name}")
        if (initiator != null && !initiator.canInteractWith(target)) {
            Logger.debug("${initiator.name} cannot interact with ${target.name}")
            return false
        }

        Logger.debug("Checking if ${target.name} is available")
        if (!target.isAvailableForInteraction()) {
            Logger.debug("${target.name} is not available for interaction")
            return false
        }

        val context = InteractionContext(
            initiator = initiator ?: return false,
            target = target,
            input = AgentInput.StartConversation(targetId = targetId, message = "Hello"),
            currentLocation = initiator.position
        )

        Logger.debug("Getting decision from ${target.name}")
        val decision = target.decide(context)
        Logger.debug("${target.name}'s decision: $decision")

        when (decision) {
            is Decision.Accept -> {
                Logger.debug("Starting interaction between ${initiator.name} and ${target.name}")
                startInteraction(initiator, target, context)
                return true
            }
            else -> {
                Logger.debug("Interaction rejected: ${(decision as? Decision.Reject)?.reason ?: "unknown reason"}")
                return false
            }
        }
    }

    suspend fun processAgentAction(agent: Agent, action: AgentAction) {
        when (action) {
            is AgentAction.Move -> {
                agent.moveTo(action.destination)
            }
            is AgentAction.Speak -> {
                val interaction = activeInteractions[agent.id]
                interaction?.let {
                    agent.speak(action.message)
                }
            }
            is AgentAction.GiveItem -> {
                val target = agentManager?.getAgent(action.targetId)
                target?.let {
                    Logger.debug("${agent.name} giving ${action.itemId} to ${target.name}")
                }
            }
            is AgentAction.TakeItem -> {
                val target = agentManager?.getAgent(action.targetId)
                target?.let {
                    Logger.debug("${agent.name} taking ${action.itemId} from ${target.name}")
                }
            }
            is AgentAction.StartDialog -> {
                val target = agentManager?.getAgent(action.targetId)
                target?.let {
                    initiateInteraction(agent.id, target.id)
                }
            }
            is AgentAction.LeaveDialog -> {
                endInteraction(agent.id)
            }
        }
    }

    private fun startInteraction(initiator: Agent, target: Agent, context: InteractionContext) {
        val interaction = Interaction(
            initiator = initiator,
            target = target,
            startTime = System.currentTimeMillis(),
            state = InteractionState.INITIATING,
            context = context
        )

        activeInteractions[initiator.id] = interaction
        activeInteractions[target.id] = interaction

        if (target is BaseNPC) {
            target.setInConversation(initiator.id)
        }
        if (initiator is BaseNPC) {
            initiator.setInConversation(target.id)
        }

        dialogManager.showDialog(interaction)

        Logger.debug("Started interaction between ${initiator.name} and ${target.name}")
    }

    fun endInteraction(agentId: String) {
        val interaction = activeInteractions[agentId] ?: return

        activeInteractions.remove(interaction.initiator.id)
        activeInteractions.remove(interaction.target.id)

        (interaction.initiator as? BaseNPC)?.setIdle()
        (interaction.target as? BaseNPC)?.setIdle()
    }

    suspend fun handleAgentResponse(agent: Agent, input: AgentInput): AgentOutput {
        val output = agent.processInput(input)
        output.actions.forEach { action ->
            processAgentAction(agent, action)
        }
        return output
    }

    fun getActiveInteraction(agentId: String): Interaction? = activeInteractions[agentId]

    fun isAgentInInteraction(agentId: String): Boolean = activeInteractions.containsKey(agentId)
}
