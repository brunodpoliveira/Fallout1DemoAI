package npc

import korlibs.datastructure.*
import korlibs.math.geom.*
import korlibs.math.algo.*

class Pathfinding(private val map: BooleanArray2) {
    private val astar: AStar = AStar(map)

    fun findPath(start: Point, end: Point): List<Point> {
        println("Attempting to find path from $start to $end")
        val startInt = clampPoint(start.toInt())
        val endInt = clampPoint(end.toInt())
        val pointsInt = astar.find(startInt.x, startInt.y, endInt.x, endInt.y, findClosest = true, diagonals = true)
        return pointListToPoints(pointsInt)
    }

    private fun Point.toInt() = PointInt(kotlin.math.round(this.x).toInt(), kotlin.math.round(this.y).toInt())

    private fun pointListToPoints(pointIntList: PointIntList): List<Point> {
        val points = mutableListOf<Point>()
        for (i in 0 until pointIntList.size) {
            val x = pointIntList.getX(i).toDouble()
            val y = pointIntList.getY(i).toDouble()
            points.add(Point(x, y))
        }
        return points
    }

    private fun clampPoint(point: PointInt): PointInt {
        val maxX = if (map.width > 0) map.width - 1 else 0
        val maxY = if (map.height > 0) map.height - 1 else 0
        val clampedX = point.x.coerceIn(0, maxX)
        val clampedY = point.y.coerceIn(0, maxY)
        println("Clamping point $point to ($clampedX, $clampedY)")
        return PointInt(clampedX, clampedY)
    }
}

data class PointInt(val x: Int, val y: Int)
