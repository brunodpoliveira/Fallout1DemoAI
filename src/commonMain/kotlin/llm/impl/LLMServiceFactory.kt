package llm.impl

import llm.*

object LLMServiceFactory {
    fun create(config: LLMConfig = LLMConfig.default()): LLMService {
        return when (config.provider) {
            LLMProvider.OPENAI -> OpenAIService(config)
            LLMProvider.OLLAMA -> OllamaService(config)
        }
    }
}
