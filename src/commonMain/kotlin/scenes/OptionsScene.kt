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

            uiButton("One-Click Install for Local Play (Windows)") {
                onClick {
                    sceneContainer.launch {
                        runOllamaScript("install_ollama.bat")
                    }
                }
            }

            uiButton("One-Click Install for Local Play (Linux/Mac)") {
                onClick {
                    sceneContainer.launch {
                        runOllamaScript("install_ollama.sh")
                    }
                }
            }

            uiButton("One-Click Uninstall (Windows)") {
                onClick {
                    sceneContainer.launch {
                        runOllamaScript("uninstall_ollama.bat")
                    }
                }
            }

            uiButton("One-Click Uninstall (Linux/Mac)") {
                onClick {
                    sceneContainer.launch {
                        runOllamaScript("uninstall_ollama.sh")
                    }
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

    private suspend fun runOllamaScript(scriptName: String) {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val setupDir = File("setup")
            val scriptFile = File(setupDir, scriptName)

            if (!scriptFile.exists()) {
                views.gameWindow.alert("Setup script not found: $scriptName")
                return
            }

            Logger.debug("Running script: ${scriptFile.absolutePath}")

            val process = withContext(Dispatchers.IO) {
                if (isWindows) {
                    ProcessBuilder("cmd", "/c", scriptFile.absolutePath)
                } else {
                    // Make script executable
                    withContext(Dispatchers.IO) {
                        withContext(Dispatchers.IO) {
                            ProcessBuilder("chmod", "+x", scriptFile.absolutePath).start()
                        }.waitFor()
                    }
                    ProcessBuilder("bash", scriptFile.absolutePath)
                }
                    .redirectErrorStream(true)
                    .directory(setupDir)
                    .start()
            }

            // Show progress dialog
            views.gameWindow.alert("Installing OLLAMA and required models. This may take several minutes. Check the terminal/command prompt for progress.")

            // Read output
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (withContext(Dispatchers.IO) {
                    reader.readLine()
                }.also { line = it } != null) {
                Logger.debug("Script output: $line")
            }

            val exitCode = withContext(Dispatchers.IO) {
                process.waitFor()
            }
            if (exitCode == 0) {
                views.gameWindow.alert("Setup completed successfully!")
            } else {
                views.gameWindow.alert("Setup failed with exit code: $exitCode\nCheck logs for details.")
            }

        } catch (e: Exception) {
            Logger.error("Failed to run script: ${e.message}")
            views.gameWindow.alert("Failed to run setup script: ${e.message}")
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
            OLLAMA needs to be installed and running with the correct LLM models on your system for local play. 

            To install them:
            1. Download and install OLLAMA and the models 
            (run the below script for the appropriate OS for automatic download and setup; 
            read and follow SETUP_TUTORIAL.md if it fails for a step-by-step manual download tutorial)
            
            After Installation:
            1. Return to the game
            2. Select LLaMA (Local) again
            """.trimIndent()
        )
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
