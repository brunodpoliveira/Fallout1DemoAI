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
    private val defaultFont: Font,
    getPlayerPosition: PointInt
) {
    private val pauseMenu = PauseMenu(mapManager, levelView, getPlayerPosition, this)
    var playerStatsUI: PlayerStatsUI? = null
    private var inventoryContainer: Container? = null

    fun initializeUI() {
        playerStatsUI = container.stage?.let { PlayerStatsUI(it, defaultFont) }
        playerStatsUI?.let { container.addChild(it) }
        updatePlayerStatsUI(playerManager.playerStats)

        addDebugReduceHealthButton()
    }

    private fun updatePlayerStatsUI(playerStats: EntityStats) {
        playerStatsUI?.update(playerStats.hp, playerStats.ammo)
    }

    fun showPauseMenu() {
        if (JunkDemoScene.isPaused) {
            pauseMenu.resumeGame()
        } else {
            pauseMenu.show(container)
        }
    }

    fun updateInventoryUI() {
        clearInventoryUI()

        inventoryContainer = container.fixedSizeContainer(Size(200, 500), false) {
            position(640, 300)

            var yOffset = 0.0
            for (item in playerInventory.getItems()) {
                uiButton(item) {
                    position(0, yOffset)
                    onClick {
                        if (item == "Red_potion" && playerManager.playerStats.hp < 100) {
                            playerManager.consumePotion(item)
                            playerInventory.removeItem(item)
                            updateInventoryUI()
                            updatePlayerStatsUI()
                        }
                    }
                }
                yOffset += 40
            }
        }
    }

    fun clearInventoryUI() {
        inventoryContainer?.removeFromParent()
        inventoryContainer = null
    }

    fun updatePlayerStatsUI() {
        playerStatsUI?.update(playerManager.playerStats.hp, playerManager.playerStats.ammo)
    }

    private fun addDebugReduceHealthButton() {
        container.fixedSizeContainer(Size(200, 500), false) {
            position(700, 20)
            uiButton("Debug Reduce Health") {
                onClick {
                    playerManager.debugReduceHealth(20)
                }
            }
        }
    }
}
