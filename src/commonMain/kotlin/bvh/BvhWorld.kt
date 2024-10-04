package bvh

import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.ds.*

class BvhWorld(private val baseView: View) {
    val bvh = BVH2D<BvhEntity>()
    private val entityMap = mutableMapOf<View, BvhEntity>()

    fun add(view: View): BvhEntity {
        val entity = BvhEntity(this, view)
        updateEntityBounds(entity)
        entityMap[view] = entity
        return entity
    }

    fun getBvhEntity(view: View): BvhEntity? {
        return entityMap[view]
    }

    private fun remove(view: View) {
        val entity = entityMap.remove(view)
        entity?.let { bvh.remove(it) }
    }

    fun updateEntityBounds(entity: BvhEntity) {
        val view = entity.view
        val bounds = view.getBounds(baseView)

        // Adjust the collision box to be more precise
        val adjustedBounds = Rectangle(
            bounds.x,
            bounds.y + bounds.height * 0.75, // Adjust this value to fit your sprites
            bounds.width,
            bounds.height * 0.25 // Adjust this value to fit your sprites
        )

        bvh.remove(entity)
        bvh.insertOrUpdate(adjustedBounds, entity)
    }

    operator fun plusAssign(view: View) {
        add(view)
    }

    operator fun minusAssign(view: View) {
        remove(view)
    }
}
