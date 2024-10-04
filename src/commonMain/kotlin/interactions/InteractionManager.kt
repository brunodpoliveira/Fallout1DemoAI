package interactions

import ai.NPCBio
import combat.CombatManager
import dialog.DialogManager
import img.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.ldtk.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.*
import movement.PlayerMovementController
import raycasting.*
import utils.*

class InteractionManager(
    private val player: LDTKEntityView,
    private val raycaster: Raycaster,
    private val dialogManager: DialogManager,
    private val playerInventory: Inventory,
    private val playerStats: EntityStats,
    private val combatManager: CombatManager,
    private val gameWindow: GameWindow,
    private val openChestTile: TilesetRectangle,
    var playerMovementController: PlayerMovementController? = null
) {
    private var playerState: String = ""
    private var lastInteractiveView: View? = null

    fun update() {
        val interactiveView = getInteractiveView()
        lastInteractiveView?.colorMul = Colors.WHITE
        if (interactiveView != null) {
            interactiveView.colorMul = Colors["#ffbec3"]
            lastInteractiveView = interactiveView
        }
    }

    suspend fun handleAnyButton() {
        val view = getInteractiveView() ?: return
        val entityView = view as? LDTKEntityView ?: return
        val doBlock = entityView.fieldsByName["Items"] ?: return
        val items = doBlock.valueDyn.list.map { it.str }

        items.forEach { item ->
            playerInventory.addItem(item)
            if (item == "Ammo") {
                playerInventory.addAmmo(10, playerStats) { newAmmo ->
                    // Update ammo UI if necessary
                }
            }
        }

        // Replace the chest's view to show it as opened
        entityView.replaceView(
            Image(entityView.tileset!!.unextrudedTileSet!!.base.sliceWithSize(
                openChestTile.x, openChestTile.y, openChestTile.w, openChestTile.h
            )).also {
                it.smoothing = false
                it.anchor(entityView.anchor)
            }
        )

        // Display a message to the player
        gameWindow.alert("Found $items")
        println("Found items: $items")
    }

    fun handleWestButton() {
        val view = getInteractiveView() ?: return
        if (view is LDTKEntityView && view.fieldsByName["Name"] != null) {
            val npcName = view.fieldsByName["Name"]!!.valueString
            val npcFactions = mapOf(
                "Rayze" to "Crypts",
                "Baka" to "Fools",
                "Lex" to "Non-Gang",
                "Robot" to "Non-Gang"
            )
            val factionName = npcFactions[npcName] ?: "Unknown"
            val npcBio = npcName?.let { NPCBio.getBioForNPC(it) }
            if (npcBio != null) {
                dialogManager.showDialog(npcName, npcBio, factionName)
            }
        }
    }

    fun handleSouthButton() {
        val playerView = (player.view as ImageDataView2)
        playerView.animation = "attack"
        playerState = "attack"
        val target = combatManager.chooseTarget()
        if (target != null) {
            combatManager.handlePlayerShoot()
        }
    }

    fun handleNorthButton() {
        // uiManager.showPauseMenu()
    }

    private fun getInteractiveView(): View? {
        val playerDirection = playerMovementController?.playerDirection ?: Vector2D(0.0, 0.0)
        val results = raycaster.doRay(player.pos, playerDirection, "Collides") ?: return null
        if (results.point.distanceTo(player.pos) >= 16f) return null
        return results.view
    }
}
