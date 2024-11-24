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

        fun default() = openAI()

        fun openAI() = LLMConfig(
            provider = LLMProvider.OPENAI,
            chatModel = "gpt-4o-mini",
            embeddingModel = "text-embedding-ada-002",
            embeddingDimension = 1536,
            stopWords = emptyList(),
            apiKey = {
                System.getProperty("open.api.key")
                    ?: System.getenv("OPENAI_API_KEY")
                    ?: loadFromConfigFile()
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
                val inputStream = LLMConfig::class.java.getResourceAsStream("/config.properties")
                    ?: java.io.FileInputStream("config.properties")

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
                        "system properties, or OPENAI_API_KEY environment variable."
                }
            }
            LLMProvider.OLLAMA -> {
                val ollamaHost = System.getProperty("ollama.host", OLLAMA_HOST)
                require(isOllamaAccessible(ollamaHost)) {
                    "Ollama is not accessible at $ollamaHost. Ensure Ollama is running."
                }
            }
        }
    }

    private fun isOllamaAccessible(host: String): Boolean {
        return try {
            val url = java.net.URL("$host/")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.responseCode == 200
        } catch (e: Exception) {
            println("Warning: Could not connect to Ollama at $host: ${e.message}")
            false
        }
    }
}
