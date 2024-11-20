package scenes

import ai.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*
import korlibs.platform.*
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

            when {
                Platform.isWindows -> {
                    uiButton("One-Click Install (Windows)").also {
                        it.width = 210.0
                        onClick {
                            sceneContainer.launch {
                                installOllama("windows")
                            }
                        }
                    }
                    uiButton("Uninstall (Windows)").also {
                        it.width = 210.0
                        onClick {
                            sceneContainer.launch {
                                uninstallOllama("windows")
                            }
                        }
                    }
                }
                Platform.isMac -> {
                    uiButton("One-Click Install (macOS)").also {
                        it.width = 210.0
                        onClick {
                            sceneContainer.launch {
                                installOllama("mac")
                            }
                        }
                    }
                    uiButton("Uninstall (macOS)").also {
                        it.width = 210.0
                        onClick {
                            sceneContainer.launch {
                                uninstallOllama("mac")
                            }
                        }
                    }
                }
                Platform.isLinux -> {
                    uiButton("One-Click Install (Linux)").also {
                        it.width = 210.0
                        onClick {
                            sceneContainer.launch {
                                installOllama("linux")
                            }
                        }
                    }
                    uiButton("Uninstall (Linux)").also {
                        it.width = 210.0
                        onClick {
                            sceneContainer.launch {
                                uninstallOllama("linux")
                            }
                        }
                    }
                }
                else -> {
                    uiText("Unsupported operating system").also {
                        it.width = 210.0
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

            uiButton("Back to Main Menu") .also {
                it.width = 190.0
                centerXOnStage()
                onClick {
                    sceneContainer.changeTo<MainMenuScene>()
                }
            }
        }
    }

    private suspend fun installOllama(platform: String) {
        try {
            val setupDir = File("setup").apply { mkdirs() }

            val scriptName = when (platform) {
                "windows" -> "install_ollama.bat"
                "mac" -> "install_ollama_alt.sh"
                "linux" -> "install_ollama.sh"
                else -> throw IllegalArgumentException("Unknown platform: $platform")
            }

            // Extract script from resources to setup dir
            val scriptFile = File(setupDir, scriptName)
            if (!scriptFile.exists()) {
                val scriptContent = resourcesVfs["setup/$scriptName"].readString()
                scriptFile.writeText(scriptContent)

                // Make script executable on Unix systems
                if (platform != "windows") {
                    withContext(Dispatchers.IO) {
                        ProcessBuilder("chmod", "+x", scriptFile.absolutePath).start().waitFor()
                    }
                }
            }

            views.gameWindow.alert(
                """
                Starting Ollama installation. This may take several minutes.
                A command window will open to show installation progress.
                
                Required steps:
                1. Download and install Ollama
                2. Download required AI models
                3. Configure the service
                """.trimIndent()
            )

            val process = withContext(Dispatchers.IO) {
                when (platform) {
                    "windows" -> {
                        // Use cmd /k to keep the window open
                        ProcessBuilder(
                            "cmd",
                            "/c",
                            "start",
                            "cmd",
                            "/k",
                            scriptFile.absolutePath
                        )
                    }
                    else -> ProcessBuilder("bash", scriptFile.absolutePath)
                }.apply {
                    redirectErrorStream(true)
                    directory(setupDir)
                }.start()
            }

            // Wait for process to complete
            val exitCode = withContext(Dispatchers.IO) { process.waitFor() }

            if (exitCode == 0) {
                views.gameWindow.alert(
                    """
                    Installation completed successfully!
                    You can now select "Local LLaMA" as your AI provider.
                    """.trimIndent()
                )
                LLMSelector.setProvider(LLMProvider.OLLAMA)
                saveProviderSelection(LLMProvider.OLLAMA)
            } else {
                views.gameWindow.alert(
                    """
                    Installation failed with error code: $exitCode
                    Please check the command window for details or try manual installation.
                    """.trimIndent()
                )
            }
        } catch (e: Exception) {
            Logger.error("Installation failed: ${e.message}")
            Logger.error("Stack trace: ${e.stackTraceToString()}")

            views.gameWindow.alert(
                """
                Installation failed: ${e.message}
                
                Please try:
                1. Running the game as administrator
                2. Checking your internet connection
                3. Following manual installation steps in SETUP_TUTORIAL.md
                
                Error details have been written to the log file.
                """.trimIndent()
            )
        }
    }

    private suspend fun uninstallOllama(platform: String) {
        try {
            val confirmed = views.gameWindow.confirm(
                """
                Are you sure you want to uninstall Ollama?
                
                This will:
                1. Remove all downloaded AI models
                2. Stop the Ollama service
                3. Remove the Ollama application
                4. Delete all related data
                
                This action cannot be undone.
                """.trimIndent()
            )

            if (!confirmed) return

            val setupDir = File("setup").apply { mkdirs() }

            val scriptName = when (platform) {
                "windows" -> "uninstall_ollama.bat"
                "mac" -> "uninstall_ollama_alt.sh"
                else -> "uninstall_ollama.sh"
            }

            val scriptFile = File(setupDir, scriptName)
            if (!scriptFile.exists()) {
                val scriptContent = when (platform) {
                    "windows" -> resourcesVfs["setup/uninstall_ollama.bat"].readString()
                    "mac" -> resourcesVfs["setup/uninstall_ollama_alt.sh"].readString()
                    else -> resourcesVfs["setup/uninstall_ollama.sh"].readString()
                }
                scriptFile.writeText(scriptContent)

                // Make script executable on Unix systems
                if (platform != "windows") {
                    withContext(Dispatchers.IO) {
                        ProcessBuilder("chmod", "+x", scriptFile.absolutePath).start().waitFor()
                    }
                }
            }

            views.gameWindow.alert("Starting Ollama uninstallation. Please wait...")

            // Execute the script with elevated privileges if needed
            val process = withContext(Dispatchers.IO) {
                when (platform) {
                    "windows" -> {
                        // For Windows, try to run with elevated privileges
                        ProcessBuilder(
                            "powershell",
                            "Start-Process",
                            "cmd",
                            "/c",
                            scriptFile.absolutePath,
                            "-Verb",
                            "RunAs",
                            "-Wait"
                        )
                    }
                    else -> {
                        // For Unix systems, use sudo if available
                        if (File("/usr/bin/sudo").exists()) {
                            ProcessBuilder("sudo", "bash", scriptFile.absolutePath)
                        } else {
                            ProcessBuilder("bash", scriptFile.absolutePath)
                        }
                    }
                }.apply {
                    redirectErrorStream(true)
                    directory(setupDir)
                }.start()
            }

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (withContext(Dispatchers.IO) { reader.readLine() }.also { line = it } != null) {
                Logger.debug("Uninstallation output: $line")
            }

            val exitCode = withContext(Dispatchers.IO) { process.waitFor() }

            if (exitCode == 0) {
                views.gameWindow.alert(
                    """
                    Uninstallation completed successfully!
                    
                    Ollama has been removed from your system.
                    The AI provider has been reset to OpenAI.
                    """.trimIndent()
                )

                // Reset to OpenAI provider
                LLMSelector.setProvider(LLMProvider.OPENAI)
                saveProviderSelection(LLMProvider.OPENAI)
            } else {
                views.gameWindow.alert(
                    """
                    Uninstallation encountered some issues (error code: $exitCode)
                    
                    Please check the logs for details or try manual uninstallation:
                    1. Stop any running Ollama processes
                    2. Remove the Ollama application
                    3. Delete the .ollama folder in your home directory
                    """.trimIndent()
                )
            }

        } catch (e: Exception) {
            Logger.error("Uninstallation failed: ${e.message}")
            Logger.error("Stack trace: ${e.stackTraceToString()}")

            views.gameWindow.alert(
                """
                Uninstallation failed: ${e.message}
                
                Please try:
                1. Running the game as administrator
                2. Manually stopping Ollama services
                3. Using your system's built-in uninstaller
                
                Error details have been written to the log file.
                """.trimIndent()
            )
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
