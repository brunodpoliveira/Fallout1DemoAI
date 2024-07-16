package scenes

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*

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
                    sceneContainer.run { changeTo<JunkDemoScene>()
                    }
                }
            }

            uiButton("Return to Main Menu") {
                onClick {
                    sceneContainer.run { changeTo<MainMenuScene>() }
                }
            }
        }
    }
}
