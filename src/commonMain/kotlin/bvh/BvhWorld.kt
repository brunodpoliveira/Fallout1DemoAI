package bvh

import korlibs.korge.view.*
import korlibs.math.geom.ds.*

class BvhWorld(val baseView: View) {
    val bvh = BVH2D<BvhEntity>()
    private val entityMap = mutableMapOf<View, BvhEntity>()

    fun add(view: View): BvhEntity {
        val entity = BvhEntity(this, view)
        val rect = view.getBounds(baseView)
        bvh.insertOrUpdate(rect, entity)
        entityMap[view] = entity
        return entity
    }

    fun getBvhEntity(view: View): BvhEntity? {
        return entityMap[view]
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
