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

        val initiator = agents.getAgent(initiatorId) ?: return false
        val target = agents.getAgent(targetId) ?: return false

        if (isAgentInInteraction(initiatorId) || isAgentInInteraction(targetId)) {
            return false
        }

        if (!initiator.canInteractWith(target)) {
            Logger.debug("${initiator.name} cannot interact with ${target.name}")
            return false
        }

        val context = InteractionContext(
            initiator = initiator,
            target = target,
            input = AgentInput.StartConversation(
                targetId = targetId,
                message = "Would you like to talk?"
            ),
            currentLocation = initiator.position
        )

        val decision = target.decide(context)

        when (decision) {
            is Decision.Accept -> {
                val interaction = Interaction(
                    initiator = initiator,
                    target = target,
                    startTime = System.currentTimeMillis(),
                    state = InteractionState.INITIATING,
                    context = context
                )

                activeInteractions[initiatorId] = interaction
                activeInteractions[targetId] = interaction

                if (target is BaseNPC) {
                    target.setInConversation(initiatorId)
                }
                if (initiator is BaseNPC) {
                    initiator.setInConversation(targetId)
                }

                dialogManager.showDialog(interaction)

                return true
            }
            else -> return false
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
