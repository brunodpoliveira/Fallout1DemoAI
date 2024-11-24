package scenes

import KR
import ai.*
import bvh.*
import combat.*
import controls.*
import dialog.*
import img.*
import interactions.*
import korlibs.datastructure.*
import korlibs.image.atlas.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.io.file.*
import korlibs.korge.annotations.*
import korlibs.korge.ldtk.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.mask.*
import korlibs.korge.virtualcontroller.*
import korlibs.math.geom.*
import korlibs.math.geom.PointInt
import llm.*
import llm.impl.*
import maps.*
import movement.*
import npc.*
import player.*
import raycasting.*
import ui.*
import utils.*

class SceneLoader(
    private val scene: Scene,
    private val container: SContainer,
    private val ldtkFile: VfsFile,
    private val levelId: String
) {
    lateinit var ldtk: LDTKWorld
    private lateinit var levelView: LDTKLevelView
    lateinit var player: LDTKEntityView
    private lateinit var entitiesBvh: BvhWorld
    private lateinit var grid: IntIArray2
    private lateinit var gridSize: Size
    private lateinit var entities: List<LDTKEntityView>
    private lateinit var highlight: Graphics
    private lateinit var playerStats: EntityStats
    private lateinit var mapManager: MapManager
    private lateinit var raycaster: Raycaster
    lateinit var npcManager: NPCManager
    private lateinit var uiManager: UIManager
    private lateinit var openChestTile: TilesetRectangle
    private lateinit var defaultFont: Font
    private lateinit var playerInventory: Inventory
    private lateinit var playerManager: PlayerManager
    private lateinit var inputManager: InputManager

    lateinit var actionModel: ActionModel
    private lateinit var combatManager: CombatManager
    private lateinit var dialogManager: DialogManager
    lateinit var interactionManager: InteractionManager
    lateinit var playerMovementController: PlayerMovementController
    private lateinit var llmService: LLMService
    private lateinit var interrogationManager: InterrogationManager




    suspend fun loadScene(): SceneLoader {
        loadResources()
        initializeCommonComponents()
        return this
    }

    @OptIn(KorgeExperimental::class)
    private suspend fun loadResources() {
        val atlas = MutableAtlasUnit()
        defaultFont = KR.fonts.publicpixel.__file.readTtfFont().lazyBitmapSDF
        val playerSprite = KR.gfx.clericF.__file.readImageDataContainer(ASE.toProps(), atlas)
        ldtk = ldtkFile.readLDTKWorld()

        val level = ldtk.levelsByName["Level_0"] ?:
        throw IllegalArgumentException("Level_0 not found in LDTK world for $levelId")

        val camera = container.camera {
            levelView = LDTKLevelView(level).addTo(this)
            highlight = graphics { }
                .filters(BlurFilter(2.0).also { it.filtering = false })
                .apply { setTo(RectangleD(0, 0, 1280, 720) * 0.5) }
        }
        levelView.mask(highlight, filtering = false)
        highlight.visible = false

        entitiesBvh = BvhWorld(camera)
        gridSize = Size(16, 16)
        grid = levelView.layerViewsByName["Kind"]!!.intGrid
        entities = levelView.layerViewsByName["Entities"]!!.entities
        entities.forEach { entitiesBvh += it }

        val tileEntities = ldtk.levelsByName["TILES"]!!.layersByName["Entities"]
        val tileEntitiesByName = tileEntities?.layer?.entityInstances?.associateBy {
            it.fieldInstancesByName["Name"].valueDyn.str
        } ?: emptyMap()
        val openedChest = tileEntitiesByName["OpenedChest"]
        openChestTile = openedChest!!.tile!!

        player = entities.first { it.fieldsByName["Name"]?.valueString == "Player" }.apply {
            replaceView(
                ImageDataView2(playerSprite.default).also {
                    it.smoothing = false
                    it.animation = "idle"
                    it.anchorPixel(Point(it.width * 0.5f, it.height))
                    it.play()
                }
            )
        }

        playerStats = readEntityStats(player)
        playerInventory = Inventory("Player")
        LLMSelector.setProvider(OptionsScene.getCurrentProvider())
        val llmConfig = LLMSelector.selectProvider()
        llmService = LLMServiceFactory.create(llmConfig)
    }

    private suspend fun initializeCommonComponents() {
        // Initialize basic managers and systems first
        mapManager = MapManager(ldtk, gridSize)

        val obstacleMap = mapManager.generateMap(levelView)

        npcManager = NPCManager(
            coroutineScope = scene,
            entities = entities.filter {
                it.fieldsByName["Name"] != null && it.fieldsByName["Name"]!!.valueString != "Player"
            },
            ldtk = ldtk,
            grid = grid,
            mapManager = mapManager,
            levelView = levelView,
            entitiesBvh = entitiesBvh
        )
        npcManager.initializeNPCs()

        playerManager = PlayerManager(
            scene = scene,
            playerInventory = playerInventory,
            playerStats = playerStats,
            playerStatsUI = null
        )

        raycaster = Raycaster(
            grid = grid,
            gridSize = gridSize,
            entitiesBvh = entitiesBvh,
            entities = entities,
            player = player,
            highlight = highlight
        )

        // Initialize ActionModel before DialogManager
        actionModel = ActionModel(
            ldtk = ldtk,
            grid = grid,
            npcManager = npcManager,
            playerInventory = playerInventory,
            coroutineScope = scene
        )

        // Initialize DialogManager before UIManager and InteractionManager
        dialogManager = DialogManager(
            coroutineScope = scene,
            container = container,
            actionModel = actionModel,
            llmService = llmService
        )

        // Initialize InterrogationManager
        interrogationManager = InterrogationManager(
            coroutineScope = scene,
            container = container,
            llmService = llmService
        )

        // Initialize UIManager after DialogManager
        uiManager = UIManager(
            container = container,
            playerInventory = playerInventory,
            mapManager = mapManager,
            levelView = levelView,
            playerManager = playerManager,
            defaultFont = defaultFont,
            getPlayerPosition = PointInt(player.x.toInt(), player.y.toInt()),
            interrogationManager = interrogationManager,
            dialogManager = dialogManager
        )
        uiManager.initializeUI()
        playerManager.playerStatsUI = uiManager.playerStatsUI

        // Initialize combat system
        combatManager = CombatManager(
            enemies = entities.filter { it.entity.identifier == "Enemy" }.toMutableList(),
            playerInventory = playerInventory,
            playerStats = playerStats,
            player = player,
            playerStatsUI = uiManager.playerStatsUI,
            container = container,
            scene = scene,
            sceneView = levelView,
            raycaster = raycaster
        )
        combatManager.initialize()

        // Initialize interaction and movement systems last
        interactionManager = InteractionManager(
            player = player,
            raycaster = raycaster,
            dialogManager = dialogManager,
            playerInventory = playerInventory,
            playerStats = playerStats,
            combatManager = combatManager,
            gameWindow = scene.views.gameWindow,
            openChestTile = openChestTile,
            playerMovementController = null,
            uiManager = uiManager
        )

        inputManager = InputManager(
            controllerManager = VirtualControllerManager(combatManager),
            interactionManager = interactionManager,
            coroutineScope = scene
        )
        inputManager.setupInput(container)

        playerMovementController = PlayerMovementController(
            player = player,
            inputManager = inputManager,
            raycaster = raycaster
        )

        interactionManager.playerMovementController = playerMovementController
    }
}
