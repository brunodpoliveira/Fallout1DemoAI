package scenes

import ai.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*
import utils.*

class OptionsScene : Scene() {
    private var isLoggingEnabled = false

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

            // Only show logging option in development environment
            if (Logger.environment == "dev") {
                uiCheckBox(text = "Enable Logging", checked = isLoggingEnabled) {
                    onChange {
                        isLoggingEnabled = it.checked
                        if (isLoggingEnabled) {
                            Logger.enableFileLogging("fallout_demo_${System.currentTimeMillis()}.log")
                            Logger.debug("File logging enabled from Options menu")
                        } else {
                            Logger.disableFileLogging()
                            Logger.debug("File logging disabled from Options menu")
                        }
                    }
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
