package combat

import enum.*
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
import korlibs.time.seconds
import kotlinx.coroutines.delay



class CombatManager(
    private val enemies: MutableList<LDTKEntityView>,
    private val playerInventory: Inventory,
    private val playerStats: EntityStats,
    private val playerStatsUI: PlayerStatsUI?,
    private val container: Container,
    private val scene: Scene,
    private val sceneView: View
) {
    private var targetingReticule: Image? = null
    private var currentTargetIndex: Int = 0
    private val entityStatsMap = mutableMapOf<String, EntityStats>()
    private var currentTurnIndex: Int = 0
    private var playerMovedSteps = 0
    private var playerActionTaken = false

    // Define o modo inicial como EXPLORATION
    private var gameMode: GameModeEnum = GameModeEnum.EXPLORATION

    suspend fun initialize() {
        // Carregar retículo de mira
        targetingReticule = container.image(texture = resourcesVfs["cross.png"].readBitmapSlice()) {
            visible = false
        }

        // Inicializar status para inimigos
        enemies.forEach { enemy ->
            val enemyId = enemy.entity.identifier + enemy.pos.toString()
            entityStatsMap[enemyId] = readEntityStats(enemy)
        }

        // Configurar eventos de tecla para ações do jogador (mover ou atirar)
        container.keys {
            down(Key.SPACE) { handlePlayerShoot() }
            down(Key.LEFT) { handlePlayerMove(-1, 0) }
            down(Key.RIGHT) { handlePlayerMove(1, 0) }
            down(Key.UP) { handlePlayerMove(0, -1) }
            down(Key.DOWN) { handlePlayerMove(0, 1) }
        }

        startTurn()
    }

    private suspend fun startTurn() {
        if (gameMode == GameModeEnum.COMBAT) {
            playerActionTaken = false
            playerMovedSteps = 0

            // Verifica de quem é o turno
            if (isPlayerTurn()) {
                println("Player's turn!")
            } else {
                println("Enemy's turn!")
                handleEnemyTurn()
            }
        }
    }

    private suspend fun endTurn() {
        if (gameMode == GameModeEnum.COMBAT) {
            currentTurnIndex = (currentTurnIndex + 1) % (enemies.size + 1)
            delay(5.seconds)
            startTurn()
        }
    }

    private suspend fun handleEnemyTurn() {
        if (gameMode == GameModeEnum.COMBAT) {
            val enemy = enemies[currentTurnIndex - 1] // O índice 0 é o jogador
            println("Enemy ${enemy.fieldsByName["Name"]?.value} is attacking!")

            if (Math.random() < 0.5) {
                println("Enemy attacked the player!")
                playerStats.hp -= 1
                playerStatsUI?.update(playerStats.hp, playerStats.ammo)
                checkPlayerHealth()
            } else {
                println("Enemy moved closer.")
            }

            endTurn()
        }
    }

    fun isPlayerTurn(): Boolean {
        return currentTurnIndex == 0
    }

    suspend fun handlePlayerShoot() {
        // Verificar munição antes de mudar para o modo combate
        if (!playerInventory.getItems().contains("GUN") || playerStats.ammo <= 0) {
            println("Cannot shoot! Ensure player has both gun and ammo.")
            return
        }

        // Se o modo é EXPLORATION e tem munição, inicia o modo COMBAT
        if (gameMode == GameModeEnum.EXPLORATION) {
            gameMode = GameModeEnum.COMBAT
            println("Entering combat mode!")
            currentTurnIndex = 0 // O jogador começa no primeiro turno
        }

        if (gameMode == GameModeEnum.COMBAT && !isPlayerTurn()) {
            println("It's not your turn!")
            return
        }

        if (playerActionTaken) {
            println("You have already taken an action this turn.")
            return
        }

        val target = enemies[currentTargetIndex]
        val enemyId = target.entity.identifier + target.pos.toString()
        val targetStats = entityStatsMap[enemyId] ?: readEntityStats(target)

        val ammoConsumed = playerInventory.useAmmo(playerStats) { newAmmo -> updateAmmoUI(newAmmo) }
        if (ammoConsumed) {
            val hitChance = 0.8 // 80% de chance de acerto
            if (Math.random() < hitChance) {
                targetStats.hp -= 20
                println("Hit! ${target.fieldsByName["Name"]?.value} HP: ${targetStats.hp}")
                if (targetStats.hp <= 0) {
                    target.removeFromParent()
                    enemies.remove(target)
                    entityStatsMap.remove(enemyId)
                    println("Target has been killed and removed from the scene")
                    if (enemies.isEmpty()) exitCombatMode()
                } else {
                    entityStatsMap[enemyId] = targetStats
                }
            } else {
                println("Missed!")
            }
            playerStatsUI?.update(playerStats.hp, playerStats.ammo)
            playerActionTaken = true
            endTurn()
        } else {
            println("Out of ammo!")
        }
    }

    private suspend fun handlePlayerMove(dx: Int, dy: Int) {
        if (gameMode == GameModeEnum.COMBAT && !isPlayerTurn()) {
            println("It's not your turn!")
            return
        }

        if (gameMode == GameModeEnum.COMBAT && (playerMovedSteps >= 2 || playerActionTaken)) {
            println("You can't move anymore this turn.")
            return
        }

        if (!canPlayerMove(dx, dy)) {
            println("Player can't move to this position.")
            return
        }

        playerStats.position = playerStats.position.copy(
            x = playerStats.position.x + dx,
            y = playerStats.position.y + dy
        )

        println("Player moved $dx, $dy")

        if (gameMode == GameModeEnum.COMBAT) {
            playerMovedSteps++
            if (playerMovedSteps >= 2) {
                playerActionTaken = true
                endTurn()
            }
        }
    }

    private fun canPlayerMove(dx: Int, dy: Int): Boolean {
        val newX = playerStats.position.x + dx
        val newY = playerStats.position.y + dy
        return newX in 0.0..sceneView.width && newY in 0.0..sceneView.height
    }

    private fun exitCombatMode() {
        gameMode = GameModeEnum.EXPLORATION
        println("Combat ended. Returning to exploration mode.")
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
