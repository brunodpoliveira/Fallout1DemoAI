package interactions

import ai.*
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
import ui.*
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
    private val uiManager: UIManager,
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

    suspend fun handleAnyButton(){
        val item = getItem();
        if(!item){
            combatManager.handlePlayerShoot();
        }

    }


    suspend fun getItem(): Boolean {
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

    fun handleWestButton() {
        val view = getInteractiveView() ?: return
        if (view is LDTKEntityView && view.fieldsByName["Name"] != null) {
            val npcName = view.fieldsByName["Name"]!!.valueString
            val factionName = npcName?.let { Director.getNPCFaction(it) }
            val npcBio = npcName?.let { NPCBio.getBioForNPC(it) }
            if (npcName != null && npcBio != null && factionName != null) {
                dialogManager.showDialog(npcName, npcBio, factionName)
            }
        }
    }

    suspend fun handleSouthButton() {
        val playerView = (player.view as ImageDataView2)
        playerView.animation = "attack"
        playerState = "attack"
    }

    fun handleNorthButton() {
        Logger.debug("InteractionManager: North button pressed, showing pause menu")
        uiManager.showPauseMenu()
    }

    private fun getInteractiveView(): View? {
        val playerDirection = playerMovementController?.playerDirection ?: Vector2D(0.0, 0.0)
        val results = raycaster.doRay(player.pos, playerDirection, "Collides") ?: return null
        if (results.point.distanceTo(player.pos) >= 16f) return null
        return results.view
    }
}
