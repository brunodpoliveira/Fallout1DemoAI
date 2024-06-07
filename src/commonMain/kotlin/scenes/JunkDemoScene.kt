package scenes

import ai.*
import bvh.*
import img.*
import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.event.*
import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.korge.animate.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.ldtk.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.mask.*
import korlibs.korge.virtualcontroller.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.ds.*
import korlibs.math.interpolation.*
import korlibs.math.raycasting.*
import korlibs.render.*
import korlibs.time.*
import ui.*
import kotlin.math.*

class JunkDemoScene : Scene() {
    @OptIn(KorgeExperimental::class)
    override suspend fun SContainer.sceneMain() {
        val atlas = MutableAtlasUnit()

        val clericFemale = KR.gfx.clericF.__file.readImageDataContainer(ASE.toProps(), atlas)
        val rayze = KR.gfx.minotaur.__file.readImageDataContainer(ASE.toProps(), atlas)
        val baka = KR.gfx.wizardF.__file.readImageDataContainer(ASE.toProps(), atlas)
        val ldtk = KR.gfx.dungeonTilesmapCalciumtrice.__file.readLDTKWorld()
        val level = ldtk.levelsByName["Level_0"]!!

        val tileEntities = ldtk.levelsByName["TILES"]!!.layersByName["Entities"]
        val tileEntitiesByName = tileEntities?.layer?.entityInstances?.associateBy { it.fieldInstancesByName["Name"].valueDyn.str } ?: emptyMap()
        val ClosedChest = tileEntitiesByName["ClosedChest"]
        val OpenedChest = tileEntitiesByName["OpenedChest"]
        println("tileEntitiesByName=$tileEntitiesByName")
        lateinit var levelView: LDTKLevelView
        lateinit var highlight: Graphics
        val camera = camera {
            levelView = LDTKLevelView(level).addTo(this)
            highlight = graphics { }
                .filters(BlurFilter(2.0).also { it.filtering = false })
            setTo(RectangleD(0, 0, 1280, 720) * 0.5)
        }

        levelView.mask(highlight, filtering = false)
        highlight.visible = false

        val entitiesBvh = BvhWorld(camera)
        addUpdater {
            for (entity in entitiesBvh.getAll()) {
                entity.value?.update()
            }
        }

        println(levelView.layerViewsByName.keys)
        val grid = levelView.layerViewsByName["Kind"]!!.intGrid
        val entities = levelView.layerViewsByName["Entities"]!!.entities

        for (entity in entities) {
            entitiesBvh += entity
        }

        val player = entities.first {
            it.fieldsByName["Name"]?.valueString == "Player"
        }.apply {
            replaceView(ImageDataView2(clericFemale.default).also {
                it.smoothing = false
                it.animation = "idle"
                it.anchorPixel(Point(it.width * 0.5f, it.height))
                it.play()
            })
        }

        entities.first {
            it.fieldsByName["Name"]?.valueString == "Rayze"
        }.replaceView(ImageDataView2(rayze.default).also {
            it.smoothing = false
            it.animation = "idle"
            it.anchor(Anchor.BOTTOM_CENTER)
            it.play()
        })

        entities.first {
            it.fieldsByName["Name"]?.valueString == "Baka"
        }.replaceView(ImageDataView2(baka.default).also {
            it.smoothing = false
            it.animation = "idle"
            it.anchor(Anchor.BOTTOM_CENTER)
            it.play()
        })

        val virtualController = virtualController(
            sticks = listOf(
                VirtualStickConfig(
                    left = Key.LEFT,
                    right = Key.RIGHT,
                    up = Key.UP,
                    down = Key.DOWN,
                    lx = GameButton.LX,
                    ly = GameButton.LY,
                    anchor = Anchor.BOTTOM_LEFT,
                )
            ),
            buttons = listOf(
                VirtualButtonConfig(
                    key = Key.SPACE,
                    button = GameButton.BUTTON_SOUTH,
                    anchor = Anchor.BOTTOM_RIGHT,
                ),
                VirtualButtonConfig(
                    key = Key.RETURN,
                    button = GameButton.BUTTON_NORTH,
                    anchor = Anchor.BOTTOM_RIGHT,
                    offset = Point(0f, -100f)
                ),
                VirtualButtonConfig(
                    key = Key.Z,
                    button = GameButton.BUTTON_WEST,
                    anchor = Anchor.BOTTOM_RIGHT,
                    offset = Point(0f, -200f)
                )
            ),
        )

        var lastInteractiveView: View? = null
        var playerDirection = Vector2D(1.0, 0.0)
        val gridSize = Size(16, 16)

        var playerState = ""

        fun IntIArray2.check(it: PointInt): Boolean {
            if (!this.inside(it.x, it.y)) return true
            val v = this.getAt(it.x, it.y)
            return v != 1 && v != 3
        }

        fun hitTest(pos: Point): Boolean {
            for (result in entitiesBvh.bvh.search(Rectangle.fromBounds(pos - Point(1, 1), pos + Point(1, 1)))) {
                val view = result.value?.view ?: continue
                if (view == player) continue
                val entityView = view as? LDTKEntityView
                val doBlock = entityView?.fieldsByName?.get("Collides")
                if (doBlock?.valueString == "false") continue

                return true
            }
            return grid.check((pos / gridSize).toInt())
        }

        fun doRay(pos: Point, dir: Vector2D, property: String): RayResult? {
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

                // NARROW result. And try for example to use circles, capsules or smaller rectangles covering only the base of the object
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

        fun getInteractiveView(): View? {
            val results = doRay(player.pos, playerDirection, "Collides") ?: return null
            if (results.point.distanceTo(player.pos) >= 16f) return null
            return results.view
        }

        fun updateRay(pos: Point): Double {
            val ANGLES_COUNT = 64
            val angles = (0 until ANGLES_COUNT).map { Angle.FULL * (it.toDouble() / ANGLES_COUNT.toDouble()) }
            val results: ArrayList<RayResult> = arrayListOf()
            val results2: ArrayList<RayResult> = arrayListOf()

            val anglesDeque = Deque(angles)

            while (anglesDeque.isNotEmpty()) {
                val angle = anglesDeque.removeFirst()
                val last = results.lastOrNull()
                val current = doRay(pos, Vector2D.polar(angle), "Occludes") ?: continue
                current.blockedResults?.let {
                    results2 += it
                }
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

        addUpdater(60.hz) {
            val dx = virtualController.lx.toDouble()
            val dy = virtualController.ly.toDouble()

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
            if (!hitTest(newPos) || hitTest(oldPos)) {
                player.pos = newPos
                player.zIndex = player.y
                updateRay(oldPos)
            } else {
                //println("Check why is this happening. Operation could have lead to stuck: oldPos=$oldPos -> newPos=$newPos, finalDir=$finalDir, moveRay=$moveRay")
            }

            lastInteractiveView?.colorMul = Colors.WHITE
            val interactiveView = getInteractiveView()
            if (interactiveView != null) {
                interactiveView.colorMul = Colors["#ffbec3"]
                lastInteractiveView = interactiveView
            }
        }

        keys {
            down(Key.R) {
                println(player.pos)
            }
        }

        virtualController.apply {
            fun onAnyButton() {
                val view = getInteractiveView() ?: return
                val entityView = view as? LDTKEntityView ?: return
                val doBlock = entityView.fieldsByName["Items"] ?: return
                val items = doBlock.valueDyn.list.map { it.str }
                val tile = OpenedChest!!.tile!!
                entityView.replaceView(
                    Image(entityView.tileset!!.unextrudedTileSet!!.base.sliceWithSize(tile.x, tile.y, tile.w, tile.h)).also {
                        it.smoothing = false
                        it.anchor(entityView.anchor)
                    }
                )
                launchImmediately {
                    gameWindow.alert("Found $items")
                }
                println("INTERACTED WITH: " + view + " :: ${doBlock.value!!::class}, ${doBlock.value}")
            }

            down(GameButton.BUTTON_WEST) {
                val view = getInteractiveView()
                if (view is LDTKEntityView && view.fieldsByName["Name"] != null) {
                    val npcName = view.fieldsByName["Name"]!!.valueString
                    val npcBio = when (npcName) {
                        "Rayze" -> NPCBio.rayzeBio
                        "Baka" -> NPCBio.bakaBio
                        else -> ""
                    }
                    if (npcName != null) {
                        DialogWindow().show(this@sceneMain, npcBio, npcName)
                        println("INTERACTED WITH: $view :: $npcName")
                    } else{
                        println("npcName = null")
                    }
                }
            }
            down(GameButton.BUTTON_SOUTH) {
                val playerView = (player.view as ImageDataView2)
                playerView.animation = "attack"
                playerState = "attack"
                onAnyButton()
            }
            down(GameButton.BUTTON_NORTH) {
                val playerView = (player.view as ImageDataView2)
                playerView.animation = "attack"
                playerState = "gesture"
                onAnyButton()
            }
        }
    }
}

private var RayResult.view: View? by Extra.Property { null }
private var RayResult.blockedResults: List<RayResult>? by Extra.Property { null }
