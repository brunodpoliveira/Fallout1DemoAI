package npc

import ai.*
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
    private val gridSize: Size,
    private val mapManager: MapManager,
    private val levelView: LDTKLevelView
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

        entities.filter {
            it.fieldsByName["Name"] != null && it.fieldsByName["Name"]!!.valueString != "Player"
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

        println("$npcName initial position: ${entity.pos}")
        println("$npcName HP: ${stats.hp}")
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
