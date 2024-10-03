package ui

import korlibs.image.font.*
import korlibs.korge.input.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import maps.*
import player.*
import scenes.*
import utils.*

class UIManager(
    private val container: Container,
    val playerInventory: Inventory,
    val mapManager: MapManager,
    val levelView: LDTKLevelView,
    val playerManager: PlayerManager,
    private val defaultFont: Font
) {
    val pauseMenu = PauseMenu(mapManager, levelView)
    var playerStatsUI: PlayerStatsUI? = null

    fun initializeUI() {
        playerStatsUI = container.stage?.let { PlayerStatsUI(it, defaultFont) }
        playerStatsUI?.let { container.addChild(it) }
        updatePlayerStatsUI(playerManager.playerStats)

        addDebugReduceHealthButton()
    }

    fun updatePlayerStatsUI(playerStats: EntityStats) {
        playerStatsUI?.update(playerStats.hp, playerStats.ammo)
    }

    fun showPauseMenu() {
        if (JunkDemoScene.isPaused) {
            pauseMenu.resumeGame()
        } else {
            pauseGame()
            pauseMenu.show(container)
        }
    }

    private fun pauseGame() {
        JunkDemoScene.isPaused = true
        container.speed = 0.0
    }

    fun addDebugReduceHealthButton() {
        container.fixedSizeContainer(Size(200, 500), false) {
            position(700, 20)
            uiButton("Debug Reduce Health") {
                onClick {
                    playerManager.debugReduceHealth(20)
                }
            }
        }
    }

    fun updateInventoryUI() {
        container.removeChildren()
        container.fixedSizeContainer(Size(200, 500), false) {
            position(440, 150)
            for (item in playerInventory.getItems()) {
                uiButton(item) {
                    onClick {
                        if (item == "red_potion" && playerManager.playerStats.hp < 100) {
                            playerManager.consumePotion(item)
                            playerInventory.removeItem(item)
                            updateInventoryUI()
                        }
                    }
                }
            }
        }
    }
}
