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
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.virtualcontroller.*
import korlibs.math.geom.Point
import scenes.*
import ui.*
import utils.*
import korlibs.time.seconds
import kotlinx.coroutines.*
import kotlin.math.*

class CombatManager(
    private val enemies: MutableList<LDTKEntityView>,
    private val playerInventory: Inventory,
    private val playerStats: EntityStats,
    private val player: LDTKEntityView,
    private val playerStatsUI: PlayerStatsUI?,
    private val container: Container,
    private val scene: Scene,
    private val sceneView: View,
    private val mapScale: Int = 2,
) {
    private var targetingReticule: Image? = null
    private var currentTargetIndex: Int = 0
    private val entityStatsMap = mutableMapOf<String, EntityStats>()
    private var currentTurnIndex: Int = 0
    private var playerTurnJob: Job? = null
    private var playerActionTaken = false
    private var playerMoved = false
    private var gameMode: GameModeEnum = GameModeEnum.EXPLORATION

    suspend fun initialize() {
        targetingReticule = container.image(texture = resourcesVfs["cross.png"].readBitmapSlice()) {
            visible = true
        }

        container.keys {
            down(Key.LEFT) { handlePlayerMove() }
            down(Key.RIGHT) { handlePlayerMove() }
            down(Key.UP) { handlePlayerMove() }
            down(Key.DOWN) { handlePlayerMove() }
        }
        startTurn()
    }


    private fun scaleEntityPosition(point: Point): Point {
        return Point((point.x * mapScale) - 10, (point.y * mapScale) - 45)
    }

    private fun updateTargetingReticule() {
        val closestEnemy = findClosestEnemy()
        closestEnemy?.let {
            currentTargetIndex = enemies.indexOf(it)
            val scaledPosition = scaleEntityPosition(Point(it.x, it.y))
            targetingReticule?.xy(scaledPosition.x, scaledPosition.y)
        } ?: run {
            targetingReticule?.visible = false
        }
    }

    private suspend fun startTurn() {
        if (gameMode == GameModeEnum.COMBAT) {
            playerActionTaken = false
            if (isPlayerTurn()) {
                delay(3.seconds)
                endTurn()
            } else {
                handleEnemyTurn()
            }
        }
    }

    private suspend fun endTurn() {
        playerTurnJob?.cancel()
        playerMoved = false
        if (gameMode == GameModeEnum.COMBAT) {
            currentTurnIndex = (currentTurnIndex + 1) % (enemies.size + 1)
            delay(5.seconds)
            startTurn()
        }
    }
    //@TODO: Implement enemy AI
    private suspend fun handleEnemyTurn() {
        if (gameMode == GameModeEnum.COMBAT) {
            val enemy = enemies[currentTurnIndex - 1]
            if (Math.random() < 0.5) {
                playerStats.hp -= 1
                playerStatsUI?.update(playerStats.hp, playerStats.ammo)
                checkPlayerHealth()
            }
            endTurn()
        }
    }

    fun isPlayerTurn(): Boolean = currentTurnIndex == 0

    suspend fun handlePlayerShoot() {
        if (!isPlayerTurn() || playerActionTaken || (gameMode == GameModeEnum.COMBAT && playerMoved)) return
        if (!playerInventory.getItems().contains("Gun") || playerStats.ammo <= 0) return
        if (!playerInventory.useAmmo(playerStats) { updateAmmoUI(it) }) return

        playerActionTaken = true
        playerTurnJob?.cancel()

        val target = enemies[currentTargetIndex]
        val enemyId = target.entity.identifier + target.pos.toString()
        val targetStats = entityStatsMap[enemyId] ?: readEntityStats(target)

        if (gameMode == GameModeEnum.EXPLORATION) {
            gameMode = GameModeEnum.COMBAT
            currentTurnIndex = 0
        }

        targetStats.hp -= 20

        if (targetStats.hp <= 0) {
            target.removeFromParent()
            enemies.remove(target)
            entityStatsMap.remove(enemyId)
            updateTargetingReticule()
            if (enemies.isEmpty()) exitCombatMode()
        } else {
            entityStatsMap[enemyId] = targetStats
        }

        playerStatsUI?.update(playerStats.hp, playerStats.ammo)
        endTurn()
    }

    private fun handlePlayerMove() {
        if (gameMode == GameModeEnum.COMBAT && !isPlayerTurn()) return
        val currentPosition = player.pos
        println(currentPosition)
        val newPosition = Point(currentPosition.x , currentPosition.y )
        if (!canPlayerMove(newPosition)) return

        playerStats.position = newPosition


        updateTargetingReticule()

        if (gameMode == GameModeEnum.COMBAT) {
            playerMoved = true
        }
    }

    private fun findClosestEnemy(): LDTKEntityView? {
        var closestEnemy: LDTKEntityView? = null
        var minDistance = Double.MAX_VALUE

        for (enemy in enemies) {
            val enemyPosition = Point(enemy.x, enemy.y)
            val playerPosition = Point(playerStats.position.x, playerStats.position.y)
            val distance = calculateDistance(playerPosition, enemyPosition)

            if (distance < minDistance) {
                minDistance = distance
                closestEnemy = enemy
            }
        }

        return closestEnemy
    }

    private fun calculateDistance(pos1: Point, pos2: Point): Double {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun canPlayerMove(newPosition: Point): Boolean {
        return newPosition.x in 0.0..sceneView.width && newPosition.y in 0.0..sceneView.height
    }

    private fun exitCombatMode() {
        gameMode = GameModeEnum.EXPLORATION
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
        scene.launchImmediately {
            scene.sceneContainer.changeTo<GameOverScene>("GameOver", scene)
        }
    }
}
