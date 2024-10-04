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
                    playerMovementController = null
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
                npcManager.initializeNPCCollisionBoxes()
            }
        }.loadScene()

        addUpdater(60.hz) {
            playerMovementController.update()
            interactionManager.update()
            //sceneLoader.entitiesBvh.getBvhEntity(player)?.update()
            sceneLoader.npcManager.updateNPCCollisionBoxes()
        }
    }
}
