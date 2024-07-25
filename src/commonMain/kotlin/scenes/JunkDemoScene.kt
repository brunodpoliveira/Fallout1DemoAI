package scenes

import EntityStats
import Inventory
import KR
import ai.*
import bvh.*
import controls.*
import img.*
import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.korge.animate.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.ldtk.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.mask.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.PointInt
import korlibs.math.geom.ds.*
import korlibs.math.interpolation.*
import korlibs.math.raycasting.*
import korlibs.render.*
import korlibs.time.*
import kotlinx.coroutines.*
import npc.*
import readEntityStats
import ui.*
import kotlin.math.*

class JunkDemoScene : Scene() {
    companion object {
        var dialogIsOpen = false
        var isPaused = false
        var instance: JunkDemoScene? = null
    }

    init {
        instance = this
    }

    private val controllerManager = VirtualControllerManager()
    lateinit var player: LDTKEntityView
    private lateinit var rayze: LDTKEntityView
    private lateinit var baka: LDTKEntityView
    private lateinit var playerDirection: Vector2D
    private lateinit var playerState: String
    private lateinit var entitiesBvh: BvhWorld
    private lateinit var grid: IntIArray2
    private lateinit var gridSize: Size
    private lateinit var entities: List<LDTKEntityView>
    private lateinit var highlight: Graphics
    private lateinit var openChestTile: TilesetRectangle
    private lateinit var pauseMenu: PauseMenu
    private val playerInventory: Inventory = Inventory()
    private lateinit var playerStats: EntityStats
    private var playerStatsUI: PlayerStatsUI? = null

    override suspend fun SContainer.sceneMain() {
        setupScene()
    }

