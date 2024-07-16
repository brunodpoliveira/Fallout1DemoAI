package scenes

import ai.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*

class OptionsScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        uiVerticalStack(padding = 4.0) {
            uiText("Options Menu") {
                centerXOnStage()
            }

            uiComboBox(
                size = Size(160f, 32f),
                items = listOf("Easy", "Normal"),
                selectedIndex = if (Director.getDifficulty() == "easy") 0 else 1
            ).apply {
                onSelectionUpdate {
                    val selectedDifficulty = it.selectedItem?.lowercase() ?: "easy"
                    Director.setDifficulty(selectedDifficulty)
                }
            }

            uiButton("Back to Main Menu") {
                centerXOnStage()
                onClick {
                    sceneContainer.run { changeTo<MainMenuScene>() }
                }
            }
        }
    }
}
