package scenes

import ai.*
import combat.*
import controls.*
import dialog.*
import interactions.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.time.*
import movement.*
import utils.*

class JunkDemoScene : Scene() {
    companion object {
        var dialogIsOpen = false
        var isPaused = false
        var instance: JunkDemoScene? = null
    }

    init {
        instance = this
    }

    lateinit var player: LDTKEntityView
    lateinit var actionModel: ActionModel
    private lateinit var combatManager: CombatManager
    private lateinit var playerMovementController: PlayerMovementController
    private lateinit var dialogManager: DialogManager
    private val controllerManager = VirtualControllerManager()
    private lateinit var inputManager: InputManager
    private lateinit var interactionManager: InteractionManager
    lateinit var ldtk: LDTKWorld

    override suspend fun SContainer.sceneMain() {
        val sceneLoader = object : SceneLoader(this@JunkDemoScene, this) {
            override suspend fun onSceneLoad() {
                combatManager = CombatManager(
                    enemies = entities.filter { it.entity.identifier == "Enemy" }.toMutableList(),
                    playerInventory = playerInventory,
                    playerStats = playerStats,
                    playerStatsUI = uiManager.playerStatsUI,
                    container = container
                )
                combatManager.initialize()

                actionModel = ActionModel(
                    ldtk = ldtk,
                    grid = grid,
                    npcManager = npcManager,
                    playerInventory = playerInventory,
                    coroutineScope = scene
                )
                dialogManager = DialogManager(
                    coroutineScope = scene,
                    container = container,
                    actionModel = actionModel
                )

                interactionManager = InteractionManager(
                    player = player,
                    raycaster = raycaster,
                    dialogManager = dialogManager,
                    playerInventory = playerInventory,
                    playerStats = playerStats,
                    combatManager = combatManager,
                    gameWindow = views.gameWindow,
                    openChestTile = openChestTile,
                    playerMovementController = null,
                    uiManager = uiManager
                )

                inputManager = InputManager(controllerManager, interactionManager, scene)
                inputManager.setupInput(container)

                playerMovementController = PlayerMovementController(
                    player = player,
                    inputManager = inputManager,
                    raycaster = raycaster
                )

                interactionManager.playerMovementController = playerMovementController
                entitiesBvh.getBvhEntity(player)?.update()

                debugTestActionModel(playerInventory)
            }
        }.loadScene()

        addUpdater(60.hz) {
            playerMovementController.update()
            interactionManager.update()

        }
    }
    private fun debugTestActionModel(playerInventory:Inventory) {
        val robotMovementTest = "I'll meet at the STATUE"
        actionModel.processNPCReflection(robotMovementTest, "Robot")
        val giveItemTest = "I'll give you my Gun"
        actionModel.processNPCReflection(giveItemTest, "Robot")
        println("Player inventory after debug test: ${playerInventory.getItems()}")
    }
}
