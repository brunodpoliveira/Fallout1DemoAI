package player

import korlibs.io.async.*
import korlibs.korge.scene.*
import scenes.*
import ui.*
import utils.*

class PlayerManager(
    private val scene: Scene,
    val playerInventory: Inventory,
    val playerStats: EntityStats,
    var playerStatsUI: PlayerStatsUI?,
) {
    fun consumePotion(potion: String) {
        playerInventory.consumePotion(potion, playerStats) { newHp ->
            updatePlayerHealthUI(newHp)
        }
    }

    fun debugReduceHealth(damage: Int) {
        playerStats.hp -= damage
        if (playerStats.hp <= 0) {
            triggerGameOver()
        } else {
            updatePlayerHealthUI(playerStats.hp)
        }
    }

    private fun updatePlayerHealthUI(newHp: Int) {
        playerStatsUI?.update(playerHp = newHp, playerAmmo = playerStats.ammo)
    }

    private fun triggerGameOver() {
        scene.launchImmediately {
            scene.sceneContainer.changeTo<GameOverScene>("GameOver", scene)
        }
    }
}
