package grid

import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.addTo
import kotlin.math.roundToInt

class GridCreation(sizeX: Int, sizeY: Int, private val cellSize: Double) {
    val grid = Array(sizeX) { arrayOfNulls<SolidRect>(sizeY) }

    init {
        createGrid(sizeX, sizeY, cellSize)
    }

    private fun createGrid(sizeX: Int, sizeY: Int, cellSize: Double) {
        for (i in 0 until sizeX) {
            for (j in 0 until sizeY) {
                // Create a square for each grid cell
                val cell = SolidRect(cellSize, cellSize).apply {
                    this.x = (i * cellSize).roundToInt().toDouble()
                    this.y = (j * cellSize).roundToInt().toDouble()
                }
                grid[i][j] = cell
            }
        }
    }

    fun addToContainer(container: Container) {
        for (i in grid.indices) {
            for (j in grid[i].indices) {
                container.addChild(grid[i][j]!!)
            }
        }
    }
}
