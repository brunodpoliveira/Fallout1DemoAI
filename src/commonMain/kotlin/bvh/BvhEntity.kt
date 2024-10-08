package bvh

import korlibs.korge.view.*

class BvhEntity(private val world: BvhWorld, val view: View) {
    fun update() {
        world.updateEntityBounds(this)
    }
}
