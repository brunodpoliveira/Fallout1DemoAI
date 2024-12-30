package agent.system

import agent.core.*
import agent.impl.*
import ai.*
import bvh.*
import img.*
import korlibs.datastructure.*
import korlibs.image.atlas.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import maps.*
import utils.*
import kotlin.collections.List
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.joinToString
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toList

class AgentManager(
    private val coroutineScope: CoroutineScope,
    private val ldtk: LDTKWorld,
    private val grid: IntIArray2,
    private val levelId: String,
    private val entities: List<LDTKEntityView>,
    private val levelView: LDTKLevelView,
    private val entitiesBvh: BvhWorld,
    private val mapManager: MapManager
) {
    private val agents = mutableMapOf<String, Agent>()
    val entityViews = mutableMapOf<String, LDTKEntityView>()
    private val agentPositions = mutableMapOf<String, Point>()
    private val agentInventories = mutableMapOf<String, Inventory>()
    private val pathfinding = AgentPathfinding(mapManager.generateMap(levelView))
    private val spriteManager = SpriteManager(MutableAtlasUnit())

    suspend fun initializeAgents() {
        spriteManager.initialize(levelId)

        entities.filter { entity ->
            val nameField = entity.fieldsByName["Name"]
            val npcName = nameField?.valueString
            !npcName.isNullOrEmpty() && npcName != "Player"
        }.forEach { entity ->
            initializeAgent(entity)
        }

        initializeMovements()
        startUpdatingBVH()
    }

    private suspend fun initializeAgent(entity: LDTKEntityView) {
        val npcName = entity.fieldsByName["Name"]!!.valueString.toString()
        val faction = entity.fieldsByName["Faction"]?.valueString ?: "Civilian"
        val gender = entity.fieldsByName["Gender"]?.valueString ?: "Male"
        val stats = readEntityStats(entity)

        val sprite = spriteManager.getSpriteForNPC(npcName, faction, gender)

        //TODO need logic for dog sprite
        entity.replaceView(
            ImageDataView2(sprite.default).also {
                it.smoothing = false
                it.animation = "idle"
                it.anchor(Anchor.BOTTOM_CENTER)
                it.play()
            }
        )

        val agent = BaseNPC(
            coroutineScope = coroutineScope,
            id = npcName,
            name = npcName,
            faction = faction,
            bio = entity.fieldsByName["Bio"]?.valueString ?: "",
            character = entity,
            pathfinding = pathfinding,
            broadcastLocation = { id, pos -> updateAgentPosition(id, pos) },
            ldtk = ldtk,
            grid = grid,
            stats = stats
        )

        agents[npcName] = agent
        entityViews[npcName] = entity
        agentInventories[npcName] = Inventory(npcName)
        Logger.debug("Initialized agent $npcName at ${entity.pos}")
    }

    fun registerPlayer(player: PlayerAgent) {
        agents[player.id] = player
        updateAgentPosition(player.id, player.position)
    }

    fun getAgent(id: String): Agent? {
        val agent = agents[id]
        if (agent == null) {
            Logger.debug("No agent found with id $id")
            Logger.debug("Available agents: ${agents.keys.joinToString()}")

        }
        return agent
    }

    fun getAllAgents(): List<Agent> = agents.values.toList()
    fun getAgentCount(): Int = agents.size
    fun getAgentInventory(agentId: String): Inventory? = agentInventories[agentId]

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

    private fun updateAgentPosition(agentId: String, position: Point) {
        agentPositions[agentId] = position
        Logger.debug("Agent $agentId moved to (${position.x}, ${position.y})")
    }

    private fun initializeMovements() {
        agents.forEach { (id, agent) ->
            if (agent is BaseNPC) {
                MovementRegistry.addMovementForNPC(
                    id,
                    AgentMovement(
                        character = entityViews[id]!!,
                        pathfinding = pathfinding,
                        npcName = id,
                        broadcastLocation = { _, pos -> updateAgentPosition(id, pos) }
                    )
                )
            }
        }
    }

    private fun startUpdatingBVH() {
        coroutineScope.launch {
            while (true) {
                entityViews.values.forEach { view ->
                    val bvhEntity = entitiesBvh.getBvhEntity(view)
                    if (bvhEntity == null) {
                        entitiesBvh += view
                    } else {
                        bvhEntity.update()
                    }
                }
                delay(16)
            }
        }
    }
}
