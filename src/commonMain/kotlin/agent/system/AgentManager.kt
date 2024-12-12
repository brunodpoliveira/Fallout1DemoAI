package agent.system

import agent.core.*
import agent.impl.*
import ai.*
import bvh.*
import img.*
import korlibs.datastructure.*
import korlibs.image.atlas.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import maps.*
import utils.*

class AgentManager(
    private val coroutineScope: CoroutineScope,
    private val ldtk: LDTKWorld,
    private val grid: IntIArray2,
    private val entities: List<LDTKEntityView>,
    private val levelView: LDTKLevelView,
    private val entitiesBvh: BvhWorld,
    private val mapManager: MapManager
) {
    private val agents = mutableMapOf<String, Agent>()
    private val entityViews = mutableMapOf<String, LDTKEntityView>()
    private val agentPositions = mutableMapOf<String, Point>()
    private val agentInventories = mutableMapOf<String, Inventory>()
    private val pathfinding = AgentPathfinding(mapManager.generateMap(levelView))

    suspend fun initializeAgents() {
        val gameData = JsonLoader.loadGameData()
        val levelData = gameData.levels["scrapheap"] ?: return

        val atlas = MutableAtlasUnit()
        val npcSprites = mapOf(
            "Rayze" to resourcesVfs["gfx/minotaur.ase"].readImageDataContainer(ASE.toProps(), atlas),
            "Baka" to resourcesVfs["gfx/wizard_f.ase"].readImageDataContainer(ASE.toProps(), atlas),
            "Robot" to resourcesVfs["gfx/cleric_f.ase"].readImageDataContainer(ASE.toProps(), atlas)
        )

        entities.filter { entity ->
            val nameField = entity.fieldsByName["Name"]
            val npcName = nameField?.valueString
            !npcName.isNullOrEmpty() && npcName != "Player"
        }.forEach { entity ->
            val npcName = entity.fieldsByName["Name"]!!.valueString.toString()
            val npcData = levelData.npcData[npcName]

            // Use faction from game data, fallback to Neutral if not found
            val faction = npcData?.faction ?: "Neutral"
            val bio = npcData?.bio ?: ""

            initializeAgent(entity, npcSprites[npcName], bio, faction)
        }

        initializeMovements()
        startUpdatingBVH()
    }

    private fun initializeAgent(
        entity: LDTKEntityView,
        sprite: ImageDataContainer?,
        bio: String,
        faction: String
    ) {
        val npcName = entity.fieldsByName["Name"]!!.valueString.toString()
        val stats = readEntityStats(entity)

        sprite?.let { spriteData ->
            entity.replaceView(
                ImageDataView2(spriteData.default).also {
                    it.smoothing = false
                    it.animation = "idle"
                    it.anchor(Anchor.BOTTOM_CENTER)
                    it.play()
                }
            )
        }

        val agent = BaseNPC(
            coroutineScope = coroutineScope,
            id = npcName,
            name = npcName,
            faction = faction,
            bio = bio,
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
        Logger.debug("Initialized agent $npcName at ${entity.pos} with faction: $faction")
    }

    fun registerPlayer(player: PlayerAgent) {
        agents[player.id] = player
        updateAgentPosition(player.id, player.position)
    }

    fun getAgent(id: String): Agent? {
        val agent = agents[id]
        if (agent == null) {
            Logger.debug("No agent found with ID: $id (Available agents: ${agents.keys.joinToString()})")
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
