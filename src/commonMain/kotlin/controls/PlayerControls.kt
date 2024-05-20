package controls

import korlibs.event.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import manager.*

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
        val collidingNPC = npcs.find { npc ->
            npc.bounds.intersects(mainPlayer.bounds)
        }

        collidingNPC?.let {
            println("Interacting with ${it.npcName}")
            startDialog(it)
        }
    }

    private fun startDialog(npc: NPC) {
        println("Starting dialog with ${npc.npcName}: ${npc.bio}")
    }

    init {
        player.addUpdater {
            keys.down { keyEvent ->
                when (keyEvent.key) {
                    Key.W -> direction = Vector2D(0.0, -1.0)
                    Key.S -> direction = Vector2D(0.0, 1.0)
                    Key.A -> direction = Vector2D(-1.0, 0.0)
                    Key.D -> direction = Vector2D(1.0, 0.0)
                    //TODO calling function every tick. FIX
                    Key.ENTER -> interactWithNPC()
                    else -> { }
                }
            }
            keys.up { keyEvent ->
                when (keyEvent.key) {
                    Key.W, Key.S, Key.A, Key.D -> direction = Vector2D(0.0, 0.0)
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
