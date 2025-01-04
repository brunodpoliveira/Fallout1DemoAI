package combat

import enum.*
import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlinx.coroutines.*
import raycasting.*
import scenes.*
import ui.*
import utils.*

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
    private val raycaster: Raycaster
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
        startGame()
    }

    fun startGame(){
        updateTargetingReticule()
        container.keys {
            down(Key.LEFT) { handlePlayerMove() }
            down(Key.RIGHT) { handlePlayerMove() }
            down(Key.UP) { handlePlayerMove() }
            down(Key.DOWN) { handlePlayerMove() }
        }
    }



    private suspend fun startTurn() {
        if (gameMode == GameModeEnum.COMBAT) {
            playerActionTaken = false
            if (isPlayerTurn()) {
                showTurnMessage("your turn!")
                delay(3.seconds)
                endTurn()
            } else {
                handleAgentTurn()
            }
        }
    }

    private fun showTurnMessage(message: String) {
        val turnMessage = container.text(message, textSize = 28.0, color = Colors.YELLOW) {
            centerOnStage()
            alpha = 0.0
        }

        scene.launchImmediately {
            turnMessage.tween(
                turnMessage::alpha[1.0],
                time = 0.5.seconds,
                easing = Easing.EASE_IN
            )

            turnMessage.tween(
                turnMessage::alpha[0.0],
                time = 0.5.seconds,
                easing = Easing.EASE_IN_OUT
            )

            turnMessage.removeFromParent()
        }
    }

    private suspend fun endTurn() {
        playerTurnJob?.cancel()
        playerMoved = false
        if (gameMode == GameModeEnum.COMBAT) {
            currentTurnIndex = (currentTurnIndex + 1) % (enemies.size + 1)
            delay(3.seconds)
            startTurn()
        }
    }

    //@TODO: Implement agent AI
    private suspend fun handleAgentTurn() {
        if (gameMode == GameModeEnum.COMBAT) {
            val agent = enemies[currentTurnIndex - 1]
            showTurnMessage(getPlayerName(agent)+"'s turn!")
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
        val agentId = target.entity.identifier + target.pos.toString()
        val targetStats = entityStatsMap[agentId] ?: readEntityStats(target)

        if (gameMode == GameModeEnum.EXPLORATION) {
            startCombatMode()
            currentTurnIndex = 0
        }

        val damage = 100
        targetStats.hp -= damage
        showDamageText("-$damage", Point(target.x, target.y))

        if (targetStats.hp <= 0) {
            removePlayerFromGame(target, agentId)
            updateTargetingReticule()
            if (enemies.isEmpty()) exitCombatMode()
        } else {
            entityStatsMap[agentId] = targetStats
        }

        playerStatsUI?.update(playerStats.hp, playerStats.ammo)
        endTurn()
    }

    private fun getPlayerName(agent: LDTKEntityView):String{
        return agent.fieldsByName["Name"]?.valueString ?: "Unknown"
    }

    private fun removePlayerFromGame(target: LDTKEntityView, agentId: String) {
        target.removeFromParent()
        enemies.remove(target)
        entityStatsMap.remove(agentId)
    }

    private fun handlePlayerMove() {
        if (gameMode == GameModeEnum.COMBAT && !isPlayerTurn()) return

        val currentPosition = player.pos
        val newPosition = Point(currentPosition.x, currentPosition.y)
        if (!canPlayerMove(newPosition)) return

        playerStats.position = newPosition
        updateTargetingReticule()

        if (gameMode == GameModeEnum.COMBAT) {
            playerMoved = true
        }
    }

    private suspend fun startCombatMode() {
        gameMode = GameModeEnum.COMBAT
        showCombatMessage("The player started a war.\nCombat mode started!")
        startTurn()
    }

    private fun showCombatMessage(message: String) {
        val messageText = container.text(message, textSize = 26.0, color = Colors.WHITE) {
            centerOnStage()
            alpha = 0.0
        }

        scene.launchImmediately {
            messageText.tween(
                messageText::alpha[1.0],
                time = 0.5.seconds,
                easing = Easing.EASE_IN_OUT
            )

            delay(1.5.seconds)

            messageText.tween(
                messageText::alpha[0.0],
                time = 0.5.seconds,
                easing = Easing.EASE_IN_OUT
            )

            messageText.removeFromParent()
        }
    }

    private fun findClosestAgent(): LDTKEntityView? {
        var closestAgent: LDTKEntityView? = null
        var minDistance = Double.MAX_VALUE

        for (agent in enemies) {
            val agentPosition = Point(agent.x, agent.y)
            val playerPosition = Point(playerStats.position.x, playerStats.position.y)

            if (raycaster.isInShadow(playerPosition, agentPosition)) continue

            val distance = playerPosition.distanceTo(agentPosition)
            if (distance < minDistance) {
                minDistance = distance
                closestAgent = agent
            }
        }

        return closestAgent
    }

    fun updateTargetingReticule() {
        val closestAgent = findClosestAgent()
        closestAgent?.let {
            currentTargetIndex = enemies.indexOf(it)
            val scaledPosition = scaleEntityPosition(Point(it.x, it.y))
            targetingReticule?.apply {
                xy(scaledPosition.x, scaledPosition.y)
                visible = true
            }
        } ?: run {
            targetingReticule?.visible = false
        }
    }
    private fun showDamageText(text: String, position: Point) {
        val scaledPosition = scaleEntityPosition(position)

        val damageText = container.text(text, textSize = 24.0, color = Colors.RED) {
            xy(scaledPosition.x , scaledPosition.y - 70)
        }

        scene.launchImmediately {
            damageText.tween(
                damageText::alpha[0.0],
                damageText::y[damageText.y],
                time = 1.seconds,
                easing = Easing.EASE_IN_OUT
            )
            damageText.removeFromParent()
        }
    }


    private fun scaleEntityPosition(point: Point): Point {
        return Point((point.x * mapScale) - 10, (point.y * mapScale) - 45)
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
