package lvls

import controls.*
import grid.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.view.*

class JunkDemo {
    private val gridSizeX = 10
    private val gridSizeY = 10
    private val cellSize = 32.0

    suspend fun setupLevel(container: Container) {
        val gridCreation = GridCreation(gridSizeX, gridSizeY, cellSize)
        gridCreation.addToContainer(container)

        // Load the korge.png sprite
        val playerBitmap = resourcesVfs["korge.png"].readBitmap()
        val playerSprite = Image(playerBitmap).apply {
            scale = cellSize / playerBitmap.width
        }.addTo(container)

        playerSprite.xy(0.0, 0.0)

        PlayerControls(playerSprite, gridCreation.grid, cellSize)
    }
}
