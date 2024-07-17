package npc

import korlibs.korge.view.*
import korlibs.math.geom.Point
import kotlinx.coroutines.delay
import kotlin.math.*

class Movement(private val character: View, private val pathfinding: Pathfinding) {

    //TODO fix; chars still zig-zagging a bit;
    suspend fun moveInSquare() {
        val startPoint = character.pos
        val points = listOf(
            Point(startPoint.x + 50, startPoint.y),
            Point(startPoint.x + 50, startPoint.y + 50),
            Point(startPoint.x, startPoint.y + 50),
            Point(startPoint.x, startPoint.y)
        )

        for (point in points) {
            moveToSmooth(point)
        }
    }

    private suspend fun moveToSmooth(target: Point) {
        println("Moving ${character.name} to $target")

        val path = pathfinding.findPath(character.pos, target)
        println("Path found for ${character.name}: $path")

        val stepCount = 20
        for (point in path) {
            val startPosition = character.pos
            val stepX = (point.x - startPosition.x) / stepCount
            val stepY = (point.y - startPosition.y) / stepCount

            for (i in 1..stepCount) {
                val nextX = (startPosition.x + stepX * i).roundToInt().toDouble()
                val nextY = (startPosition.y + stepY * i).roundToInt().toDouble()
                character.pos = Point(nextX, nextY)
                println("Position of ${character.name} at ${character.pos}")
                delay(1)
            }
        }
    }
}
