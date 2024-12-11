package scenes

import agent.core.*
import kotlinx.coroutines.*
import utils.*

class DemoLevel : BaseLevelScene("scrapheap") {
    override suspend fun initializeLevelSpecifics() {
        // Test suite controller - uncomment tests as needed
        debugTestBasicAgentFunctionality()
        //debugTestInteractions()
        //debugTestItemExchange()
        //debugTestDecisionMaking()
    }

    private fun debugTestBasicAgentFunctionality() {
        Logger.debug("Starting basic agent functionality tests...")

        val agentCount = sceneLoader.agentManager.getAgentCount()
        Logger.debug("Initial agent count: $agentCount")

        sceneLoader.agentManager.getAllAgents().forEach { agent ->
            Logger.debug("Agent ${agent.id} initial state:")
            Logger.debug("- Available: ${agent.isAvailableForInteraction()}")
            Logger.debug("- Faction: ${agent.faction}")
            Logger.debug("- Position: ${agent.position}")

            // Test factional grouping
            val factionMates = sceneLoader.agentManager.getAgentsInFaction(agent.faction)
            Logger.debug("- Faction members: ${factionMates.size}")

            // Test proximity grouping
            val nearbyAgents = sceneLoader.agentManager.getAgentsInRange(agent.position, 100.0)
            Logger.debug("- Nearby agents: ${nearbyAgents.size}")
        }

        testBasicDecisionMaking()
        Logger.debug("Basic agent functionality tests completed.")
    }

    private fun testBasicDecisionMaking() {
        val allAgents = sceneLoader.agentManager.getAllAgents()
        allAgents.forEach { agent ->
            if (allAgents.size > 1) {
                val testContext = InteractionContext(
                    initiator = agent,
                    target = allAgents.first { it.id != agent.id },
                    input = AgentInput.Observe("Test observation"),
                    currentLocation = agent.position
                )

                runBlocking {
                    val decision = agent.decide(testContext)
                    Logger.debug("Agent ${agent.id} test decision: $decision")
                }
            }
        }
    }

    private fun debugTestInteractions() {
        Logger.debug("Starting agent interaction tests...")

        val allAgents = sceneLoader.agentManager.getAllAgents()
        if (allAgents.size >= 2) {
            runBlocking {
                val agent1 = allAgents[0]
                val agent2 = allAgents[1]

                val interactionSuccess = sceneLoader.agentInteractionManager.initiateInteraction(
                    agent1.id,
                    agent2.id
                )
                Logger.debug("Interaction initiation success: $interactionSuccess")

                if (interactionSuccess) {
                    val conversation = sceneLoader.agentInteractionManager
                        .getActiveInteraction(agent1.id)
                    Logger.debug("Active conversation state: ${conversation?.state}")
                }
            }
        }

        Logger.debug("Agent interaction tests completed.")
    }

    private fun debugTestItemExchange() {
        Logger.debug("Starting item exchange tests...")

        val rayze = sceneLoader.agentManager.getAgent("Rayze")
        val baka = sceneLoader.agentManager.getAgent("Baka")

        if (rayze != null && baka != null) {
            sceneLoader.agentManager.getAgentInventory("Rayze")?.apply {
                addItem("TEST_POTION")
                addItem("TEST_WEAPON")
            }

            runBlocking {
                val giveInput = AgentInput.RequestItem(baka.id, "TEST_POTION")
                val giveOutput = rayze.processInput(giveInput)
                Logger.debug("Give command output: $giveOutput")

                val takeInput = AgentInput.RequestItem(rayze.id, "TEST_WEAPON")
                val takeOutput = baka.processInput(takeInput)
                Logger.debug("Take command output: $takeOutput")

                logInventories()
            }
        }

        Logger.debug("Item exchange tests completed.")
    }

    private fun logInventories() {
        Logger.debug("Current Inventories:")
        sceneLoader.agentManager.getAllAgents().forEach { agent ->
            val inventory = sceneLoader.agentManager.getAgentInventory(agent.id)
            Logger.debug("${agent.name}'s inventory: ${inventory?.getItems()}")
        }
    }
}
