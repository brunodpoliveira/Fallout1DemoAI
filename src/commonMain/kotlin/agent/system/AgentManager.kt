package agent.system

import agent.core.*
import agent.impl.*
import korlibs.korge.ldtk.view.*
import npc.*
import korlibs.datastructure.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import utils.*

class AgentManager(
    private val npcManager: NPCManager,
    private val ldtk: LDTKWorld,
    private val grid: IntIArray2,
    private val coroutineScope: CoroutineScope
) {
    private val agents = mutableMapOf<String, Agent>()
    private val agentPositions = mutableMapOf<String, Point>()

    // Public interface methods
    fun getAgentCount(): Int = agents.size
    fun getAllAgents(): List<Agent> = agents.values.toList()
    fun getAgent(id: String): Agent? = agents[id]

    fun getAgentsInFaction(faction: String): List<Agent> =
        agents.values.filter { it.faction == faction }

    fun getAgentsInRange(position: Point, radius: Double): List<Agent> =
        agents.values.filter {
            (agentPositions[it.id]?.distanceTo(position) ?: Double.MAX_VALUE) <= radius
        }

    fun getNearbyAgents(agentId: String, radius: Double): List<Agent> {
        val agentPos = agentPositions[agentId] ?: return emptyList()
        return agents.values.filter { other ->
            other.id != agentId &&
                (agentPositions[other.id]?.distanceTo(agentPos) ?: Double.MAX_VALUE) <= radius
        }
    }

    fun registerPlayer(player: PlayerAgent) {
        agents[player.id] = player
        updateAgentPosition(player.id, player.position)
    }

    suspend fun initializeAgents() {
        npcManager.initializeNPCs()
        npcManager.npcs.forEach { (id, entityView) ->
            try {
                val agent = createAgentFromEntity(entityView)
                agents[id] = agent
                updateAgentPosition(id, agent.position)

                Logger.debug("""
                    Initialized agent:
                    - ID: ${agent.id}
                    - Name: ${agent.name}
                    - Faction: ${agent.faction}
                    - Position: ${agent.position}
                """.trimIndent())
            } catch (e: Exception) {
                Logger.error("Failed to initialize agent for entity $id: ${e.message}")
            }
        }
    }

    private fun createAgentFromEntity(entity: LDTKEntityView): Agent {
        validateEntityFields(entity)

        return BaseNPC(
            id = entity.entity.identifier,
            name = entity.fieldsByName["Name"]!!.valueString.toString(),
            faction = entity.fieldsByName["Faction"]?.valueString ?: "Neutral",
            bio = entity.fieldsByName["Bio"]?.valueString ?: "",
            character = entity,
            pathfinding = npcManager.pathfinding,
            broadcastLocation = { id, pos -> updateAgentPosition(id, pos) },
            ldtk = ldtk,
            grid = grid,
            stats = readEntityStats(entity),
            coroutineScope = coroutineScope
        )
    }

    private fun validateEntityFields(entity: LDTKEntityView) {
        requireNotNull(entity.fieldsByName["Name"]) { "Entity missing required Name field" }
        requireNotNull(entity.entity.identifier) { "Entity missing required identifier" }

        val position = entity.pos
        require(!position.x.isNaN() && !position.y.isNaN()) {
            "Entity has invalid position: $position"
        }
    }

    private fun updateAgentPosition(agentId: String, position: Point) {
        agentPositions[agentId] = position
        npcManager.broadcastLocation(agentId, position)
    }
}
