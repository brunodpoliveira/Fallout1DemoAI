package agent.system

import agent.core.*
import korlibs.korge.ldtk.view.*
import npc.*
import agent.impl.BaseNPC
import korlibs.datastructure.*
import utils.*

class AgentManager(
    private val npcManager: NPCManager,
    private val ldtk: LDTKWorld,
    private val grid: IntIArray2
) {
    private val agents = mutableMapOf<String, Agent>()

    suspend fun initializeAgents() {
        npcManager.initializeNPCs()
        npcManager.npcs.forEach { (id, entityView) ->
            agents[id] = createAgentFromEntity(entityView)
        }
    }

    private fun createAgentFromEntity(entity: LDTKEntityView): Agent {
        val npcName = entity.fieldsByName["Name"]?.valueString ?: error("NPC has no name")
        val faction = entity.fieldsByName["Faction"]?.valueString ?: "Neutral"
        val stats = readEntityStats(entity)

        return BaseNPC(
            id = entity.entity.identifier,
            name = npcName,
            faction = faction,
            bio = entity.fieldsByName["Bio"]?.valueString ?: "",
            character = entity,
            pathfinding = npcManager.pathfinding,
            broadcastLocation = npcManager::broadcastLocation,
            ldtk = ldtk,
            grid = grid,
            stats = stats,
        )
    }

    fun getAgent(id: String): Agent? = agents[id]

    fun getNearbyAgents(agentId: String, radius: Double): List<Agent> {
        val nearbyNPCs = npcManager.getNearbyNPCs(agentId, radius)
        return nearbyNPCs.mapNotNull { (id, _) -> agents[id] }
    }
}
