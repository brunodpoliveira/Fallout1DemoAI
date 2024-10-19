package combat

import korlibs.event.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import scenes.*
import ui.*
import utils.*

class CombatManager(
    private val enemies: MutableList<LDTKEntityView>,
    private val playerInventory: Inventory,
    private val playerStats: EntityStats,
    private val playerStatsUI: PlayerStatsUI?,
    private val container: Container,
    private val scene: Scene
) {
    private var targetingReticule: Image? = null
    private var currentTargetIndex: Int = 0
    private val entityStatsMap = mutableMapOf<String, EntityStats>()
    private var currentTurnIndex: Int = 0
    private var playerMovedSteps = 0
    private var playerActionTaken = false

    suspend fun initialize() {
        // Load targeting reticule
        targetingReticule = container.image(texture = resourcesVfs["cross.png"].readBitmapSlice()) {
            visible = false
        }

        // Initialize stats for enemies
        enemies.forEach { enemy ->
            val enemyId = enemy.entity.identifier + enemy.pos.toString()
            entityStatsMap[enemyId] = readEntityStats(enemy)
        }

        // Setup key events for player actions (move or shoot)
        container.keys {
            down(Key.SPACE) { handlePlayerShoot() }
            down(Key.LEFT) { handlePlayerMove(-1, 0) }
            down(Key.RIGHT) { handlePlayerMove(1, 0) }
            down(Key.UP) { handlePlayerMove(0, -1) }
            down(Key.DOWN) { handlePlayerMove(0, 1) }
        }

        startTurn()
    }

    private fun startTurn() {
        playerActionTaken = false
        playerMovedSteps = 0
        if (currentTurnIndex == 0) {
            println("Player's turn!")
        } else {
            println("Enemy's turn!")
            handleEnemyTurn()
        }
    }

    private fun endTurn() {
        currentTurnIndex = (currentTurnIndex + 1) % (enemies.size + 1) // Alterna entre jogador e inimigos
        startTurn()
    }

    private fun handleEnemyTurn() {
        // Lógica simples de movimentação ou ataque de inimigos
        val enemy = enemies[currentTurnIndex - 1] // O índice 0 é o jogador
        println("Enemy ${enemy.fieldsByName["Name"]?.value} is attacking!")

        // Simulação de ataque ou movimento
        if (Math.random() < 0.5) {
            println("Enemy attacked the player!")
            playerStats.hp -= 1
            playerStatsUI?.update(playerStats.hp, playerStats.ammo)
            checkPlayerHealth();
        } else {
            println("Enemy moved closer.")
        }

        endTurn()
    }

    fun handlePlayerShoot() {
        if (playerActionTaken) {
            println("You have already taken an action this turn.")
            return
        }

        if (!playerInventory.getItems().contains("GUN") || playerStats.ammo <= 0) {
            Logger.warn("Cannot shoot! Ensure player has both gun and ammo.")
            return
        }

        if (enemies.isEmpty()) {
            Logger.debug("No targets available.")
            return
        }

        val target = enemies[currentTargetIndex]
        val enemyId = target.entity.identifier + target.pos.toString()
        val targetStats = entityStatsMap[enemyId] ?: readEntityStats(target)

        // Ammo consumption
        val ammoConsumed = playerInventory.useAmmo(playerStats) { newAmmo -> updateAmmoUI(newAmmo) }
        if (ammoConsumed) {
            val hitChance = 0.8 // 80% hit chance
            if (Math.random() < hitChance) {
                targetStats.hp -= 20
                Logger.debug("Hit! ${target.fieldsByName["Name"]?.value} HP: ${targetStats.hp}")
                if (targetStats.hp <= 0) {
                    target.removeFromParent()
                    enemies.remove(target)
                    entityStatsMap.remove(enemyId)
                    Logger.debug("Target has been killed and removed from the scene")
                } else {
                    entityStatsMap[enemyId] = targetStats
                }
            } else {
                Logger.debug("Missed!")
            }
            playerStatsUI?.update(playerStats.hp, playerStats.ammo)
            playerActionTaken = true
            endTurn()
        } else {
            Logger.debug("Out of ammo!")
        }
    }

    private fun handlePlayerMove(dx: Int, dy: Int) {
        if (playerMovedSteps >= 2 || playerActionTaken) {
            println("You can't move anymore this turn.")
            return
        }

        // Movimenta o jogador, ajustando sua posição
        println("Player moved $dx, $dy")
        playerMovedSteps++

        if (playerMovedSteps >= 2) {
            playerActionTaken = true
            endTurn()
        }
    }

    fun updateAmmoUI(newAmmo: Int) {
        playerStatsUI?.update(playerStats.hp, newAmmo)
    }

    private fun checkPlayerHealth() {
        if (playerStats.hp <= 0) {
            triggerGameOver()
        }
    }

    private fun triggerGameOver() {
        println("Game Over! Player has been defeated.")
        scene.launchImmediately {
            scene.sceneContainer.changeTo<GameOverScene>("GameOver", scene)
        }
    }
}
