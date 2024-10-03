package bvh

import korlibs.korge.view.*

class BvhEntity(private val world: BvhWorld, val view: View) {
    fun update() {
        val rect = view.getBounds(world.baseView)
        world.bvh.insertOrUpdate(rect, this)
    }
}
