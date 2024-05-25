package grid

import korlibs.image.color.*
import korlibs.korge.view.*
import kotlin.math.*

class GridCreation(sizeX: Int, sizeY: Int, private val levelType: String) {
    companion object {
        const val CELL_SIZE = 32.0
    }

    val grid = Array(sizeX) { arrayOfNulls<SolidRect>(sizeY) }

    init {
        createGrid(sizeX, sizeY)
        useFallbackGridIfEmpty()
    }

    private fun createGrid(sizeX: Int, sizeY: Int) {
        when (levelType) {
            "junkdemo" -> {
                for (x in 0 until sizeX) {
                    for (y in 0 until sizeY) {
                        val isObstacle = (x + y) % 11 == 0
                        if (isObstacle) {
                            grid[x][y] = SolidRect(CELL_SIZE, CELL_SIZE, Colors.LIGHTGRAY).apply {
                                this.x = (x * CELL_SIZE).roundToInt().toDouble()
                                this.y = (y * CELL_SIZE).roundToInt().toDouble()
                            }
                        }
                    }
                }
            }
            // Add other level types and their grid creation logic here
        }
    }

    private fun useFallbackGridIfEmpty() {
        for (x in grid.indices) {
            for (y in grid[x].indices) {
                if (grid[x][y] == null) {
                    val isObstacle = (x + y) % 11 == 0
                    if (isObstacle) {
                        grid[x][y] = SolidRect(CELL_SIZE, CELL_SIZE, Colors.LIGHTGRAY).apply {
                            this.x = (x * CELL_SIZE).roundToInt().toDouble()
                            this.y = (y * CELL_SIZE).roundToInt().toDouble()
                        }
                    }
                }
            }
        }
    }

    fun addToContainer(container: Container) {
        for (i in grid.indices) {
            for (j in grid[i].indices) {
                grid[i][j]?.let {
                    container.addChild(it)
                }
            }
        }
    }
}
