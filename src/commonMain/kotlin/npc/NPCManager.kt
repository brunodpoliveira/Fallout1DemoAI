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
    private val npcStats: MutableMap<String, EntityStats> = mutableMapOf()
    var pathfinding: Pathfinding = Pathfinding(mapManager.generateMap(levelView))

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
            if (npcSprite != null) {
                initializeNPC(entity, npcSprite)
            } else {
                println("Sprite for NPC '$npcName' not found.")
            }
        }

        // Initialize NPC movements
        initNPCMovements()
        startUpdatingBVH()
    }

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
        entitiesBvh += npcView
        println("$npcName initial position: ${entity.pos}")
        println("$npcName HP: ${stats.hp}")
    }


    private fun startUpdatingBVH() {
        coroutineScope.launch {
            while (true) {
                npcs.values.forEach { npcView ->
                    entitiesBvh.getBvhEntity(npcView)?.update()
                }
                delay(16) // Wait for ~1 frame (assuming 60fps)
            }
        }
    }

    fun initializeNPCCollisionBoxes() {
        npcs.values.forEach { npcView ->
            entitiesBvh.getBvhEntity(npcView)?.update()
        }
    }

    fun updateNPCCollisionBoxes() {
        npcs.values.forEach { npcView ->
            entitiesBvh.getBvhEntity(npcView)?.update()
        }
    }

    private fun initNPCMovements() {
        val movementScope = coroutineScope

        npcs["Rayze"]?.let { npc ->
            MovementRegistry.addMovementForNPC("Rayze", Movement(npc, pathfinding))
            coroutineScope.launch {
                MovementRegistry.getMovementForNPC("Rayze")?.moveToPoint(253.0, 69.0)
            }
        }

        npcs["Baka"]?.let { npc ->
            val patrolPoints = listOf(
                Point(100.0, 100.0),
                Point(200.0, 100.0),
                Point(200.0, 200.0),
                Point(100.0, 200.0)
            )
            MovementRegistry.addMovementForNPC("Baka", Movement(npc, pathfinding))
            movementScope.launch {
                MovementRegistry.getMovementForNPC("Baka")?.patrol(patrolPoints)
            }
        }

        npcs["Robot"]?.let { npc ->
            MovementRegistry.addMovementForNPC("Robot", Movement(npc, pathfinding))
            movementScope.launch {
                MovementRegistry.getMovementForNPC("Robot")?.moveToSector(ldtk, "STATUE", grid)
            }
        }


    }
}
