package maps

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.properties.*


class MapManager(
    val ldtk: LDTKWorld,
    val gridSize: Size
) {
    lateinit var grid: IntIArray2
    var gridWidth by Delegates.notNull<Int>()
    var gridHeight by Delegates.notNull<Int>()

    /*
    fun generateMap(levelName: String = "Level_0"): BooleanArray2 {
        val level =
            ldtk.levelsByName[levelName] ?: throw IllegalArgumentException("Level $levelName not found in LDtk world")
        gridWidth = level.level.pxWid / gridSize.width.toInt()
        gridHeight = level.level.pxHei / gridSize.height.toInt()
        grid = level.layerIntGrid

        // Generate obstacle map
        val obstacleMap = BooleanArray2(gridWidth, gridHeight) { x, y ->
        }
    }

     */

    fun generateMap(levelView: LDTKLevelView, levelName: String = "Level_0"): BooleanArray2 {
        val level = ldtk.levelsByName[levelName] ?: throw IllegalArgumentException("Level $levelName not found in LDtk world")

        gridWidth = level.level.pxWid / gridSize.width.toInt()
        gridHeight = level.level.pxHei / gridSize.height.toInt()

        grid = levelView.layerViewsByName["Collision"]!!.intGrid

        val lWidth = level.level.pxWid
        val lHeight = level.level.pxHei
        val gWidth = gridWidth
        val gHeight = gridHeight

        // Initialize the BooleanArray2 with all cells set to false (walkable)
        val gridArray = BooleanArray(gWidth * gHeight) { false }
        val array = BooleanArray2(gWidth, gHeight, gridArray)

        // First Pass: Mark cells in the "Collision" layer as walkable (false) or obstacles (true)
        level.layersByName.values.forEach { layer ->
            when (layer.layer.identifier) {
                "Collision" -> {
                    layer.layer.intGridCSV.forEachIndexed { index, value ->
                        val x = index % gWidth
                        val y = index / gWidth
                        array[x, y] = value != 0  // true means collision/blocked
                    }
                }
            }
        }

        // Second Pass: Mark cells in the "Entities" layer as obstacles (true)
        level.layersByName.values.forEach { layer ->
            when (layer.layer.identifier) {
                "Entities" -> {
                    layer.layer.entityInstances.forEach { entity ->
                        val cx = entity.grid[0]
                        val cy = entity.grid[1]
                        when (entity.identifier) {
                            "Object", "Chest" -> {
                                array[cx, cy] = true
                            }
                        }
                    }
                }
            }
        }

        // Rescale the grid array to match the level dimensions
        val levelArray = BooleanArray(lWidth * lHeight) { true }
        val scaledArray = BooleanArray2(lWidth, lHeight, levelArray)

        for (y in 0 until lHeight) {
            for (x in 0 until lWidth) {
                val scaledX = (x * gWidth) / lWidth
                val scaledY = (y * gHeight) / lHeight
                scaledArray[x, y] = array[scaledX, scaledY]
            }
        }
        return scaledArray
    }

    private fun displayObstacleMap(view: Container, obstacleMap: BooleanArray2, scaleFactor: Double = 1.0) {
        val displayWidth = 300.0
        val displayHeight = 300.0

        val rectWidth = (displayWidth / obstacleMap.width) * scaleFactor
        val rectHeight = (displayHeight / obstacleMap.height) * scaleFactor

        // Start position for the obstacle map
        val offsetX = 0.0
        val offsetY = 0.0

        val graphics = view.graphics {
            Rectangle(0, 0, displayWidth, displayHeight)
            fill(Colors.BLACK) { Rectangle(0, 0, displayWidth, displayHeight) } // Background
        }

        // Draw the obstacle and free cells
        graphics.updateShape {
            for (y in 0 until obstacleMap.height) {
                for (x in 0 until obstacleMap.width) {
                    val color = if (obstacleMap[x, y]) Colors.DARKGREEN else Colors.GREEN
                    fill(color) {
                        rect(offsetX + x * rectWidth, offsetY + y * rectHeight, rectWidth, rectHeight)
                    }
                }
            }
        }
    }

    private fun scaleEntityPositions(entities: List<PointInt>): List<PointInt> {
        val mapScale = 2
        return entities.map { point ->
            val x = (point.x * mapScale) - 10
            val y = (point.y * mapScale) - 45
            PointInt(x, y)
        }
    }
}
