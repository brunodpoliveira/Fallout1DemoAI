package lvls

import ai.*
import grid.*
import korlibs.korge.view.*
import kotlinx.coroutines.*
import manager.*

class JunkDemo : Container() {
    private lateinit var player: Player
    private lateinit var rayze: NPC
    private lateinit var baka: NPC
    private lateinit var grid: Array<Array<SolidRect?>>
    private val npcs = mutableListOf<NPC>()

    init {
        setupLevel()
    }

    private fun setupLevel() {
        val gridCreation = GridCreation(256, 256, "junkdemo")
        gridCreation.addToContainer(this)
        grid = gridCreation.grid

        rayze = NPC("Rayze", 128.0, 64.0, NPCBio.rayzeBio)
        addChild(rayze)
        npcs.add(rayze)

        baka = NPC("Baka", 256.0, 64.0, NPCBio.bakaBio)
        addChild(baka)
        npcs.add(baka)

        player = Player(32.0, 32.0, grid, npcs)
        addChild(player)

    }
}
