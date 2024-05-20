package lvls

import ai.*
import korlibs.image.color.*
import korlibs.korge.view.*
import manager.*

class JunkDemo : Container() {

    private lateinit var player: Player
    private lateinit var rayze: NPC
    private lateinit var baka: NPC
    private val cellSize = 32.0
    private val grid: Array<Array<SolidRect?>> = Array(10) { arrayOfNulls<SolidRect?>(10) }
    private val npcs = mutableListOf<NPC>()

    init {
        setupLevel()
    }

    private fun setupLevel() {
        createGrid()

        rayze = NPC("Rayze", 12.0, 12.0, NPCBio.rayzeBio)
        addChild(rayze)
        npcs.add(rayze)

        baka = NPC("Baka", 256.0, 256.0, NPCBio.bakaBio)
        addChild(baka)
        npcs.add(baka)

        player = Player(0.0, 0.0, grid, npcs, cellSize)
        addChild(player)
    }

    private fun createGrid() {
        for (x in 0 until 10) {
            for (y in 0 until 10) {
                val isObstacle = (x + y) % 3 == 0
                if (isObstacle) {
                    grid[x][y] = solidRect(cellSize, cellSize, Colors.LIGHTGRAY).xy(x * cellSize, y * cellSize)
                }
            }
        }
    }
}
