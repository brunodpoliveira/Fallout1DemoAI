import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.view.*
import lvls.*
import manager.*

lateinit var collisionManager: CollisionManager

suspend fun main() = Korge {
    createBaseGrid()

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

fun Container.createBaseGrid() {
    for (x in 0 until 10) {
        for (y in 0 until 10) {
            solidRect(32.0, 32.0, Colors.DARKGRAY).xy(x * 32.0, y * 32.0)
        }
    }
}
