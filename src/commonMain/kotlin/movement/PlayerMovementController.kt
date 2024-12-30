package movement

import combat.*
import controls.*
import img.*
import interactions.*
import korlibs.korge.ldtk.view.*
import korlibs.math.*
import korlibs.math.geom.*
import raycasting.*
import utils.*
import kotlin.math.*

class PlayerMovementController(
    private val player: LDTKEntityView,
    var inputManager: InputManager?,
    private val raycaster: Raycaster,
    private val playerInteractionHandler: PlayerInteractionHandler,
    private val combatManager: CombatManager
) {
    var playerDirection: Vector2D = Vector2D(1.0, 0.0)
    private var playerState: String = ""

    fun update() {
        if (!GameState.isDialogOpen) {
            val (dx, dy) = inputManager?.getControllerInput() ?: Pair(0.0, 0.0)
            val playerView = (player.view as ImageDataView2)
            if (!dx.isAlmostZero() || !dy.isAlmostZero()) {
                playerDirection = Vector2D(dx.sign, dy.sign)
            }
            if (dx == 0.0 && dy == 0.0) {
                playerView.animation = if (playerState != "") playerState else "idle"
            } else {
                playerState = ""
                playerView.animation = "walk"
                playerView.scaleX = if (playerDirection.x < 0) -1.0 else +1.0
            }
            val speed = 1.5
            val newDir = Vector2D(dx * speed, dy * speed)
            val oldPos = player.pos
            val moveRay = raycaster.doRay(oldPos, newDir, "Collides")
            val finalDir = if (moveRay != null && moveRay.point.distanceTo(oldPos) < 6f) {
                val res = newDir.reflected(moveRay.normal)
                if (moveRay.normal.y != 0.0) {
                    Vector2D(res.x, 0f)
                } else {
                    Vector2D(0f, res.y)
                }
            } else {
                newDir
            }
            val newPos = oldPos + finalDir
            if (!raycaster.hitTest2(newPos) || !raycaster.hitTest2(oldPos)) {
                player.pos = newPos
                player.zIndex = player.y
                raycaster.updateRay(oldPos)
            }
            playerInteractionHandler.update()
            combatManager.startGame()
        }
    }
}
