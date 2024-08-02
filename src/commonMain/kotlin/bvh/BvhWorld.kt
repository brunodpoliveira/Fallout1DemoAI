package bvh

import korlibs.datastructure.ds.*
import korlibs.korge.view.*
import korlibs.math.geom.ds.*

class BvhWorld(val baseView: View) {
    val bvh = BVH2D<BvhEntity>()
    fun getAll(): List<BVH.Node<BvhEntity>> = bvh.search(bvh.envelope())

    private fun add(view: View): BvhEntity {
        return BvhEntity(this, view).also { it.update() }
    }

    private fun remove(view: View) {
        return bvh.remove(BvhEntity(this, view))
    }

    operator fun plusAssign(view: View) {
        add(view)
    }

    operator fun minusAssign(view: View) {
        remove(view)
    }
}
