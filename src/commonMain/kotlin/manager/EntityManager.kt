package manager

import controls.*
import korlibs.image.format.*
import korlibs.io.async.launch
import korlibs.io.file.std.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

abstract class Entity(initialX: Double, initialY: Double, initialWidth: Double, initialHeight: Double) : Container() {
    init {
        this.x = initialX
        this.y = initialY
        this.width = initialWidth
        this.height = initialHeight
    }

    val bounds: Rectangle
        get() = Rectangle(x, y, width, height)

    abstract fun onCollision(other: Entity)
}

class Player(
    initialX: Double,
    initialY: Double,
    private val grid: Array<Array<SolidRect?>>,
    private val npcs: List<NPC>,
    private val cellSize: Double
) : Entity(initialX, initialY, 32.0, 32.0) {
    private lateinit var sprite: Image
    private lateinit var controls: PlayerControls
    //TODO private lateinit var inventory: Inventory
    //TODO private lateinit var stats: Stats

    init {
        GlobalScope.launch {
            val bitmap = resourcesVfs["korge.png"].readBitmap()
            sprite = image(bitmap) {
                position(initialX, initialY)
                scale = 0.1
            }
            addChild(sprite)
            controls = PlayerControls(sprite, grid, npcs, cellSize, this@Player)
            println("Initialized Player with Controls")
        }
    }

    override fun onCollision(other: Entity) {
        if (other is NPC) {
            // Handle collision with NPC (e.g., start dialogue)
        }
    }
}

class NPC(
    val npcName: String,
    initialX: Double,
    initialY: Double,
    val bio: String
) : Entity(initialX, initialY, 32.0, 32.0) {
    init {
        GlobalScope.launch {
            val bitmap = resourcesVfs["korge.png"].readBitmap()
            val sprite = image(bitmap) {
                position(initialX, initialY)
                scale = 0.1
            }
            addChild(sprite)
            println("Initialized $npcName with bio: $bio")
        }
    }

    override fun onCollision(other: Entity) {
        if (other is Player) {
            // Handle collision with player (e.g., start dialogue)
        }
    }
}
