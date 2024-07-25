package npc

import korlibs.korge.view.*
import korlibs.math.geom.Point
import kotlinx.coroutines.delay
import scenes.*
import kotlin.math.*

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
        println("Moving ${character.name} to $target")
        val path = pathfinding.findPath(character.pos, target)
        println("Path found for ${character.name}: $path")
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
                println("Position of ${character.name} at ${character.pos}")
                delay(1)
            }
        }
        // Once reaching the final destination point, break the loop
        character.pos = target
    }
}
