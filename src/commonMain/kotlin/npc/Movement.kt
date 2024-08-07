package npc

import korlibs.korge.view.*
import korlibs.math.geom.Point
import kotlinx.coroutines.delay
import kotlin.math.*

class Movement(private val character: View, private val pathfinding: Pathfinding) {

    //TODO fix; chars still zig-zagging a bit;
    //TODO it keeps running even after NPC gets to destination; make it stop once it reaches their destination
    //TODO add patrol movem which will make char move in a perimeter until said otherwise
    //TODO pause movement if game is paused
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

    suspend fun moveToPoint(targetX: Double, targetY: Double) {
        while (true) {
            delay(1000)
            val target = Point(targetX, targetY)
            moveToSmooth(target)
        }
    }

    private suspend fun moveToSmooth(target: Point) {
        //println("Moving ${character.name} to $target")

        val path = pathfinding.findPath(character.pos, target)
        //println("Path found for ${character.name}: $path")

        val stepCount = 20
        for (point in path) {
            val startPosition = character.pos
            val stepX = (point.x - startPosition.x) / stepCount
            val stepY = (point.y - startPosition.y) / stepCount

            for (i in 1..stepCount) {
                val nextX = (startPosition.x + stepX * i).roundToInt().toDouble()
                val nextY = (startPosition.y + stepY * i).roundToInt().toDouble()
                character.pos = Point(nextX, nextY)
                //println("Position of ${character.name} at ${character.pos}")
                delay(1)
            }
        }
    }
}