    @OptIn(KorgeExperimental::class)
    private suspend fun SContainer.setupScene() {

        val atlas = MutableAtlasUnit()
        val clericFemale = KR.gfx.clericF.__file.readImageDataContainer(ASE.toProps(), atlas).apply {
        }
        val rayzeSprite = KR.gfx.minotaur.__file.readImageDataContainer(ASE.toProps(), atlas).apply {
        }
        val bakaSprite = KR.gfx.wizardF.__file.readImageDataContainer(ASE.toProps(), atlas).apply {
        }
        val ldtk = KR.gfx.dungeonTilesmapCalciumtrice.__file.readLDTKWorld().apply {
        }
        val level = ldtk.levelsByName["Level_0"]!!
        val tileEntities = ldtk.levelsByName["TILES"]!!.layersByName["Entities"]
        val tileEntitiesByName = tileEntities?.layer?.entityInstances?.associateBy { it.fieldInstancesByName["Name"].valueDyn.str } ?: emptyMap()
        val closedChest = tileEntitiesByName["ClosedChest"]
        val openedChest = tileEntitiesByName["OpenedChest"]
        openChestTile = openedChest!!.tile!!

        lateinit var levelView: LDTKLevelView

        val camera = camera {
            levelView = LDTKLevelView(level).addTo(this).apply {
            }
            highlight = graphics { }
                .filters(BlurFilter(2.0).also { it.filtering = false })
                .apply {
                    setTo(RectangleD(0, 0, 1280, 720) * 0.5)
                }
        }

        levelView.mask(highlight, filtering = false)
        highlight.visible = false

        entitiesBvh = BvhWorld(camera)

        addUpdater {
            for (entity in entitiesBvh.getAll()) {
                entity.value?.update()
            }
        }

        grid = levelView.layerViewsByName["Kind"]!!.intGrid
        entities = levelView.layerViewsByName["Entities"]!!.entities

        val entityHpMap = mutableMapOf<LDTKEntityView, Int>()
        for (entity in entities) {
            val stats = readEntityStats(entity)
            entityHpMap[entity] = stats.hp
        }

        for (entity in entities) {
            entitiesBvh += entity
        }

        player = entities.first { it.fieldsByName["Name"]?.valueString == "Player" }.apply {
            replaceView(
                ImageDataView2(clericFemale.default).also {
                    it.smoothing = false
                    it.animation = "idle"
                    it.anchorPixel(Point(it.width * 0.5f, it.height))
                    it.play()
                }
            )
        }
        // Get NPC positions for display
        val npcPositions = mutableListOf<PointInt>()

        playerStats = readEntityStats(player)
        println("Player HP: ${playerStats.hp}")
        playerStatsUI = stage?.let { PlayerStatsUI(it, KR.fonts.publicpixel.__file.readTtfFont().lazyBitmapSDF) }
        playerStatsUI?.let { addChild(it) }
        playerStatsUI?.update(playerHp = playerStats.hp, playerAmmo = 0)

        addDebugReduceHealthButton(this)

        entities.firstOrNull { it.fieldsByName["Name"]?.valueString == "Rayze" }?.let { entity ->
            val rayzeStats = readEntityStats(entity)
            println("Rayze HP: ${rayzeStats.hp}")

            val x = entity.x
            val y = entity.y
            npcPositions.add(PointInt(x.toInt(), y.toInt()))

            rayze = entity.apply {
                replaceView(
                    ImageDataView2(rayzeSprite.default).also {
                        it.smoothing = false
                        it.animation = "idle"
                        it.anchor(Anchor.BOTTOM_CENTER)
                        it.play()
                    }
                )
            }
            println("Rayze initial position: ${entity.pos}")
        }

        entities.firstOrNull { it.fieldsByName["Name"]?.valueString == "Baka" }?.let { entity ->
            val bakaStats = readEntityStats(entity)
            println("Baka HP: ${bakaStats.hp}")

            val x = entity.x
            val y = entity.y
            npcPositions.add(PointInt(x.toInt(), y.toInt()))

            baka = entity.apply {
                replaceView(
                    ImageDataView2(bakaSprite.default).also {
                        it.smoothing = false
                        it.animation = "idle"
                        it.anchor(Anchor.BOTTOM_CENTER)
                        it.play()
                    }
                )
            }
            println("Baka initial position: ${entity.pos}")
        }

        pauseMenu = PauseMenu()
        controllerManager.apply {
            setupVirtualController()
            setupButtonActions(
                onAnyButton = { handleAnyButton() },
                onWestButton = { handleWestButton(this@setupScene) },
                onSouthButton = { handleSouthButton() },
                onNorthButton = { handleNorthButton(this@setupScene) }
            )
        }

        var lastInteractiveView: View? = null
        playerDirection = Vector2D(1.0, 0.0)
        gridSize = Size(16, 16)
        playerState = ""
        initNPCMovement(this, ldtk)
        addUpdater(60.hz) {
            if (!dialogIsOpen) {
                val (dx, dy) = controllerManager.getControllerInput()
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
                val moveRay = doRay(oldPos, newDir, "Collides")
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
                if (!hitTest2(newPos) || !hitTest2(oldPos)) {
                    player.pos = newPos
                    player.zIndex = player.y
                    updateRay(oldPos)
                }

                lastInteractiveView?.colorMul = Colors.WHITE
                val interactiveView = getInteractiveView()
                if (interactiveView != null) {
                    interactiveView.colorMul = Colors["#ffbec3"]
                    lastInteractiveView = interactiveView
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private fun initNPCMovement(container: Container, ldtk: LDTKWorld) {
        val movementCoroutineContext = newSingleThreadContext("MovementCoroutine")
        val pathfinding = Pathfinding(generateMap(ldtk))

        val patrolPoints = listOf(
            Point(100.0, 100.0),
            Point(200.0, 100.0),
            Point(200.0, 200.0),
            Point(100.0, 200.0)
        )

        //OPTIONAL: decomment to create a graphic representation of the obstacle map for debugging
        //val obstacleMap = generateMap(ldtk)
        //displayObstacleMap(container, obstacleMap)

        //TODO set up more sophisticated movement logic here
        GlobalScope.launch(movementCoroutineContext) {
            Movement(rayze, pathfinding).moveToPoint(253.0, 69.0)

        }

        GlobalScope.launch(movementCoroutineContext) {
            Movement(baka, pathfinding).patrol(patrolPoints)
        }
    }

    fun generateMap(ldtk: LDTKWorld, levelName: String = "Level_0"): BooleanArray2 {
        val level = ldtk.levelsByName[levelName] ?: throw IllegalArgumentException("Level $levelName not found in LDtk world")

        val lWidth = level.level.pxWid
        val lHeight = level.level.pxHei
        val gWidth = grid.width
        val gHeight = grid.height

        // Initialize the BooleanArray2 with all cells set to false (walkable)
        val gridArray = BooleanArray(gWidth * gHeight) { false }
        val array = BooleanArray2(gWidth, gHeight, gridArray)

        // First Pass: Mark cells in the "Kind" layer as walkable (false) or obstacles (true)
        level.layersByName.values.forEach { layer ->
            when (layer.layer.identifier) {
                "Kind" -> {
                    layer.layer.intGridCSV.forEachIndexed { index, value ->
                        val x = index % gWidth
                        val y = index / gWidth
                        array[x, y] = when (value) {
                            1, 3 -> false
                            else -> true  // Any other value means blocked
                        }
                    }
                }
            }
        }

        // Second Pass: Mark cells in the "Entities" layer as obstacles (true)
        level.layersByName.values.forEach { layer ->
            when (layer.layer.identifier) {
                "Entities" -> {
                    layer.layer.entityInstances.forEach { entity ->
                        val cx = entity.grid[0]
                        val cy = entity.grid[1]
                        when (entity.identifier) {
                            "Object", "Chest" -> {
                                array[cx, cy] = true
                            }
                        }
                    }
                }
            }
        }

        // Rescale the grid array to match the level dimensions
        val levelArray = BooleanArray(lWidth * lHeight) { true }
        val scaledArray = BooleanArray2(lWidth, lHeight, levelArray)

        for (y in 0 until lHeight) {
            for (x in 0 until lWidth) {
                val scaledX = (x * gWidth) / lWidth
                val scaledY = (y * gHeight) / lHeight
                scaledArray[x, y] = array[scaledX, scaledY]
            }
        }
        return scaledArray
    }

    private fun displayObstacleMap(view: Container, obstacleMap: BooleanArray2, scaleFactor: Double = 1.0) {
        val displayWidth = 300.0
        val displayHeight = 300.0

        val rectWidth = (displayWidth / obstacleMap.width) * scaleFactor
        val rectHeight = (displayHeight / obstacleMap.height) * scaleFactor

        // Start position for the obstacle map
        val offsetX = 0.0
        val offsetY = 0.0

        val graphics = view.graphics {
            Rectangle(0, 0, displayWidth, displayHeight)
            fill(Colors.BLACK) { Rectangle(0, 0, displayWidth, displayHeight) } // Background
        }

        // Draw the obstacle and free cells
        graphics.updateShape {
            for (y in 0 until obstacleMap.height) {
                for (x in 0 until obstacleMap.width) {
                    val color = if (obstacleMap[x, y]) Colors.DARKGREEN else Colors.GREEN
                    fill(color) {
                        rect(offsetX + x * rectWidth, offsetY + y * rectHeight, rectWidth, rectHeight)
                    }
                }
            }
        }
    }

    private fun handleAnyButton() {
        val view = getInteractiveView() ?: return
        val entityView = view as? LDTKEntityView ?: return
        val doBlock = entityView.fieldsByName["Items"] ?: return
        val items = doBlock.valueDyn.list.map { it.str }

        items.forEach { playerInventory.addItem(it) }

        entityView.replaceView(
            Image(entityView.tileset!!.unextrudedTileSet!!.base.sliceWithSize(openChestTile.x,
                openChestTile.y, openChestTile.w, openChestTile.h)).also {
                it.smoothing = false
                it.anchor(entityView.anchor)
            }
        )
        launchImmediately {
            gameWindow.alert("Found $items")
        }
        println("Found items: $items")
    }

    private fun addDebugReduceHealthButton(container: Container) {
        container.fixedSizeContainer(Size(200, 500), false) {
            position(700, 20)
            uiButton("Debug Reduce Health") {
                onClick {
                    debugReduceHealth(20)
                }
            }
        }
    }

    private fun consumePotion(potion: String) {
        playerInventory.consumePotion(potion, playerStats) { newHp ->
            updatePlayerHealthUI(newHp)
        }
    }

    private fun debugReduceHealth(damage: Int) {
        playerStats.hp -= damage
        if (playerStats.hp <= 0) {
            triggerGameOver()
        } else {
            updatePlayerHealthUI(playerStats.hp)
        }
    }

    private fun updatePlayerHealthUI(newHp: Int) {
        playerStatsUI?.update(playerHp = newHp, playerAmmo = 0)
    }

    private fun triggerGameOver() {
        launchImmediately {
            sceneContainer.changeTo<GameOverScene>("GameOver", this@JunkDemoScene)
        }
    }

    fun updateInventoryUI(container: Container) {
        container.removeChildren()

        container.fixedSizeContainer(Size(200, 500),false) {
            position(440, 150)

            for (item in playerInventory.getItems()) {
                uiButton(item) {
                    onClick {
                        if (item == "red_potion" && playerStats.hp < 100) {
                            consumePotion(item)
                            playerInventory.removeItem(item)
                            updateInventoryUI(container)
                        }
                    }
                }
            }
        }
    }

    private fun handleWestButton(container: Container) {
        val view = getInteractiveView() ?: return
        if (view is LDTKEntityView && view.fieldsByName["Name"] != null) {
            val npcName = view.fieldsByName["Name"]!!.valueString
            //TODO Put faction info in ldtk entity
            //TODO transfer all this logic to NPC init or some other class
            val npcFactions = mapOf(
                "Rayze" to "Crypts",
                "Baka" to "Fools",
                "Lex" to "Non-Gang"
            )
            val factionName = npcFactions[npcName] ?: "Unknown"
            val npcBio = when (npcName) {
                "Rayze" -> NPCBio.rayzeBio
                "Baka" -> NPCBio.bakaBio
                "Lex" -> NPCBio.lexBio
                else -> ""
            }
            if (npcName != null) {
                DialogWindow().show(container, npcBio, npcName,factionName)
            }
            println("INTERACTED WITH: $view :: $npcName")
        }
    }

    private fun handleSouthButton() {
        val playerView = (player.view as ImageDataView2)
        playerView.animation = "attack"
        playerState = "attack"
        handleAnyButton()  // Placeholder, assuming attack shares logic with 'use' for now
    }

    private fun handleNorthButton(container: Container) {
        if (isPaused) {
            pauseMenu.resumeGame()
        } else {
            pauseMenu.show(container)
        }
    }

    private fun IntIArray2.check(it: PointInt): Boolean {
        if (!this.inside(it.x, it.y)) return true
        val v = this.getAt(it.x, it.y)
        return v != 1 && v != 3
    }

    private fun hitTest2(pos: Point): Boolean {
        val results = entitiesBvh.bvh.search(Rectangle.fromBounds(pos - Point(1, 1), pos + Point(1, 1)))
        for (result in results) {
            val view = result.value?.view ?: continue
            if (view == player) continue
            val entityView = view as? LDTKEntityView
            val doBlock = entityView?.fieldsByName?.get("Collides")
            if (doBlock?.valueString == "false") continue
            return true
        }
        return grid.check((pos / gridSize).toInt())
    }

    private fun doRay(pos: Point, dir: Vector2D, property: String): RayResult? {
        val dir = Vector2D(
            if (dir.x.isAlmostEquals(0.0)) .00001 else dir.x,
            if (dir.y.isAlmostEquals(0.0)) .00001 else dir.y,
        )
        val ray = Ray(pos, dir)
        val outResults = arrayListOf<RayResult?>()
        val blockedResults = arrayListOf<RayResult>()
        outResults += grid.raycast(ray, gridSize, collides = { check(it) })?.also { it.view = null }
        for (result in entitiesBvh.bvh.intersect(ray)) {
            val view = result.obj.value?.view
            if (view == player) continue
            val rect = result.obj.d.toRectangle()
            val intersectionPos = ray.point + ray.direction.normalized * result.intersect
            val normalX = if (intersectionPos.x <= rect.left + 0.5f) -1f else if (intersectionPos.x >= rect.right - .5f) +1f else 0f
            val normalY = if (intersectionPos.y <= rect.top + 0.5f) -1f else if (intersectionPos.y >= rect.bottom - .5f) +1f else 0f
            val rayResult = RayResult(ray, intersectionPos, Vector2D(normalX, normalY)).also { it.view = view }
            val entityView = view as? LDTKEntityView
            val doBlock = entityView?.fieldsByName?.get(property)
            if (doBlock?.valueString == "false") {
                blockedResults += rayResult
                continue
            }
            outResults += rayResult
        }
        return outResults.filterNotNull().minByOrNull { it.point.distanceTo(pos) }?.also { res ->
            val dist = res.point.distanceTo(pos)
            res.blockedResults = blockedResults.filter { it.point.distanceTo(pos) < dist }
        }
    }

    private fun getInteractiveView(): View? {
        val results = doRay(player.pos, playerDirection, "Collides") ?: return null
        if (results.point.distanceTo(player.pos) >= 16f) return null
        return results.view
    }

    private fun updateRay(pos: Point): Double {
        val anglesCount = 64
        val angles = (0 until anglesCount).map { Angle.FULL * (it.toDouble() / anglesCount.toDouble()) }
        val results: ArrayList<RayResult> = arrayListOf()
        val results2: ArrayList<RayResult> = arrayListOf()
        val anglesDeque = Deque(angles)
        while (anglesDeque.isNotEmpty()) {
            val angle = anglesDeque.removeFirst()
            val last = results.lastOrNull()
            val current = doRay(pos, Vector2D.polar(angle), "Occludes") ?: continue
            current.blockedResults?.let { results2 += it }
            if (last != null && (last.point.distanceTo(current.point) >= 16 || last.normal != current.normal)) {
                val lastAngle = last.ray.direction.angle
                val currentAngle = current.ray.direction.angle
                if ((lastAngle - currentAngle).absoluteValue >= 0.25.degrees) {
                    anglesDeque.addFirst(angle)
                    anglesDeque.addFirst(
                        Angle.fromRatio(0.5.toRatio().interpolate(lastAngle.ratio, currentAngle.ratio))
                    )
                    continue
                }
            }
            results += current
        }
        entities.fastForEach { entity ->
            if ("hide_on_fog" in entity.entity.tags) {
                entity.simpleAnimator.cancel().sequence {
                    tween(entity::alpha[if (entity != player) .1f else 1f], time = 0.25.seconds)
                }
            }
        }
        for (result in (results + results2)) {
            val view = result.view ?: continue
            if (view.alpha != 1.0) {
                view.simpleAnimator.cancel().sequence {
                    tween(view::alpha[1f], time = 0.25.seconds)
                }
            }
        }
        highlight.updateShape {
            fill(Colors["#FFFFFF55"]) {
                rect(0, 0, 600, 500)
            }
            fill(Colors.WHITE) {
                var first = true
                for (result in results) {
                    if (first) {
                        first = false
                        moveTo(result.point)
                    } else {
                        lineTo(result.point)
                    }
                }
                close()
            }
            fill(Colors.WHITE) {
                for (result in results) {
                    val view = result.view ?: continue
                    rect(view.getBounds(highlight).expanded(MarginInt(-2)))
                }
            }
        }
        return results.minOfOrNull { it.point.distanceTo(pos) } ?: 0.0
    }

    private var RayResult.view: View? by Extra.Property { null }
    private var RayResult.blockedResults: List<RayResult>? by Extra.Property { null }
}
