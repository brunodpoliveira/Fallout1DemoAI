package lvls

import controls.PlayerControls
import grid.GridCreation
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.addTo

class JunkDemo {
    private val gridSizeX = 10
    private val gridSizeY = 10
    private val cellSize = 32.0

    fun setupLevel(container: Container) {
        val gridCreation = GridCreation(gridSizeX, gridSizeY, cellSize)
        gridCreation.addToContainer(container)

        // Create the player
        val player = SolidRect(cellSize, cellSize).addTo(container).apply {
            x = 0.0
            y = 0.0
        }

        // Initialize player controls
        PlayerControls(player, gridCreation.grid, cellSize)
    }
}
