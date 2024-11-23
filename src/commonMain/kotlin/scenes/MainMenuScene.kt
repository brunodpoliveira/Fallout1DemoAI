package scenes

import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import kotlin.time.Duration.Companion.milliseconds

class MainMenuScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bg = image(resourcesVfs["rect10.png"].readBitmap()) {
            size(this@sceneMain.width, this@sceneMain.height)
        }

        uiVerticalStack(padding = 4.0) {
            uiButton("Start Game") {
                onClick {
                    showLoadingScreen {
                        sceneContainer.changeTo { DemoLevel() }
                    }
                }
            }
            uiButton("Load Game"){}
            uiButton("Options") {
                onClick {
                    sceneContainer.changeTo { OptionsScene() }
                }
            }
            uiButton("Exit") {
                onClick {
                    gameWindow.close()
                }
            }
        }
    }

    private fun Container.showLoadingScreen(onComplete: suspend () -> Unit) {
        val loadingScreen = container {
            solidRect(width, height, Colors.BLACK)
            val progressBar = uiProgressBar {
                size(width * 0.8, 40.0)
                centerOnStage()
                current = 0.0
                maximum = 100.0
            }
            launchImmediately {
                while (progressBar.current < progressBar.maximum) {
                    progressBar.current += 1
                    delay(50.milliseconds)
                }
                onComplete()
                this@container.removeFromParent()
            }
        }
        this@showLoadingScreen.addChild(loadingScreen)
    }
}
