package dialog

import ai.*
import korlibs.korge.view.*
import kotlinx.coroutines.*
import llm.*
import ui.*
import utils.*

class InterrogationManager(
    private val coroutineScope: CoroutineScope,
    private val container: Container,
    private val llmService: LLMService
) {
    private var window: InterrogationWindow? = null

    fun showInterrogation(npcs: List<String>) {
        if (window == null) {
            window = InterrogationWindow(::handleQuestion)
            window?.show(container, npcs)
        }
    }

    suspend fun handleQuestion(npcName: String, question: String): String {
        val systemContext = """
            You are $npcName.
            General Context: ${Director.getContext()}
            NPC Context: ${Director.getNPCContext(npcName)}
            Answer questions in-character as $npcName.
            Keep responses concise but informative.
        """.trimIndent()

        val messages = listOf(
            SystemMessage(systemContext),
            UserMessage(question)
        )

        return try {
            llmService.chat(messages)
        } catch (e: Exception) {
            Logger.error("Failed to get response from LLM: ${e.message}")
            throw e
        }
    }
}
