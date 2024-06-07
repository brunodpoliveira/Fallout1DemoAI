package bvh

import korlibs.korge.view.*
import korlibs.math.geom.*

class BvhEntity(val world: BvhWorld, val view: View) {

    fun update() {
        val rect = view.getBounds(world.baseView)
        val pos = rect.getAnchoredPoint(Anchor.BOTTOM_CENTER)
        world.bvh.insertOrUpdate(RectangleD(pos - Point(8, 16), Size(16, 16)), this)
    }
}
