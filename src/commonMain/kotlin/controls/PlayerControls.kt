package controls

import korlibs.event.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import manager.*
import ui.*

@OptIn(DelicateCoroutinesApi::class)
class PlayerControls(
    private val player: Image,
    private val grid: Array<Array<SolidRect?>>,
    private val npcs: List<NPC>,
    private val cellSize: Double,
    private val mainPlayer: Player
) {
    private var currentPos = Point(0, 0)
    private var direction = Vector2D(0.0, 0.0)
    private var interacting = false

    private fun movePlayer() {
        val newX = (currentPos.x + direction.x.toInt()).coerceIn(0.0, ((grid.size - 1).toDouble()))
        val newY = (currentPos.y + direction.y.toInt()).coerceIn(0.0, ((grid[0].size - 1).toDouble()))

        val collisionGrid = grid[newX.toInt()][newY.toInt()] != null

        val collisionEntity = npcs.any { npc ->
            val npcX = (npc.x / cellSize).toInt()
            val npcY = (npc.y / cellSize).toInt()
            npcX == newX.toInt() && npcY == newY.toInt()
        }

        if (!collisionGrid && !collisionEntity) {
            currentPos = Point(newX, newY)
            player.position(newX * cellSize, newY * cellSize)
        } else {
            println("Collision at: $newX, $newY")
        }
    }

    private fun interactWithNPC() {
        if (interacting) return  // Ensure only one interaction per key press

        // Finding the closest NPC
        val collidingNPC = npcs.filter { npc ->
            npc.bounds.intersects(mainPlayer.bounds)
        }.minByOrNull { npc ->
            Point.distance(mainPlayer.x, mainPlayer.y, npc.x, npc.y)
        }

        collidingNPC?.let {
            println("Interacting with ${it.npcName}")
            startDialog(it)
            interacting = true
        }
    }

    private fun startDialog(npc: NPC) {
        println("Starting dialog with ${npc.npcName}: ${npc.bio}")
        val dialog = DialogWindow()
        mainPlayer.parent?.parent?.let { parent ->
            if (parent is Container) {
                dialog.show(parent)
            } else {
                println("Error: The parent of the stage is not a Container.")
            }
        } ?: run {
            println("Error: The stage or its parent is null.")
        }
    }

    init {
        player.addUpdater {
            keys.down { keyEvent ->
                when (keyEvent.key) {
                    Key.W -> direction = Vector2D(0.0, -1.0)
                    Key.S -> direction = Vector2D(0.0, 1.0)
                    Key.A -> direction = Vector2D(-1.0, 0.0)
                    Key.D -> direction = Vector2D(1.0, 0.0)
                    Key.ENTER -> interactWithNPC()
                    else -> { }
                }
            }
            keys.up { keyEvent ->
                when (keyEvent.key) {
                    Key.W, Key.S, Key.A, Key.D -> direction = Vector2D(0.0, 0.0)
                    Key.ENTER -> interacting = false  // Reset the interaction flag on key release
                    else -> { }
                }
            }
        }
        GlobalScope.launch {
            while (true) {
                movePlayer()
                delay(100)
            }
        }
    }
}
