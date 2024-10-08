package combat

import korlibs.event.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import ui.*
import utils.*
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.set

class CombatManager(
    private val enemies: MutableList<LDTKEntityView>,
    private val playerInventory: Inventory,
    private val playerStats: EntityStats,
    private val playerStatsUI: PlayerStatsUI?,
    private val container: Container
) {
    private var targetingReticule: Image? = null
    private var currentTargetIndex: Int = 0
    private val entityStatsMap = mutableMapOf<String, EntityStats>()

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
        // Initialize stats for enemies
        enemies.forEach { enemy ->
            val enemyId = enemy.entity.identifier + enemy.pos.toString()
            entityStatsMap[enemyId] = readEntityStats(enemy)
        }

        // Setup key events for selecting targets
        // TODO: Adjust controls when in combat vs "on foot"
        container.keys {
            down(Key.LEFT) { selectPreviousTarget() }
            down(Key.RIGHT) { selectNextTarget() }
        }
    }

    private fun updateTargetReticule() {
        if (enemies.isNotEmpty()) {
            val scaledPositions = scaleEntityPositions(enemies.map { PointInt(it.x.toInt(), it.y.toInt()) })
            val targetPosition = scaledPositions[currentTargetIndex]
            targetingReticule?.visible = true
            targetingReticule?.xy(targetPosition.x.toDouble(), targetPosition.y.toDouble())
        } else {
            targetingReticule?.visible = false
        }
    }

    private fun selectNextTarget() {
        if (enemies.isNotEmpty()) {
            currentTargetIndex = (currentTargetIndex + 1) % enemies.size
            updateTargetReticule()
        }
    }

    private fun selectPreviousTarget() {
        if (enemies.isNotEmpty()) {
            currentTargetIndex = (currentTargetIndex - 1 + enemies.size) % enemies.size
            updateTargetReticule()
        }
    }

    private fun scaleEntityPositions(entities: List<PointInt>): List<PointInt> {
        val mapScale = 2
        return entities.map { point ->
            val x = (point.x * mapScale) - 10
            val y = (point.y * mapScale) - 45
            PointInt(x, y)
        }
    }


    fun handlePlayerShoot() {
        if (!playerInventory.getItems().contains("Gun") || playerStats.ammo <= 0) {
            println("Cannot shoot! Ensure player has both gun and ammo.")
            return
        }
        if (enemies.isEmpty()) {
            println("No targets available.")
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
                println("Hit! ${target.fieldsByName["Name"]?.value} HP: ${targetStats.hp}")
                if (targetStats.hp <= 0) {
                    target.removeFromParent()
                    enemies.remove(target)
                    entityStatsMap.remove(enemyId)
                    println("Target has been killed and removed from the scene")
                } else {
                    entityStatsMap[enemyId] = targetStats
                    // Indicate damage via visual effect
                    // ...
                }
            } else {
                println("Missed!")
            }
            playerStatsUI?.update(playerStats.hp, playerStats.ammo)
        } else {
            println("Out of ammo!")
        }
    }

    fun updateAmmoUI(newAmmo: Int) {
        playerStatsUI?.update(playerStats.hp, newAmmo)
    }

    fun chooseTarget(): LDTKEntityView? {
        if (enemies.isNotEmpty()) {
            val target = enemies.first()
            targetingReticule?.visible = true
            targetingReticule?.xy(target.x, target.y)
            return target
        } else {
            targetingReticule?.visible = false
            return null
        }
    }

    // Optionally, add other combat-related methods here
}
