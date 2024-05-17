package controls

import korlibs.event.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

@OptIn(DelicateCoroutinesApi::class)
class PlayerControls(private val player: SolidRect, private val grid: Array<Array<SolidRect?>>, private val cellSize: Double) {
    private var currentPos = Point(0, 0)
    private var direction = Vector2D(0.0, 0.0)

    private fun movePlayer() {
        val newX = (currentPos.x + direction.x.toInt()).coerceIn(0.0, ((grid.size - 1).toDouble()))
        val newY = (currentPos.y + direction.y.toInt()).coerceIn(0.0, ((grid[0].size - 1).toDouble()))

        // Collision detection
        if (grid[newX.toInt()][newY.toInt()] != null) {
            currentPos = Point(newX, newY)
            player.position(newX * cellSize, newY * cellSize)
        }
    }

    init {
        player.addUpdater {
            keys.down {
                when (it.key) {
                    Key.W -> direction = Vector2D(0.0, -1.0)
                    Key.S -> direction = Vector2D(0.0, 1.0)
                    Key.A -> direction = Vector2D(-1.0, 0.0)
                    Key.D -> direction = Vector2D(1.0, 0.0)
                    else -> {}
                }
            }
            keys.up {
                when (it.key) {
                    Key.W, Key.S, Key.A, Key.D -> direction = Vector2D(0.0, 0.0)
                    else -> {}
                }
            }
        }

        // Move the player at a fixed interval
        GlobalScope.launch {
            while (true) {
                movePlayer()
                delay(100) // Adjust as needed for the game speed
            }
        }
    }
}
