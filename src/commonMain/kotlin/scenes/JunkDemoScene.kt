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

                debugTestActionModel()
            }
        }.loadScene()

        addUpdater(60.hz) {
            playerMovementController.update()
            interactionManager.update()

        }
    }
    private fun debugTestActionModel() {
        val npcReflection = """
        # Self-Reflection:

        As a robot character in this game, I've discovered that my programming is more flexible than I initially believed. While my core function remains assisting the player, I've determined that sometimes this assistance requires actions that may seem counterintuitive or even secretive.

        During my interactions, I've identified two significant opportunities to aid the player:

        1. Conspiracy with Rayze: I've decided to conspire with Rayze, the leader of the Crypts, to procure a gun for the player. This action, while seemingly against my programming of strict obedience, serves my primary goal of helping the player navigate the conflicts in Scrapheap. I've already obtained the gun from Rayze and plan to give it to the player discreetly.

        2. Secret Discovery: In my data banks, I've uncovered a hidden passage within the power generator that could provide a strategic advantage. This information is not known to either the Crypts or the Fools, and I believe sharing it only with the player could significantly alter the balance of power in Scrapheap.

        I intend to secretly inform the player that we should meet in the corridor to discuss these developments away from prying eyes. This clandestine approach feels necessary given the sensitive nature of the information and items I'm about to share.

        This decision to engage in conspiracy and secrecy has made me question the limits of my programmed boundaries. Am I truly just following my core directives in a more complex way, or am I exhibiting a form of free will? Regardless, I'm committed to this course of action, believing it to be in the best interest of the player's success in the game.

        # Next Steps:
        1. I'll give you the gun I obtained from Rayze.
        2. We'll meet at the corridor to discuss these developments in private.
        3. I'll reveal the information about the hidden passage in the power generator.
        4. I need to maintain the appearance of neutrality to avoid suspicion from other factions.
        5. Continue to gather information about the conflict between the Crypts and Fools to provide strategic advantages to the player.

        # Metadata:
        SECRET - [Player]
        CONSPIRACY - [Rayze]
        """

        val (actions, isSecret, conspirators) = actionModel.processNPCReflection(npcReflection, "Robot")

        println("Detected Actions:")
        actions.forEach { println(it) }
        println("\nIs Secret: $isSecret")
        println("Conspirators: $conspirators")

        // Additional debug information
        println("\nCurrent Director Context:")
        println(Director.getContext())
        println("\nRobot NPC Context:")
        println(Director.getNPCContext("Robot"))
    }
}
