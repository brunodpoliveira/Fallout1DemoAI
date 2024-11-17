package raycasting

import bvh.*
import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.image.color.*
import korlibs.korge.animate.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.ds.*
import korlibs.math.interpolation.*
import korlibs.math.raycasting.*
import korlibs.time.*
import utils.*

class Raycaster(
    val grid: IntIArray2,
    private val gridSize: Size,
    private val entitiesBvh: BvhWorld,
    private val entities: List<LDTKEntityView>,
    private val player: View,
    private val highlight: Graphics
) {
    private fun IntIArray2.check(it: PointInt): Boolean {
        if (!this.inside(it.x, it.y)) return true
        val v = this.getAt(it.x, it.y)
        return v != 1 && v != 3
    }

    fun hitTest2(pos: Point): Boolean {
        val results = entitiesBvh.bvh.search(Rectangle.fromBounds(pos - Point(1, 1), pos + Point(1, 1)))
        for (result in results) {
            val view = result.value?.view ?: continue
            if (view == player) continue
            val entityView = view as? LDTKEntityView
            val doBlock = entityView?.fieldsByName?.get("Collides")
            if (doBlock?.valueString == "false") continue
            return true
        }
        return grid.check((pos / gridSize).toInt())
    }

    fun doRay(pos: Point, dir: Vector2D, property: String): RayResult? {
        val dir = Vector2D(
            if (dir.x.isAlmostEquals(0.0)) .00001 else dir.x,
            if (dir.y.isAlmostEquals(0.0)) .00001 else dir.y,
        )
        val ray = Ray(pos, dir)
        val outResults = arrayListOf<RayResult?>()
        val blockedResults = arrayListOf<RayResult>()
        outResults += grid.raycast(ray, gridSize, collides = { check(it) })?.also { it.view = null }
        for (result in entitiesBvh.bvh.intersect(ray)) {
            val view = result.obj.value?.view
            if (view == player) continue
            val rect = result.obj.d.toRectangle()
            val intersectionPos = ray.point + ray.direction.normalized * result.intersect
            val normalX = if (intersectionPos.x <= rect.left + 0.5f) -1f else if (intersectionPos.x >= rect.right - .5f) +1f else 0f
            val normalY = if (intersectionPos.y <= rect.top + 0.5f) -1f else if (intersectionPos.y >= rect.bottom - .5f) +1f else 0f
            val rayResult = RayResult(ray, intersectionPos, Vector2D(normalX, normalY)).also { it.view = view }
            val entityView = view as? LDTKEntityView
            val doBlock = entityView?.fieldsByName?.get(property)
            if (doBlock?.valueString == "false") {
                blockedResults += rayResult
                continue
            }
            outResults += rayResult
        }
        return outResults.filterNotNull().minByOrNull { it.point.distanceTo(pos) }?.also { res ->
            val dist = res.point.distanceTo(pos)
            res.blockedResults = blockedResults.filter { it.point.distanceTo(pos) < dist }
        }
    }

    fun updateRay(pos: Point): Double {
        val anglesCount = 64
        val angles = (0 until anglesCount).map { Angle.FULL * (it.toDouble() / anglesCount.toDouble()) }
        val results: ArrayList<RayResult> = arrayListOf()
        val results2: ArrayList<RayResult> = arrayListOf()
        val anglesDeque = Deque(angles)
        while (anglesDeque.isNotEmpty()) {
            val angle = anglesDeque.removeFirst()
            val last = results.lastOrNull()
            val current = doRay(pos, Vector2D.polar(angle), "Occludes") ?: continue
            current.blockedResults?.let { results2 += it }
            if (last != null && (last.point.distanceTo(current.point) >= 16 || last.normal != current.normal)) {
                val lastAngle = last.ray.direction.angle
                val currentAngle = current.ray.direction.angle
                if ((lastAngle - currentAngle).absoluteValue >= 0.25.degrees) {
                    anglesDeque.addFirst(angle)
                    anglesDeque.addFirst(
                        Angle.fromRatio(0.5.toRatio().interpolate(lastAngle.ratio, currentAngle.ratio))
                    )
                    continue
                }
            }
            results += current
        }

        // Manage entity visibility based on raycasting results
        entities.fastForEach { entity ->
            if ("hide_on_fog" in entity.entity.tags) {
                entity.simpleAnimator.cancel().sequence {
                    tween(entity::alpha[if (entity != player) .1f else 1f], time = 0.25.seconds)
                }
            }
        }
        for (result in (results + results2)) {
            val view = result.view ?: continue
            if (view.alpha != 1.0) {
                view.simpleAnimator.cancel().sequence {
                    tween(view::alpha[1f], time = 0.25.seconds)
                }
            }
        }

        // Update the highlight graphics (fog of war)
        highlight.updateShape {
            fill(Colors["#FFFFFF55"]) {
                rect(0, 0, 600, 500)
            }
            fill(Colors.WHITE) {
                var first = true
                for (result in results) {
                    if (first) {
                        first = false
                        moveTo(result.point)
                    } else {
                        lineTo(result.point)
                    }
                }
                close()
            }
            fill(Colors.WHITE) {
                for (result in results) {
                    val view = result.view ?: continue
                    rect(view.getBounds(highlight).expanded(MarginInt(-2)))
                }
            }
        }

        return results.minOfOrNull { it.point.distanceTo(pos) } ?: 0.0
    }
}
