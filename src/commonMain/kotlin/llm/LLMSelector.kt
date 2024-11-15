package llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import llm.impl.*
import utils.Logger
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper class for LLM provider selection in game setup
 * TODO OLLAMA downloader and installer in-game
 */
object LLMSelector {
    private var selectedProvider = LLMProvider.OPENAI

    fun setProvider(provider: LLMProvider) {
        Logger.debug("Setting LLM provider to: $provider")
        selectedProvider = provider
    }

    suspend fun selectProvider(): LLMConfig {
        Logger.debug("LLMSelector.selectProvider() called with selectedProvider: $selectedProvider")

        return when (selectedProvider) {
            LLMProvider.OLLAMA -> {
                Logger.debug("Attempting to use Local LLaMA")
                try {
                    val ollamaStatus = checkOllamaStatus()
                    if (!ollamaStatus.isRunning) {
                        Logger.error("OLLAMA service check failed: ${ollamaStatus.errorDetails}")
                        Logger.debug("Falling back to OpenAI due to OLLAMA service issues")
                        return LLMConfig.openAI()
                    }

                    val ollamaConfig = LLMConfig.ollama()
                    val ollamaService = OllamaService(ollamaConfig)

                    withContext(Dispatchers.IO) {
                        try {
                            Logger.debug("Attempting to pull model: ${ollamaConfig.chatModel}")
                            ollamaService.pullModel(ollamaConfig.chatModel)
                            Logger.debug("Successfully pulled chat model")

                            Logger.debug("Attempting to pull model: ${ollamaConfig.embeddingModel}")
                            ollamaService.pullModel(ollamaConfig.embeddingModel)
                            Logger.debug("Successfully pulled embedding model")
                        } catch (e: Exception) {
                            Logger.error("Failed to pull OLLAMA models: ${e.message}")
                            Logger.error("Stack trace: ${e.stackTraceToString()}")
                            throw e
                        }
                    }

                    Logger.debug("Successfully initialized OLLAMA")
                    ollamaConfig
                } catch (e: Exception) {
                    Logger.error("Failed to initialize LLaMA: ${e.message}")
                    Logger.error("Full stack trace: ${e.stackTraceToString()}")
                    Logger.debug("System info: ${getSystemInfo()}")
                    Logger.debug("Falling back to OpenAI")
                    LLMConfig.openAI()
                }
            }
            LLMProvider.OPENAI -> {
                Logger.debug("Using OpenAI")
                LLMConfig.openAI()
            }
        }
    }

    private suspend fun checkOllamaStatus(): OllamaStatus = withContext(Dispatchers.IO) {
        try {
            val ollamaHost = System.getProperty("ollama.host", "http://127.0.0.1:11434")
            val url = URL("$ollamaHost/")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try {
                connection.connect()
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    OllamaStatus(true)
                } else {
                    OllamaStatus(false, "Unexpected response code: $responseCode")
                }
            } catch (e: Exception) {
                OllamaStatus(false, "Connection failed: ${e.message}")
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            OllamaStatus(false, "Failed to create connection: ${e.message}")
        }
    }

    private fun getSystemInfo(): String {
        return buildString {
            append("OS: ${System.getProperty("os.name")} ")
            append("Version: ${System.getProperty("os.version")} ")
            append("Arch: ${System.getProperty("os.arch")}\n")
            append("Java Version: ${System.getProperty("java.version")}\n")
            append("Available processors: ${Runtime.getRuntime().availableProcessors()}\n")
            append("Max Memory: ${Runtime.getRuntime().maxMemory() / 1024 / 1024}MB\n")
            append("Free Memory: ${Runtime.getRuntime().freeMemory() / 1024 / 1024}MB\n")
            append("Total Memory: ${Runtime.getRuntime().totalMemory() / 1024 / 1024}MB")
        }
    }
}

data class OllamaStatus(
    val isRunning: Boolean,
    val errorDetails: String? = null
)
