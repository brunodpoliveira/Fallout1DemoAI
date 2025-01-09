package scenes

import ai.*
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

    companion object {
        /**
         * Retrieves the current LLM provider based on the configuration.
         */
        fun getCurrentProvider(): LLMProvider {
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

    override suspend fun SContainer.sceneMain() {
        uiVerticalStack(padding = 4.0) {
            centerOnStage()

            uiText("Options Menu") {
                centerXOnStage()
            }
            uiComboBox(
                size = Size(160f, 32f),
                items = listOf("OpenAI (Online)", "LLaMA (Local)"),
                selectedIndex = when (getCurrentProvider()) {
                    LLMProvider.OPENAI -> 0
                    LLMProvider.OLLAMA -> 1
                }
            ).apply {
                onSelectionUpdate { selection ->
                    Logger.debug("Dropdown selection updated: ${selection.selectedItem}")

                    val selectedProvider = when (selection.selectedIndex) {
                        0 -> LLMProvider.OPENAI
                        else -> LLMProvider.OLLAMA
                    }

                    Logger.debug("User selected provider: $selectedProvider")

                    if (selectedProvider == LLMProvider.OLLAMA) {
                        if (!isOllamaInstalled()) {
                            Logger.debug("OLLAMA not installed. Showing installation prompt.")
                            sceneContainer.launch { showOllamaInstallPrompt() }
                            return@onSelectionUpdate
                        }

                        Logger.debug("OLLAMA installed. Proceeding with local mode warning.")
                        sceneContainer.launch { showLocalModeWarning() }
                    }

                   // LLMSelector.setProvider(selectedProvider)
                    LLMSelector.setProvider(LLMProvider.OLLAMA)
                    saveProviderSelection(selectedProvider)
                    Logger.debug("Provider successfully switched to $selectedProvider")
                }
            }

            when {
                Platform.isWindows -> {
                    uiVerticalStack {
                        uiButton("One-Click Install (Windows)").also {
                            it.width = 210.0
                            onClick {
                                sceneContainer.launch {
                                    installOllama("windows")
                                }
                            }
                        }
                    }
                    uiVerticalStack {
                        uiButton("Uninstall (Windows)").also {
                            it.width = 210.0
                            onClick {
                                sceneContainer.launch {
                                    uninstallOllama("windows")
                                }
                            }
                        }
                    }
                }
                Platform.isMac -> {
                    uiVerticalStack {
                        uiButton("One-Click Install (macOS)").also {
                            it.width = 210.0
                            onClick {
                                sceneContainer.launch {
                                    installOllama("mac")
                                }
                            }
                        }
                    }
                    uiVerticalStack {
                        uiButton("Uninstall (macOS)").also {
                            it.width = 210.0
                            onClick {
                                sceneContainer.launch {
                                    uninstallOllama("mac")
                                }
                            }
                        }
                    }
                }
                Platform.isLinux -> {
                    uiVerticalStack {
                        uiButton("One-Click Install (Linux)").also {
                            it.width = 210.0
                            onClick {
                                sceneContainer.launch {
                                    installOllama("linux")
                                }
                            }
                        }
                    }
                    uiVerticalStack {
                        uiButton("Uninstall (Linux)").also {
                            it.width = 210.0
                            onClick {
                                sceneContainer.launch {
                                    uninstallOllama("linux")
                                }
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
            val setupDirMacOs = File("src/commonMain/resources/setup").canonicalFile

            val scriptFile = when (platform) {
                "windows" -> File(setupDir, "install_ollama_win.bat")
                "mac" -> File(setupDirMacOs, "install_ollama_mac.sh")
                else -> File(setupDir, "install_ollama_unix.sh")
            }

            if (platform != "windows") {
                withContext(Dispatchers.IO) {
                    ProcessBuilder("chmod", "+x", scriptFile.absolutePath)
                        .directory(setupDir)
                        .start()
                        .waitFor()
                }
            }

            views.gameWindow.alert(
                """
            Starting Ollama installation. This may take several minutes.
            A new terminal window will open to show installation progress.
            
            Required steps:
            1. Download and install Ollama
            2. Download required AI models
            3. Configure the service
            """.trimIndent()
            )

            withContext(Dispatchers.IO) {
                when (platform) {
                    "windows" -> {
                        ProcessBuilder(
                            "cmd",
                            "/c",
                            "start",
                            "cmd",
                            "/k",
                            scriptFile.absolutePath
                        ).directory(setupDir).start()
                    }
                    "mac" -> {
                        ProcessBuilder(
                            "/bin/bash", "-c",
                            "osascript -e 'tell application \"Terminal\" to do script \"cd ${setupDirMacOs.canonicalPath} && ./install_ollama_mac.sh\"'"
                        ).directory(setupDir).start()
                    }
                    else -> {
                        // Try different terminal emulators in order of commonality
                        val terminals = listOf(
                            arrayOf("gnome-terminal", "--", "bash", scriptFile.absolutePath),  // Standard Ubuntu/GNOME
                            arrayOf("x-terminal-emulator", "-e", "bash ${scriptFile.absolutePath}"),  // Debian-based systems
                            arrayOf("xfce4-terminal", "--command", "bash ${scriptFile.absolutePath}"), // XFCE
                            arrayOf("konsole", "--e", "bash ${scriptFile.absolutePath}"),  // KDE
                            arrayOf("mate-terminal", "--command", "bash ${scriptFile.absolutePath}"),  // MATE
                            arrayOf("xterm", "-e", "bash ${scriptFile.absolutePath}")  // Basic fallback
                        )

                        var success = false
                        var lastException: Exception? = null

                        for (terminal in terminals) {
                            try {
                                ProcessBuilder(*terminal)
                                    .directory(setupDir)
                                    .start()
                                success = true
                                break
                            } catch (e: Exception) {
                                lastException = e
                            }
                        }

                        if (success) {
                            Unit
                        } else {
                            throw lastException ?: Exception("No suitable terminal emulator found")
                        }
                    }
                }
            }


        } catch (e: Exception) {
            Logger.error("Installation failed: ${e.message}")
            Logger.error("Stack trace: ${e.stackTraceToString()}")

            views.gameWindow.alert(
                """
            Installation failed: ${e.message}
            
            Please try:
            1. Opening a terminal manually
            2. Navigating to the game's 'setup' directory
            3. Running: chmod +x install_ollama_unix.sh
            4. Running: sudo ./install_ollama_unix.sh
            
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
            val setupDirMacOs = File("src/commonMain/resources/setup").canonicalFile

            val scriptFile = when (platform) {
                "windows" -> File(setupDir, "uninstall_ollama_win.bat")
                "mac" -> File(setupDirMacOs, "uninstall_ollama_mac.sh")
                else -> File(setupDir, "uninstall_ollama_unix.sh")
            }


            if (platform != "windows") {
                withContext(Dispatchers.IO) {
                    ProcessBuilder("chmod", "+x", scriptFile.absolutePath)
                        .directory(setupDir)
                        .start()
                        .waitFor()
                }
            }

            withContext(Dispatchers.IO) {
                when (platform) {
                    "windows" -> {
                        ProcessBuilder(
                            "cmd",
                            "/c",
                            "start",
                            "cmd",
                            "/k",
                            scriptFile.absolutePath
                        ).directory(setupDir).start()
                    }
                    "mac" -> {
                        ProcessBuilder(
                            "/bin/bash", "-c",
                            "osascript -e 'tell application \"Terminal\" to do script \"cd ${setupDirMacOs.canonicalPath} && ./uninstall_ollama_mac.sh\"'"
                        ).directory(setupDir).start()
                    }
                    else -> {
                        val terminals = listOf(
                            arrayOf("gnome-terminal", "--", "bash", scriptFile.absolutePath),  // Standard Ubuntu/GNOME
                            arrayOf("x-terminal-emulator", "-e", "bash ${scriptFile.absolutePath}"),  // Debian-based systems
                            arrayOf("xfce4-terminal", "--command", "bash ${scriptFile.absolutePath}"), // XFCE
                            arrayOf("konsole", "--e", "bash ${scriptFile.absolutePath}"),  // KDE
                            arrayOf("mate-terminal", "--command", "bash ${scriptFile.absolutePath}"),  // MATE
                            arrayOf("xterm", "-e", "bash ${scriptFile.absolutePath}")  // Basic fallback
                        )

                        var success = false
                        var lastException: Exception? = null

                        for (terminal in terminals) {
                            try {
                                ProcessBuilder(*terminal)
                                    .directory(setupDir)
                                    .start()
                                success = true
                                break
                            } catch (e: Exception) {
                                lastException = e
                            }
                        }

                        if (success) {
                            Unit
                        } else {
                            throw lastException ?: Exception("No suitable terminal emulator found")
                        }
                    }
                }
            }

            views.gameWindow.alert(
                """
            Uninstallation process has started in a new terminal window.
            Please follow the prompts in that window.
            
            The game will reset to using OpenAI after the uninstallation is complete.
            """.trimIndent()
            )

            // Reset to OpenAI provider
            LLMSelector.setProvider(LLMProvider.OPENAI)
            saveProviderSelection(LLMProvider.OPENAI)

        } catch (e: Exception) {
            Logger.error("Uninstallation failed: ${e.message}")
            Logger.error("Stack trace: ${e.stackTraceToString()}")

            views.gameWindow.alert(
                """
            Failed to start uninstallation: ${e.message}
            
            Please try:
            1. Opening a terminal manually
            2. Navigating to the game's 'setup' directory
            3. Running: chmod +x uninstall_ollama_unix.sh
            4. Running: sudo ./uninstall_ollama_unix.sh
            
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
            * Graphics Card (GPU): NVIDIA with CUDA support or AMD Radeon, 8GB or more of VRAM (NVIDIA RTX 3000 series or better)
            * Storage: At least 8GB of free space
            
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


}
