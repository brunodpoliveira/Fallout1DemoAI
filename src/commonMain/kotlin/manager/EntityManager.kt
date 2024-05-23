package manager

import controls.*
import korlibs.image.color.*
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
    lateinit var boundingBox: SolidRect
    private var direction = Vector2D(0.0, 0.0)

    init {
        addUpdater {
            updatePlayerPosition()
            if (this::boundingBox.isInitialized) updateBoundingBox()
        }
        GlobalScope.launch {
            val bitmap = resourcesVfs["korge.png"].readBitmap()
            sprite = image(bitmap) {
                position(initialX, initialY)
                scale = 0.1
            }
            addChild(sprite)
            setupBoundingBox()
            PlayerControls(this@Player, grid, npcs, cellSize)
            println("Initialized Player with Controls")
        }
    }

    private fun setupBoundingBox() {
        boundingBox = solidRect(width, height, Colors.RED.withAd(0.3)).also {
            it.position(x, y)
        }
        addChild(boundingBox)
    }

    private fun updatePlayerPosition() {
        x += direction.x
        y += direction.y

        if (x < 0) x = 0.0
        if (y < 0) y = 0.0
        if (x > (grid.size - 1) * cellSize) x = (grid.size - 1) * cellSize
        if (y > (grid[0].size - 1) * cellSize) y = (grid[0].size - 1) * cellSize
    }

    private fun updateBoundingBox() {
        sprite.position(boundingBox.x, boundingBox.y)
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
    //todo var sprite
    //TODO private lateinit var inventory: Inventory
    //TODO private lateinit var stats: Stats

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
            solidRect(width, height, Colors.RED.withAd(0.3)).also {
                it.position(initialX, initialY)
            }
        }
    }

    override fun onCollision(other: Entity) {
        if (other is Player) {
            // Handle collision with player (e.g., start dialogue)
        }
    }
}
