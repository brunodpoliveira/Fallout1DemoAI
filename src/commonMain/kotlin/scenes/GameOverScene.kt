package scenes

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import utils.*

class GameOverScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        solidRect(width, height, Colors["#FF0000"])

        uiVerticalStack(padding = 8.0) {
            centerOnStage()

            uiText("Game Over") {
                centerXOnStage()
            }

            uiButton("Restart") {
                onClick {
                    restartLevel()
                    }
                }

            uiButton("Return to Main Menu") {
                onClick {
                    sceneContainer.run { changeTo<MainMenuScene>() }
                }
            }
        }
    }
    private suspend fun restartLevel() {
        when (GameState.currentLevel) {
            "scrapheap" -> sceneContainer.changeTo<DemoLevel>()
            "interrogation" -> sceneContainer.changeTo<OtherDemoLevel>()
            // Add more levels as needed
            else -> {
                println("Unknown level: ${GameState.currentLevel}. Defaulting to scrapheap.")
                sceneContainer.changeTo<DemoLevel>()
            }
        }
    }
}
