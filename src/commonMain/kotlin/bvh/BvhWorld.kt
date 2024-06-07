package bvh

import korlibs.datastructure.ds.*
import korlibs.korge.view.*
import korlibs.math.geom.ds.*

class BvhWorld(val baseView: View) {
    val bvh = BVH2D<BvhEntity>()
    fun getAll(): List<BVH.Node<BvhEntity>> = bvh.search(bvh.envelope())
    fun add(view: View): BvhEntity {
        return BvhEntity(this, view).also { it.update() }
    }

    operator fun plusAssign(view: View) {
        add(view)
    }
}
