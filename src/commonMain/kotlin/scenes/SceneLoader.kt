package scenes

import bvh.*
import img.*
import korlibs.datastructure.*
import korlibs.image.atlas.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.korge.annotations.*
import korlibs.korge.ldtk.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.mask.*
import korlibs.math.geom.*
import maps.*
import npc.*
import player.*
import raycasting.*
import ui.*
import utils.*


open class SceneLoader(
    protected val scene: Scene,
    protected val container: SContainer
) {
    // Common properties accessible by subclasses
    lateinit var ldtk: LDTKWorld
    private lateinit var levelView: LDTKLevelView
    lateinit var player: LDTKEntityView
    lateinit var entitiesBvh: BvhWorld
    lateinit var grid: IntIArray2
    private lateinit var gridSize: Size
    lateinit var entities: List<LDTKEntityView>
    private lateinit var highlight: Graphics
    lateinit var playerStats: EntityStats
    private lateinit var mapManager: MapManager
    lateinit var raycaster: Raycaster
    lateinit var npcManager: NPCManager
    lateinit var uiManager: UIManager
    lateinit var openChestTile: TilesetRectangle
    private lateinit var defaultFont: Font
    lateinit var playerInventory: Inventory
    private lateinit var playerManager: PlayerManager

    open suspend fun loadScene(): SceneLoader {
        loadResources()
        initializeCommonComponents()
        onSceneLoad()

        return this
    }

    protected open suspend fun onSceneLoad() {
        // This method can be overridden by subclasses for scene-specific initializations
    }

    @OptIn(KorgeExperimental::class)
    protected open suspend fun loadResources() {
        // Load resources required by all scenes
        val atlas = MutableAtlasUnit()
        defaultFont = KR.fonts.publicpixel.__file.readTtfFont().lazyBitmapSDF
        val playerSprite = KR.gfx.clericF.__file.readImageDataContainer(ASE.toProps(), atlas)
        ldtk = KR.gfx.dungeonTilesmapCalciumtrice.__file.readLDTKWorld()
        val level = ldtk.levelsByName["Level_0"]!!

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
        playerInventory = Inventory()
        playerManager = PlayerManager(
            scene = scene,
            playerInventory = playerInventory,
            playerStats = playerStats,
            playerStatsUI = null // PlayerStatsUI will be initialized in UIManager
        )
    }

    protected open suspend fun initializeCommonComponents() {
        // Initialize MapManager
        mapManager = MapManager(ldtk, gridSize)
        val obstacleMap = mapManager.generateMap(levelView)

        // Initialize NPCManager
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

        // Initialize UIManager
        uiManager = UIManager(
            container = container,
            playerInventory = playerInventory,
            mapManager = mapManager,
            levelView = levelView,
            playerManager = playerManager,
            defaultFont = defaultFont
        )
        uiManager.initializeUI()
        playerManager.playerStatsUI = uiManager.playerStatsUI

        // Initialize Raycaster
        raycaster = Raycaster(
            grid = grid,
            gridSize = gridSize,
            entitiesBvh = entitiesBvh,
            entities = entities,
            player = player,
            highlight = highlight
        )
    }
}
