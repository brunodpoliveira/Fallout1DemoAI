package grid

import korlibs.image.color.*
import korlibs.korge.view.*
import kotlin.math.*

class GridCreation(val sizeX: Int, val sizeY: Int, private val levelType: String) {
    companion object {
        const val CELL_SIZE = 32.0
        val WALL_COLOR = Colors.LIGHTGRAY
        val FLOOR_COLOR = Colors.DARKGRAY
    }

    val grid = Array(sizeX) { arrayOfNulls<SolidRect>(sizeY) }

    init {
        createGrid()
    }

    private fun createGrid() {
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                grid[x][y] = when (levelType) {
                    "junkdemo" -> {
                        val isObstacle = (x + y) % 11 == 0
                        if (isObstacle) {
                            SolidRect(CELL_SIZE, CELL_SIZE, WALL_COLOR).apply {
                                this.x = (x * CELL_SIZE).roundToInt().toDouble()
                                this.y = (y * CELL_SIZE).roundToInt().toDouble()
                            }
                        } else {
                            SolidRect(CELL_SIZE, CELL_SIZE, Colors.BLUE).apply {
                                this.x = (x * CELL_SIZE).roundToInt().toDouble()
                                this.y = (y * CELL_SIZE).roundToInt().toDouble()
                            }
                        }
                    }
                    else -> SolidRect(CELL_SIZE, CELL_SIZE, Colors.WHITE).apply {
                        this.x = (x * CELL_SIZE).roundToInt().toDouble()
                        this.y = (y * CELL_SIZE).roundToInt().toDouble()
                    }
                }
            }
        }
        println("Grid created with dimensions: $sizeX x $sizeY")
    }

    fun addToContainer(container: Container) {
        println("Adding grid to container")
        for (x in grid.indices) {
            for (y in grid[x].indices) {
                grid[x][y]?.let { container.addChild(it) }
            }
        }
    }
}
