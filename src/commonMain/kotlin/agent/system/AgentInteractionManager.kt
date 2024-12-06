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
        var state: InteractionState
    )

    enum class InteractionState {
        INITIATING,
        IN_PROGRESS,
        ENDING
    }

    fun update() {
        val currentTime = System.currentTimeMillis()
        activeInteractions.values.toSet().forEach { interaction ->
            // Remove stalled interactions
            if (interaction.state == InteractionState.INITIATING &&
                currentTime - interaction.startTime > 5000) {
                endInteraction(interaction.initiator.id)
            }

            // Process any pending agent actions
            agentManager?.let { manager ->
                manager.getAgent(interaction.target.id)?.let { agent ->
                    if (agent is BaseNPC) {
                        // Update NPC state and process any queued actions
                        agent.update()
                    }
                }
            }
        }
    }

    fun initializeAgentManager(manager: AgentManager) {
        agentManager = manager
    }

    suspend fun initiateInteraction(initiatorId: String, targetId: String): Boolean {
        val agentMgr = agentManager ?: run {
            Logger.error("AgentManager not set")
            return false
        }

        val initiator = agentMgr.getAgent(initiatorId) ?: return false
        val target = agentMgr.getAgent(targetId) ?: return false

        // Check if either agent is already in an interaction
        if (activeInteractions.containsKey(initiatorId) || activeInteractions.containsKey(targetId)) {
            Logger.debug("Interaction failed - one or both agents already in interaction")
            return false
        }

        // Create interaction context
        val context = InteractionContext(
            initiator = initiator,
            target = target,
            input = AgentInput.StartConversation(
                targetId = target.toString(),
                message = "Would you like to talk?"
            ),
            currentLocation = initiator.position
        )

        // Get target's decision
        val decision = target.decide(context)

        when (decision) {
            is Decision.Accept -> {
                val interaction = Interaction(
                    initiator = initiator,
                    target = target,
                    startTime = System.currentTimeMillis(),
                    state = InteractionState.INITIATING
                )

                activeInteractions[initiatorId] = interaction
                activeInteractions[targetId] = interaction

                // Start dialog if accepted
                dialogManager.showDialog(
                    npcName = target.name,
                    npcBio = "Agent ${target.name} from ${target.faction} faction",
                    factionName = target.faction
                )

                return true
            }

            is Decision.Reject -> {
                Logger.debug("Interaction rejected by ${target.name}: ${decision.reason}")
                return false
            }

            is Decision.Counter -> {
                Logger.debug("Counter-proposal from ${target.name}: ${decision.alternativeProposal}")
                return false
            }

            Decision.Ignore -> {
                Logger.debug("Interaction ignored by ${target.name}")
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
                if (interaction != null) {
                    dialogManager.showDialog(
                        npcName = agent.name,
                        npcBio = "Agent speaking: ${action.message}",
                        factionName = agent.faction
                    )
                }
            }

            is AgentAction.GiveItem -> {
                val target = agentManager?.getAgent(action.targetId)
                if (target != null) {
                    Logger.debug("${agent.name} giving ${action.itemId} to ${target.name}")
                }
            }

            is AgentAction.TakeItem -> {
                val target = agentManager?.getAgent(action.targetId)
                if (target != null) {
                    Logger.debug("${agent.name} taking ${action.itemId} from ${target.name}")
                }
            }

            is AgentAction.StartDialog -> {
                val target = agentManager?.getAgent(action.targetId)
                if (target != null) {
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

        // Reset agent states if they implement BaseNPC
        (interaction.initiator as? BaseNPC)?.setIdle()
        (interaction.target as? BaseNPC)?.setIdle()

        Logger.debug("Ended interaction between ${interaction.initiator.name} and ${interaction.target.name}")
    }

    suspend fun makeDecision(agent: Agent, input: AgentInput): Decision {
        val context = buildContext(agent, input)
        return agent.decide(context)
    }

    private fun buildContext(agent: Agent, input: AgentInput): InteractionContext {
        val target = when(input) {
            is AgentInput.StartConversation -> agentManager?.getAgent(input.targetId)
            is AgentInput.ReceiveMessage -> agentManager?.getAgent(input.fromId)
            else -> null
        } ?: return defaultContext(agent)

        return InteractionContext(
            initiator = agent,
            target = target,
            input = input,
            currentLocation = agent.position
        )
    }

    private fun defaultContext(agent: Agent) = InteractionContext(
        initiator = agent,
        target = agent, // Self as target for non-interactive inputs
        input = AgentInput.Observe("No specific context"),
        currentLocation = agent.position
    )

    fun getActiveInteraction(agentId: String): Interaction? = activeInteractions[agentId]

    fun isAgentInInteraction(agentId: String): Boolean = activeInteractions.containsKey(agentId)
}
