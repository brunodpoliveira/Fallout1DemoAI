package interactions

import agent.core.*
import agent.system.*
import combat.CombatManager
import dialog.DialogManager
import img.ImageDataView2
import korlibs.image.bitmap.*
import korlibs.korge.ldtk.view.LDTKEntityView
import korlibs.korge.view.*
import korlibs.math.geom.Vector2D
import korlibs.render.*
import movement.PlayerMovementController
import raycasting.Raycaster
import ui.UIManager
import utils.*
import korlibs.korge.ldtk.*
import korlibs.image.color.*


class PlayerInteractionHandler(
    private val player: LDTKEntityView,
    private val raycaster: Raycaster,
    private val dialogManager: DialogManager,
    private val playerInventory: Inventory,
    private val playerStats: EntityStats,
    private val combatManager: CombatManager,
    private val agentInteractionManager: AgentInteractionManager,
    private val gameWindow: GameWindow,
    private val openChestTile: TilesetRectangle,
    private val uiManager: UIManager,
    var playerMovementController: PlayerMovementController?,
    private val agentId: String
) {
    private var lastInteractiveView: View? = null

    suspend fun handleAnyButton() {
        val hasItem = getItem()
        if (!hasItem) {
            combatManager.handlePlayerShoot()
        }
    }

    suspend fun handleWestButton() {
        val view = getInteractiveView() ?: return
        if (view is LDTKEntityView) {
            val targetNpcName = view.fieldsByName["Name"]?.valueString
            if (targetNpcName != null) {
                val success = agentInteractionManager.initiateInteraction(agentId, targetNpcName)

                if (!success) {
                    Logger.debug("Failed to initiate interaction with $targetNpcName")
                    gameWindow.alert("Cannot interact with $targetNpcName at this time")
                }
            }
        }
    }

    suspend fun handleSouthButton() {
        val playerView = (player.view as ImageDataView2)
        playerView.animation = "attack"
        // Create agent combat action if needed
        agentInteractionManager.processAgentAction(
            agentInteractionManager.agentManager?.getAgent(agentId)!!,
            AgentAction.StartDialog(targetId = "", opening = "Combat initiated")
        )
    }

    fun handleNorthButton() {
        Logger.debug("PlayerInteractionHandler: North button pressed, showing pause menu")
        uiManager.showPauseMenu()
    }

    private suspend fun getItem(): Boolean {
        val view = getInteractiveView() ?: return false
        val entityView = view as? LDTKEntityView ?: return false
        val doBlock = entityView.fieldsByName["Items"] ?: return false
        val items = doBlock.valueDyn.list.map { it.str }

        if (items.isEmpty()) return false

        items.forEach { item ->
            playerInventory.addItem(item)
            if (item == "Ammo") {
                playerInventory.addAmmo(10, playerStats) { newAmmo ->
                    combatManager.updateAmmoUI(newAmmo)
                }
            }
            if (item == "Gun") {
                playerInventory.addItem("Gun")
            }
        }

        entityView.replaceView(
            Image(entityView.tileset?.unextrudedTileSet?.base?.sliceWithSize(
                openChestTile.x, openChestTile.y, openChestTile.w, openChestTile.h
            ) ?: return false).also {
                it.smoothing = false
                it.anchor(entityView.anchor)
            }
        )

        gameWindow.alert("Found $items")
        Logger.debug("Found items: $items")
        return true
    }

    private fun getInteractiveView(): View? {
        val playerDirection = playerMovementController?.playerDirection ?: Vector2D(0.0, 0.0)
        val results = raycaster.doRay(player.pos, playerDirection, "Collides") ?: return null
        if (results.point.distanceTo(player.pos) >= 16f) return null

        val view = results.view
        if (view is LDTKEntityView) {
            Logger.debug("Found interactive entity: ${view.fieldsByName["Name"]?.valueString ?: "unnamed"} (${view.entity.identifier})")
        }
        return view
    }

    fun update() {
        val interactiveView = getInteractiveView()
        lastInteractiveView?.colorMul = Colors.WHITE
        if (interactiveView != null) {
            interactiveView.colorMul = Colors["#ffbec3"]
            lastInteractiveView = interactiveView
        }
    }
}
