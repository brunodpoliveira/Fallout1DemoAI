import korlibs.korge.*
import korlibs.korge.view.*
import lvls.*
import manager.*

lateinit var collisionManager: CollisionManager

suspend fun main() = Korge {
    val junkDemo = JunkDemo()
    addChild(junkDemo)

    collisionManager = CollisionManager()
    junkDemo.children.forEach { entity ->
        if (entity is Entity) {
            collisionManager.addEntity(entity)
        }
    }

    addUpdater {
        collisionManager.checkCollisions()
    }
}
