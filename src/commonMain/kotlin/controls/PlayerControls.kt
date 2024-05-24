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
    private val player: Player,
    private val grid: Array<Array<SolidRect?>>,
    private val npcs: List<NPC>,
    private val cellSize: Double
) {
    private val boundingBox = player.boundingBox
    private var interacting = false

    private var direction = Vector2D(0.0, 0.0)

    private fun movePlayer() {
        val newX = (player.x + direction.x).coerceIn(0.0, (grid.size - 1) * cellSize)
        val newY = (player.y + direction.y).coerceIn(0.0, (grid[0].size - 1) * cellSize)

        val collisionGrid = grid[(newX / cellSize).toInt()][(newY / cellSize).toInt()] != null
        val collisionEntity = npcs.any { npc ->
            npc.bounds.intersects(Rectangle(
                newX,
                newY,
                player.width / 8,
                player.height / 8))
        }

        if (!collisionGrid && !collisionEntity) {
            player.position(newX, newY)
            boundingBox.position(newX, newY)
        }
    }

    private fun interactWithNPC() {
        if (interacting) return

        val collidingNPC = npcs.find { npc ->
            npc.bounds.intersects(player.bounds)
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
        player.parent?.parent?.let { parent ->
            dialog.show(parent, npc.bio, npc.npcName)
        } ?: run {
            println("Error: The stage or its parent is null.")
        }
    }

    init {
        boundingBox.addUpdater {
            keys {
                down {
                    when (it.key) {
                        Key.W -> direction = Vector2D(0.0, -cellSize)
                        Key.S -> direction = Vector2D(0.0, cellSize)
                        Key.A -> direction = Vector2D(-cellSize, 0.0)
                        Key.D -> direction = Vector2D(cellSize, 0.0)
                        Key.ENTER -> interactWithNPC()
                        else -> {}
                    }
                }
                up {
                    when (it.key) {
                        Key.W, Key.S, Key.A, Key.D -> direction = Vector2D(0.0, 0.0)
                        Key.ENTER -> interacting = false
                        else -> {}
                    }
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
