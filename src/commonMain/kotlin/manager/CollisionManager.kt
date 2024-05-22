package manager

class CollisionManager {
    private val entities = mutableListOf<Entity>()

    fun addEntity(entity: Entity) {
        entities.add(entity)
    }

    fun removeEntity(entity: Entity) {
        entities.remove(entity)
    }

    fun checkCollisions() {
        for (i in 0 until entities.size) {
            for (j in i + 1 until entities.size) {
                if (entities[i].bounds.intersects(entities[j].bounds)) {
                    entities[i].onCollision(entities[j])
                    entities[j].onCollision(entities[i])
                }
            }
        }
    }
}
