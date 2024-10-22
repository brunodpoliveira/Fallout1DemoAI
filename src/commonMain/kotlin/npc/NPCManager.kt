package npc

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

class NPCManager(
    private val coroutineScope: CoroutineScope,
    private val entities: List<LDTKEntityView>,
    private val ldtk: LDTKWorld,
    private val grid: IntIArray2,
    mapManager: MapManager,
    levelView: LDTKLevelView,
    private val entitiesBvh: BvhWorld,
) {

    val npcs = mutableMapOf<String, LDTKEntityView>()
    private val npcPositions = mutableMapOf<String, Point>()
    private val npcStats: MutableMap<String, EntityStats> = mutableMapOf()
    var pathfinding: Pathfinding = Pathfinding(mapManager.generateMap(levelView))
    private val npcInventories = mutableMapOf<String, Inventory>()

    private fun broadcastLocation(npcName: String, position: Point) {
        npcPositions[npcName] = position
        Logger.debug("[$npcName], (${position.x.toInt()}, ${position.y.toInt()})")
    }

    suspend fun initializeNPCs() {
        val atlas = MutableAtlasUnit()

        val npcSprites = mapOf(
            "Rayze" to resourcesVfs["gfx/minotaur.ase"].readImageDataContainer(ASE.toProps(), atlas),
            "Baka" to resourcesVfs["gfx/wizard_f.ase"].readImageDataContainer(ASE.toProps(), atlas),
            "Robot" to resourcesVfs["gfx/cleric_f.ase"].readImageDataContainer(ASE.toProps(), atlas)
            // Add more NPCs here
        )

        entities.filter { entity ->
            val nameField = entity.fieldsByName["Name"]
            val npcName = nameField?.valueString
            !npcName.isNullOrEmpty() && npcName != "Player"
        }.forEach { entity ->
            val npcName = entity.fieldsByName["Name"]!!.valueString
            val npcSprite = npcSprites[npcName]
            npcInventories[npcName.toString()] = npcName?.let { Inventory(it) }!!
            if (npcSprite != null) {
                initializeNPC(entity, npcSprite)
            } else {
                Logger.debug("Sprite for NPC '$npcName' not found.")
            }
        }

        initNPCMovements()
        startUpdatingBVH()
    }

    fun getNPCInventory(npcName: String): Inventory? = npcInventories[npcName]

    private fun initializeNPC(entity: LDTKEntityView, npcSprite: ImageDataContainer) {
        val npcName = entity.fieldsByName["Name"]!!.valueString.toString()
        val stats = readEntityStats(entity)
        npcStats[npcName] = stats

        val npcView = entity.apply {
            replaceView(
                ImageDataView2(npcSprite.default).also {
                    it.smoothing = false
                    it.animation = "idle"
                    it.anchor(Anchor.BOTTOM_CENTER)
                    it.play()
                }
            )
        }

        npcs[npcName] = npcView
        Logger.debug("$npcName initial position: ${entity.pos}")
        Logger.debug("$npcName HP: ${stats.hp}")
    }

    private fun startUpdatingBVH() {
        coroutineScope.launch {
            while (true) {
                updateNPCCollisionBoxes()
                delay(16) // Wait for ~1 frame (assuming 60fps)
            }
        }
    }

    private fun updateNPCCollisionBoxes() {
        npcs.values.forEach { npcView ->
            val bvhEntity = entitiesBvh.getBvhEntity(npcView)
            if (bvhEntity == null) {
                entitiesBvh += npcView
            } else {
                bvhEntity.update()
            }
        }
    }

    private fun initNPCMovements() {
        updateNPCCollisionBoxes()

        npcs.forEach { (npcName, npc) ->
            MovementRegistry.addMovementForNPC(
                npcName,
                Movement(npc, pathfinding, npcName) { name, pos -> broadcastLocation(name, pos) }
            )
        }
    }

    fun getNearbyNPCs(npcName: String, radius: Double): List<Pair<String, Point>> {
        val npcPosition = npcPositions[npcName] ?: return emptyList()
        return npcPositions.filter { (name, position) ->
            name != npcName && position.distanceTo(npcPosition) <= radius
        }.toList()
    }
}
