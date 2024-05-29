package controls

import grid.*
import korlibs.event.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import manager.*
import ui.*

@OptIn(DelicateCoroutinesApi::class)
class PlayerControls(
    private val player: Player,
    private val grid: Array<Array<SolidRect?>>,
    private val npcs: List<NPC>
) {
    companion object {
        var isDialogActive: Boolean = false
        private var dialogWindow: DialogWindow? = null

        fun initializeDialogControls(dialog: DialogWindow) {
            dialogWindow = dialog
            setupButtonListeners()
            setupKeyListeners()
        }

        private fun setupButtonListeners() {
            dialogWindow?.sendMessageButton?.onClick {
                dialogWindow?.sendMessage()
            }
            dialogWindow?.closeButton?.onClick {
                dialogWindow?.closeDialog()
            }
        }

        //TODO fix
        private fun setupKeyListeners() {
            dialogWindow?.stage?.keys {
                down {
                    when (it.key) {
                        Key.ENTER -> dialogWindow?.sendMessage()
                        Key.ESCAPE -> dialogWindow?.closeDialog()
                        else -> {}
                    }
                }
            }
        }
    }

    private val boundingBox = player.boundingBox
    private var interacting = false
    private var direction = Vector2D(0.0, 0.0)

    private fun movePlayer() {
        if (isDialogActive) return // Prevent movement if dialog is active
        val newX = (player.x + direction.x).coerceIn(0.0, (grid.size - 1) * GridCreation.CELL_SIZE)
        val newY = (player.y + direction.y).coerceIn(0.0, (grid[0].size - 1) * GridCreation.CELL_SIZE)

        val cell = grid[(newX / GridCreation.CELL_SIZE).toInt()][(newY / GridCreation.CELL_SIZE).toInt()]
        val collisionGrid = cell != null && cell.color == GridCreation.WALL_COLOR // Check if the cell is a wall
        val collisionEntity = npcs.any { npc -> npc.bounds.intersects(Rectangle(newX, newY, player.width / 8, player.height / 8)) }

        if (!collisionGrid && !collisionEntity) {
            player.position(newX, newY)
            boundingBox.position(newX, newY)
        }
    }

    private fun interactWithNPC() {
        if (interacting) return
        if (isDialogActive) return
        val collidingNPC = npcs.find { npc -> npc.bounds.intersects(player.bounds) }
        collidingNPC?.let {
            println("Interacting with ${it.npcName}")
            startDialog(it)
            interacting = true
        }
    }

    private fun startDialog(npc: NPC) {
        val dialog = DialogWindow()
        println("Creating dialog window for NPC: ${npc.npcName}")
        initializeDialogControls(dialog)
        val gameParent = player.parent?.parent

        if (gameParent != null) {
            println("Displaying dialog window for NPC: ${npc.npcName}")
            dialog.show(gameParent, npc.bio, npc.npcName)
        } else {
            println("Error: The stage or its parent is null.")
        }
    }

    init {
        boundingBox.addUpdater {
            keys {
                down {
                    when (it.key) {
                        Key.W -> direction = Vector2D(0.0, -GridCreation.CELL_SIZE)
                        Key.S -> direction = Vector2D(0.0, GridCreation.CELL_SIZE)
                        Key.A -> direction = Vector2D(-GridCreation.CELL_SIZE, 0.0)
                        Key.D -> direction = Vector2D(GridCreation.CELL_SIZE, 0.0)
                        Key.ENTER -> interactWithNPC()
                        else -> {}
                    }
                }
                up {
                    when (it.key) {
                        Key.W, Key.S, Key.A, Key.D -> direction = Vector2D(0.0, 0.0)
                        Key.ENTER -> interacting = false
                        else -> {}
                    }
                }
            }
        }

        GlobalScope.launch {
            while (true) {
                movePlayer()
                delay(100)
            }
        }
    }
}
