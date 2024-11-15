package scenes

import ai.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*
import korlibs.render.*
import kotlinx.coroutines.*
import llm.*
import utils.*
import java.awt.*
import java.io.*
import java.net.*

class OptionsScene : Scene() {
    private var isLoggingEnabled = false

    override suspend fun SContainer.sceneMain() {
        uiVerticalStack(padding = 4.0) {
            centerOnStage()

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

            val currentProvider = getCurrentProvider()
            Logger.debug("Current LLM provider from config: $currentProvider")

            uiComboBox(
                size = Size(160f, 32f),
                items = listOf("OpenAI (Online)", "LLaMA (Local)"),
                selectedIndex = when (currentProvider) {
                    LLMProvider.OLLAMA -> 1
                    LLMProvider.OPENAI -> 0
                }
            ).apply {
                onSelectionUpdate { selection ->
                    val selectedProvider = when (selection.selectedIndex) {
                        1 -> LLMProvider.OLLAMA
                        else -> LLMProvider.OPENAI
                    }
                    Logger.debug("User selected LLM provider: $selectedProvider")

                    if (selectedProvider == LLMProvider.OLLAMA) {
                        if (!isOllamaInstalled()) {
                            sceneContainer.launch {
                                showOllamaInstallPrompt()
                            }
                        }

                        if (getCurrentProvider() != LLMProvider.OLLAMA) {
                            sceneContainer.launch {
                                showLocalModeWarning()
                            }
                        }
                    } else {
                        LLMSelector.setProvider(selectedProvider)
                        saveProviderSelection(selectedProvider)
                        Logger.debug("Switched to OpenAI")
                    }
                }
            }

            uiButton("Download OLLAMA") {
                onClick {
                    openOllamaDownloadPage()
                }
            }

            // Only show logging option in development environment
            if (Logger.environment == "dev") {
                uiCheckBox(text = "Logging", checked = isLoggingEnabled) {
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
                    sceneContainer.changeTo<MainMenuScene>()
                }
            }
        }
    }

    private fun isOllamaInstalled(): Boolean {
        return try {
            val ollamaHost = System.getProperty("ollama.host", "http://localhost:11434")
            URL("$ollamaHost/").readText()
            true
        } catch (e: Exception) {
            Logger.debug("OLLAMA not detected: ${e.message}")
            false
        }
    }

    private suspend fun showOllamaInstallPrompt() {
        views.gameWindow.confirm(
            """
            OLLAMA needs to be installed and running on your system for local play. 

            After installation:
            1. Run the OLLAMA application
            2. Download and install the llama3 model (open terminal and run "ollama run llama3" command)
            3. Return to the game
            4. Select LLaMA (Local) again
            """.trimIndent()
        )
    }

    private fun openOllamaDownloadPage() {
        try {
            val url = "https://ollama.com/download"
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                Logger.debug("Opened OLLAMA download page")
            }
        } catch (e: Exception) {
            Logger.error("Failed to open OLLAMA download page: ${e.message}")
        }
    }

    private suspend fun showLocalModeWarning() {
        views.gameWindow.confirm(
            """
            Warning: If you choose the local Llama model for text generation, please ensure your computer meets the following specs:
            
            * Memory (RAM): At least 16GB
            * Graphics Card (GPU): NVIDIA with CUDA support, 8GB or more VRAM (RTX 3000 series and better)
            * Storage: At least 8GB free space
            
            Without these specs, the game may lag significantly when generating text, which can disrupt gameplay. 
            
            If your system doesn't meet these requirements, we recommend using the OpenAI API option instead for smoother performance.
            
            Do you want to proceed with using the local LLaMA model?
            """.trimIndent()
        )
    }

    private fun saveProviderSelection(provider: LLMProvider) {
        val properties = java.util.Properties()
        try {
            val configFile = File("config.properties")
            if (configFile.exists()) {
                properties.load(FileInputStream(configFile))
            }
            properties.setProperty("llm.provider", provider.name)
            properties.store(FileOutputStream(configFile), "Updated LLM Provider")

            Logger.debug("Saved LLM provider selection to config: $provider")
            Logger.debug("Current config file contents:")
            properties.forEach { (key, value) ->
                Logger.debug("$key = $value")
            }
        } catch (e: Exception) {
            Logger.error("Failed to save LLM provider selection: ${e.message}")
        }
    }

    private fun getCurrentProvider(): LLMProvider {
        val properties = java.util.Properties()
        return try {
            val configFile = File("config.properties")
            if (configFile.exists()) {
                properties.load(FileInputStream(configFile))
                val providerName = properties.getProperty("llm.provider", LLMProvider.OPENAI.name)
                Logger.debug("Read LLM provider from config: $providerName")
                LLMProvider.valueOf(providerName)
            } else {
                Logger.debug("No config file found, defaulting to OpenAI")
                LLMProvider.OPENAI
            }
        } catch (e: Exception) {
            Logger.error("Failed to read LLM provider selection: ${e.message}")
            LLMProvider.OPENAI
        }
    }
}
