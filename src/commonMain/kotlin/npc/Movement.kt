package npc

import korlibs.datastructure.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.delay
import scenes.*
import kotlin.math.*
import kotlin.random.*

class Movement(private val character: View, private val pathfinding: Pathfinding) {

    //TODO fix; chars still zig-zagging a bit;
    suspend fun moveToPoint(targetX: Double, targetY: Double) {
        val target = Point(targetX, targetY)
        moveToSmooth(target)
    }

    suspend fun patrol(points: List<Point>) {
        if (points.size > 5) throw IllegalArgumentException("Patrol can have a maximum of 5 points")

        while (true) {
            for (point in points) {
                moveToSmooth(point)
            }
        }
    }

    private suspend fun moveToSmooth(target: Point) {
        //println("Moving ${character.name} to $target")
        val path = pathfinding.findPath(character.pos, target)
        //println("Path found for ${character.name}: $path")
        val stepCount = 20
        for (point in path) {
            if (JunkDemoScene.isPaused) {
                delay(100)
                continue
            }
            val startPosition = character.pos
            val stepX = (point.x - startPosition.x) / stepCount
            val stepY = (point.y - startPosition.y) / stepCount

            for (i in 1..stepCount) {
                if (JunkDemoScene.isPaused) {
                    delay(100)
                    continue
                }
                val nextX = (startPosition.x + stepX * i).roundToInt().toDouble()
                val nextY = (startPosition.y + stepY * i).roundToInt().toDouble()
                character.pos = Point(nextX, nextY)
                //println("Position of ${character.name} at ${character.pos}")
                delay(1)
            }
        }
        // Once reaching the final destination point, break the loop
        character.pos = target
    }

    private val sectorMap = mapOf(
        "CHEST_ROOM" to 1,
        "MAIN_ROOM" to 2,
        "STATUE" to 3,
        "CORRIDOR" to 4
    )

    suspend fun moveToSector(ldtk: LDTKWorld, sectorName: String, grid: IntIArray2) {
        val level = ldtk.levelsByName["Level_0"] ?: throw IllegalArgumentException("Level Level_0 not found")
        val gWidth = grid.width
        val gHeight = grid.height

        println("Attempting to move to sector: $sectorName")

        val targetIntGridValue = sectorMap[sectorName]
            ?: throw IllegalArgumentException("Sector $sectorName is not defined in sectorMap")

        // Initialize the BooleanArray2 with all cells set to false (walkable)
        val gridArray = BooleanArray(gWidth * gHeight) { false }
        val array = BooleanArray2(gWidth, gHeight, gridArray)

        val tileWidth = 16
        val tileHeight = 16

        // First Pass: Mark cells in the "Kind" layer as walkable (false) or obstacles (true)
        level.layersByName.values.forEach { layer ->
            when (layer.layer.identifier) {
                "Kind" -> {
                    layer.layer.intGridCSV.forEachIndexed { index, value ->
                        val x = index % gWidth
                        val y = index / gWidth
                        array[x, y] = when (value) {
                            1, 3 -> false
                            else -> true  // Any other value means blocked
                        }
                    }
                }
            }
        }

        // Mark cells in the "Entities" layer as walkable (false) or obstacles (true)
        level.layersByName.values.forEach { layer ->
            when (layer.layer.identifier) {
                "Entities" -> {
                    layer.layer.entityInstances.forEach { entity ->
                        val cx = entity.grid[0]
                        val cy = entity.grid[1]
                        when (entity.identifier) {
                            "Object", "Chest" -> {
                                array[cx, cy] = true // Obstacles
                            }
                        }
                    }
                }
            }
        }

        // Collect all cells that belong to the target sector and are walkable
        val walkableCells = mutableListOf<Point>()

        level.layersByName["Sector"]!!.layer.intGridCSV.forEachIndexed { index, value ->
            if (value == targetIntGridValue) {
                val x = (index % gWidth)
                val y = (index / gWidth)
                if (!array[x, y]) {  // Filter out obstacles
                    walkableCells.add(Point(x.toDouble(), y.toDouble()))
                }
            }
        }

        if (walkableCells.isEmpty()) {
            throw IllegalArgumentException("No walkable cells found in sector $sectorName")
        }

        // Pick a random walkable cell
        val targetCoords = walkableCells[Random.nextInt(walkableCells.size)]

        // Scale the coordinates by the tile size
        val scaledCoords = Point(targetCoords.x * tileWidth, targetCoords.y * tileHeight)
        println("Found sector $sectorName and picked random position $scaledCoords")

        moveToSmooth(scaledCoords)
    }
}
