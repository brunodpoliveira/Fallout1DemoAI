package npc

import korlibs.datastructure.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlinx.coroutines.*
import utils.*
import kotlin.random.*

class Movement(private val character: View,
               private val pathfinding: Pathfinding,) {
    private val speed = 50.0 // pixels per second

    suspend fun moveToPoint(targetX: Double, targetY: Double) {
        if (!GameState.isPaused) {
            val target = Point(targetX, targetY)
            moveAlongPath(target)
        }
    }

    suspend fun patrol(points: List<Point>) {
        if (points.size > 5) throw IllegalArgumentException("Patrol can have a maximum of 5 points")
        while (true) {
            for (point in points) {
                if (!GameState.isPaused) {
                    moveAlongPath(point)
                    delay(500) // Optional delay between points
                } else {
                    delay(100) // Small delay to prevent busy waiting when paused
                }
            }
        }
    }

    private suspend fun moveAlongPath(target: Point) {
        val path = pathfinding.findPath(character.pos, target)

        if (path.isEmpty()) {
            println("No path found to $target")
            return
        }

        var currentIndex = 0
        while (currentIndex < path.size) {
            if (GameState.isPaused) {
                delay(100) // Small delay to prevent busy waiting when paused
                continue
            }

            val nextPoint = path[currentIndex]
            val direction = (nextPoint - character.pos).normalized
            val distance = character.pos.distanceTo(nextPoint)
            var remainingDistance = distance

            while (remainingDistance > 0) {
                if (GameState.isPaused) {
                    delay(100) // Small delay to prevent busy waiting when paused
                    continue
                }

                val deltaTime = 16.milliseconds
                val moveDistance = speed * (deltaTime.seconds)
                if (moveDistance >= remainingDistance) {
                    character.pos = nextPoint
                    remainingDistance = 0.0
                } else {
                    character.pos += direction * moveDistance
                    remainingDistance -= moveDistance
                }
                character.zIndex = character.y
                delay(deltaTime)
            }
            currentIndex++
        }
    }

    private val sectorMap = mapOf(
        "CHEST_ROOM" to 1,
        "MAIN_ROOM" to 2,
        "STATUE" to 3,
        "CORRIDOR" to 4,
        "TOWN_SQUARE" to 5
    )
    suspend fun moveToSector(ldtk: LDTKWorld, sectorName: String, grid: IntIArray2) {
        val level = ldtk.levelsByName["Level_0"] ?: throw IllegalArgumentException("Level Level_0 not found")
        val gWidth = grid.width
        val gHeight = grid.height

        println("Attempting to move to sector: $sectorName")

        val targetIntGridValue = sectorMap[sectorName]
            ?: throw IllegalArgumentException("Sector $sectorName is not defined in sectorMap")

        if (!GameState.isPaused) {
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

            moveAlongPath(scaledCoords)
        }
    }
}
