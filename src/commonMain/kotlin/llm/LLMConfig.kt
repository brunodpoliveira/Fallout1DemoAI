package llm

data class LLMConfig(
    val provider: LLMProvider,
    val chatModel: String,
    val embeddingModel: String,
    val embeddingDimension: Int,
    val stopWords: List<String>,
    val apiKey: () -> String?
) {
    companion object {
        private const val OLLAMA_HOST = "http://127.0.0.1:11434"

        // Default OpenAI configuration
        fun default() = openAI()

        fun openAI() = LLMConfig(
            provider = LLMProvider.OPENAI,
            chatModel = "gpt-4o-mini",
            embeddingModel = "text-embedding-ada-002",
            embeddingDimension = 1536,
            stopWords = emptyList(),
            apiKey = {
                // Try multiple methods to get the API key
                System.getProperty("open.api.key")
                    ?: loadFromConfigFile()
                    ?: System.getenv("OPENAI_API_KEY")
            }
        )

        fun ollama() = LLMConfig(
            provider = LLMProvider.OLLAMA,
            chatModel = "llama3",
            embeddingModel = "mxbai-embed-large",
            embeddingDimension = 1024,
            stopWords = listOf("<|eot_id|>"),
            apiKey = {
                System.setProperty("ollama.host", OLLAMA_HOST)
                null
            }
        )

        private fun loadFromConfigFile(): String? {
            return try {
                val properties = java.util.Properties()
                // Try loading from classpath first
                val inputStream = LLMConfig::class.java.getResourceAsStream("/config.properties")
                    ?: // If not found in classpath, try loading from current directory
                    java.io.FileInputStream("config.properties")

                properties.load(inputStream)
                inputStream.close()
                properties.getProperty("open.api.key")
            } catch (e: Exception) {
                println("Warning: Could not load API key from config.properties: ${e.message}")
                null
            }
        }
    }

    fun validate() {
        when (provider) {
            LLMProvider.OPENAI -> {
                requireNotNull(apiKey()) {
                    "OpenAI API key is required. Set 'open.api.key' in config.properties, " +
                        "system properties, or OPENAI_API_KEY environment variable"
                }
            }
            LLMProvider.OLLAMA -> {
                // Ensure Ollama is running and accessible
                val ollamaHost = System.getProperty("ollama.host", "http://localhost:11434")
                require(isOllamaAccessible(ollamaHost)) {
                    "Ollama is not accessible at $ollamaHost. Ensure Ollama is running."
                }
            }
        }
    }

    private fun isOllamaAccessible(host: String): Boolean {
        return try {
            java.net.URL("$host/").readText()
            true
        } catch (e: Exception) {
            false
        }
    }
}
