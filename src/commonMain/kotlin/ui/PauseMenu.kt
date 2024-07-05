package ui

import KR
import TextDisplayManager
import ai.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*
import scenes.*
import scenes.JunkDemoScene.Companion.isPaused

class PauseMenu : Container() {

    init {
        setupMainMenu()
    }

    private fun setupMainMenu(){
        removeChildren()
        solidRect(1280, 720, Colors["#00000088"]) // Semi-transparent background

        uiVerticalStack(padding = 4.0, width = 400.0) {
            position(440, 150)

            uiButton("Resume") {
                onClick {
                    resumeGame()
                }
            }
            uiButton("Save/Load"){}
            uiButton("Inventory"){}
            uiButton("Options") {}
            uiButton("Notes") {
                onClick {
                    showNotes(this@uiVerticalStack)
                }
            }
            uiButton("Return to Main Menu") {
                onClick {
                    //TODO FIX main menu misalignment bug
                    sceneContainer().changeTo { MainMenuScene() }
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
                setupMainMenu()
            }
        }.xy(640, 530)
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
}
