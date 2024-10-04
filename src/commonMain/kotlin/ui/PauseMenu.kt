package ui

import KR
import img.TextDisplayManager
import ai.*
import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.TextAlignment.Companion.TOP_LEFT
import korlibs.korge.input.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*
import maps.*
import scenes.*
import scenes.JunkDemoScene.Companion.isPaused
import ui.DialogWindow.Companion.isInDialog

class PauseMenu(private val mapManager: MapManager,
                private  val levelView: LDTKLevelView,
                private val getPlayerPosition: PointInt) : Container() {

    init {
        if (!isInDialog) {
            setupPauseMenu()
        }
    }

    private fun setupPauseMenu(){
        removeChildren()

        // Set the position and size explicitly
        position(0, 0)
        size(1280, 720)

        // Ensure this container is on top
        zIndex = 1000.0

        solidRect(1280, 720, Colors["#00000088"])

        uiVerticalStack(padding = 4.0, width = 400.0) {
            position(440, 150)

            uiButton("Resume") {
                onClick {
                    resumeGame()
                }
            }
            uiButton("Save/Load"){}
            uiButton("Inventory") {
                onClick {
                    showInventory(this@uiVerticalStack)
                }
            }
            uiButton("Options") {}
            uiButton("Notes") {
                onClick {
                    showNotes(this@uiVerticalStack)
                }
            }
            uiButton("Auto-Map") {
                onClick {
                    showAutoMap(this@uiVerticalStack)
                }
            }
            uiButton("Return to Main Menu") {
                onClick {
                    //TODO fix misaligned menu and not destroying gameplay scene
                    sceneContainer().run { changeTo<MainMenuScene>() }
                }
            }
        }
    }

    fun show(mainContainer: Container) {
        pauseGame(mainContainer)
    }

    private fun pauseGame(mainContainer: Container) {
        if (isPaused) return
        isPaused = true
        mainContainer.speed = 0.0 // Freezes game world updates
        mainContainer.addChild(this)
    }

    fun resumeGame() {
        if (!isPaused) return
        isPaused = false
        this@PauseMenu.parent?.speed = 1.0 // Resumes game world updates
        this@PauseMenu.removeFromParent()
    }

    //TODO split this into new scenes
    private fun showInventory(container: Container) {
        container.removeChildren()
        solidRect(1280, 720, Colors["#00000088"]) // Semi-transparent background

        uiVerticalStack(padding = 4.0, width = 400.0) {
            position(440, 150)
            uiText("Inventory:")

            //JunkDemoScene.instance?.updateInventoryUI(this@uiVerticalStack)

            uiButton("Back to Menu") {
                onClick {
                    setupPauseMenu()
                }
            }
        }
    }

    private suspend fun showNotes(container: Container) {
        container.removeChildren()
        val font = KR.fonts.publicpixel.__file.readTtfFont().lazyBitmapSDF
        val directorText = text("", font = font).xy(10, 10)
        val selfReflectionText = text("", font = font).xy(10, 60)
        val nextStepsText = text("", font = font).xy(10, 110)

        TextDisplayManager.directorText = directorText
        TextDisplayManager.selfReflectionText = selfReflectionText
        TextDisplayManager.nextStepsText = nextStepsText

        uiScrollable(size = Size(800, 400)) {
            position(240, 110)
            val textContainer = container {
                directorText.text = "Director:\n${Director.getContext()}"
                addChild(directorText)

                if (Director.getDifficulty() == "easy") {
                    selfReflectionText.text = "Self-Reflection:\n"
                    nextStepsText.text = "Next Steps:\n"

                    addChild(selfReflectionText)
                    addChild(nextStepsText)
                }
            }
            addChild(textContainer)
        }
        uiButton("Back to Pause Menu") {
            centerXOnStage()
            onClick {
                setupPauseMenu()
            }
        }.xy(640, 530)
    }

    private suspend fun displayAutoMap(view: Container,
                                       obstacleMap: BooleanArray2,
                                       playerPosition: PointInt,
                                       npcPositions: List<PointInt>,
                                       chestPositions: List<PointInt>,
                                       objectPositions: List<PointInt>,
                                       scaleFactor: Double = 1.0) {

        val displayWidth = 300.0
        val displayHeight = 300.0
        val subtitleStartY = displayHeight + 10.0  // Position below the map
        val subtitleX = 10.0  // Left margin for subtitles

        val rectWidth = (displayWidth / obstacleMap.width) * scaleFactor
        val rectHeight = (displayHeight / obstacleMap.height) * scaleFactor

        // Start position for the auto-map
        val offsetX = 0.0
        val offsetY = 0.0

        val font = KR.fonts.publicpixel.__file.readTtfFont().lazyBitmapSDF

        val graphics = view.graphics {}

        fun drawSubtitleText(yOffset: Double, text: String) {
            view.text(
                text = text,
                font = font,
                textSize = 14.0,
                color = Colors.WHITE,
                alignment = TOP_LEFT
            ).position(subtitleX + rectWidth + 20.0, subtitleStartY + yOffset + 5.0)
        }

        fun scheduleSubtitleTexts() {
            drawSubtitleText(0.0, "Walkable Area")
            drawSubtitleText(25.0, "Non-Walkable Area")
            drawSubtitleText(50.0, "Chests")
            drawSubtitleText(75.0, "Objects")
            drawSubtitleText(100.0, "NPCs")
            drawSubtitleText(125.0, "Player")
        }

        graphics.updateShape {
            // Draw the obstacle map
            for (y in 0 until obstacleMap.height) {
                for (x in 0 until obstacleMap.width) {
                    val color = if (obstacleMap[x, y]) Colors.DARKGREEN else Colors.GREEN
                    fill(color) {
                        rect(Point(offsetX + x * rectWidth, offsetY + y * rectHeight), Size(rectWidth + 1, rectHeight + 1))
                    }
                }
            }
            // Draw entities (chests, objects)
            chestPositions.forEach { pos ->
                fill(Colors.YELLOW) {
                    rect(Point(offsetX + pos.x * rectWidth, offsetY + pos.y * rectHeight), Size(15.0, 15.0)) // Increased size for clear visibility
                }
            }

            objectPositions.forEach { pos ->
                fill(Colors.BLUE) {
                    rect(Point(offsetX + pos.x * rectWidth, offsetY + pos.y * rectHeight), Size(15.0, 15.0)) // Increased size for clear visibility
                }
            }

            npcPositions.forEach { pos ->
                fill(Colors.ORANGE) {
                    rect(Point(offsetX + pos.x * rectWidth, offsetY + pos.y * rectHeight), Size(15.0, 15.0)) // Increased size for clear visibility
                }
            }

            // Draw player's current location with a black square
            fill(Colors.BLACK) {
                rect(Point(offsetX + playerPosition.x * rectWidth, offsetY + playerPosition.y * rectHeight), Size(15.0, 15.0)) // Increased size for clear visibility
            }

            fun drawSubtitleColor(yOffset: Double, color: RGBA) {
                fill(color) {
                    rect(Point(subtitleX, subtitleStartY + yOffset), Size(rectWidth + 10.0, rectHeight + 10.0)) // Larger size for visibility
                }
            }

            drawSubtitleColor(0.0, Colors.GREEN)
            drawSubtitleColor(25.0, Colors.DARKGREEN)
            drawSubtitleColor(50.0, Colors.YELLOW)
            drawSubtitleColor(75.0, Colors.BLUE)
            drawSubtitleColor(100.0, Colors.ORANGE)
            drawSubtitleColor(125.0, Colors.BLACK)
        }

        scheduleSubtitleTexts()
    }

    private fun scaleEntityPositions(entities: List<PointInt>): List<PointInt> {
        val mapScale = 16
        return entities.map { point ->
            val x = point.x * mapScale
            val y = point.y * mapScale
            PointInt(x, y)
        }
    }

    // Method in your Game Scene to open the auto-map
    private suspend fun showAutoMap(container: Container) {
        container.removeChildren()

        // Semi-transparent background
        solidRect(1280, 720, Colors.BLACK.withAd(0.5))

        // Scrollable UI for the auto-map
        uiScrollable(size = Size(800, 600)) {
            position(240.0, 60.0)

            val autoMapContainer = container {
                // Read the LDTKWorld object - customize the path as needed
                val ldtk = KR.gfx.dungeonTilesmapCalciumtrice.__file.readLDTKWorld().apply { }

                // Generate the obstacle map from the specific level in the LDTK world
                val obstacleMap = mapManager.generateMap(levelView)

                val playerPosition = getPlayerPosition

                // Fetching positions as scaled using the map generation logic
                val chestPositions = getEntityPositions(ldtk, "Chest")
                val objectPositions = getEntityPositions(ldtk, "Object")
                val npcPositions = getEntityPositions(ldtk, "Enemy")

                // Scaled positions based on the map grid
                val scaledChestPositions = scaleEntityPositions(chestPositions)
                val scaledObjectPositions = scaleEntityPositions(objectPositions)
                val scaledNpcPositions = scaleEntityPositions(npcPositions)

                displayAutoMap(this, obstacleMap, playerPosition, scaledNpcPositions, scaledChestPositions, scaledObjectPositions)
            }

            addChild(autoMapContainer)
        }

        uiButton("Back to Menu") {
            centerXOnStage()
            onClick { setupPauseMenu() }
        }.xy(640.0, 680.0)
    }

    private fun getEntityPositions(ldtk: LDTKWorld, entityType: String): List<PointInt> {
        val positions = mutableListOf<PointInt>()
        val level = ldtk.levelsByName["Level_0"] ?: return positions

        level.layersByName.values.forEach { layer ->
            if (layer.layer.identifier == "Entities") {
                layer.entities.forEach { entity ->
                    if (entity.identifier == entityType) {
                        val cx = entity.grid[0]
                        val cy = entity.grid[1]
                        positions.add(PointInt(cx, cy))
                    }
                }
            }
        }

        return positions
    }
}
