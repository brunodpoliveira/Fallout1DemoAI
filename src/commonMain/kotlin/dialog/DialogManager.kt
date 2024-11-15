package dialog

import ai.*
import korlibs.korge.view.*
import kotlinx.coroutines.*
import llm.*
import ui.*
import utils.*

class DialogManager(
    private val coroutineScope: CoroutineScope,
    private val container: Container,
    private val actionModel: ActionModel,
    private val llmService: LLMService
) {
    private var dialogWindow: DialogWindow? = null
    private var cooldownActive = false

    val isInDialog: Boolean get() = _isInDialog
    private var _isInDialog = false

    // Conversation state
    private val conversationMessages = mutableListOf<LLMMessage>()
    private val conversationText = StringBuilder()

    // Current conversation context
    private lateinit var currentNpcName: String
    private lateinit var currentNpcBio: String
    private lateinit var currentFactionName: String

    fun showDialog(npcName: String, npcBio: String, factionName: String) {
        if (isInDialog || cooldownActive) return

        // Store conversation context
        currentNpcName = npcName
        currentNpcBio = npcBio
        currentFactionName = factionName

        if (dialogWindow == null) {
            dialogWindow = DialogWindow().apply {
                onMessageSend = { message -> handleMessage(message) }
                onClose = { handleClose() }
            }
        }

        _isInDialog  = true
        GameState.isDialogOpen = true

        dialogWindow?.apply {
            clear()
            showLoading()
        }

        container.addChild(dialogWindow!!)

        // Start the conversation
        coroutineScope.launch {
            startConversation()
        }
    }

    private suspend fun startConversation() {
        // Initialize conversation with system context
        val systemContext = """
            Bio: $currentNpcBio
            General Context: ${Director.getContext()}
            Faction Context: ${Director.getFactionContext(currentFactionName)}
            NPC Context: ${Director.getNPCContext(currentNpcName)}
            DO NOT talk about non-existent characters, items, and locations
        """.trimIndent()

        conversationMessages.clear()
        conversationText.clear()

        conversationMessages.add(SystemMessage(systemContext))
        conversationMessages.add(UserMessage("Hi"))

        try {
            val response = llmService.chat(conversationMessages)
            conversationMessages.add(AssistantMessage(response))

            conversationText.append("${currentNpcName}: $response")
            dialogWindow?.updateConversation(conversationText.toString())
        } catch (e: Exception) {
            Logger.error("Error starting conversation: ${e.message}")
            handleError("Failed to start conversation")
        }

        dialogWindow?.hideLoading()
    }

    private fun handleMessage(message: String) {
        coroutineScope.launch {
            dialogWindow?.showLoading()

            conversationMessages.add(UserMessage(message))
            conversationText.append("\nPlayer: $message")

            try {
                val response = llmService.chat(conversationMessages)
                conversationMessages.add(AssistantMessage(response))

                conversationText.append("\n$currentNpcName: $response")
                dialogWindow?.updateConversation(conversationText.toString())
            } catch (e: Exception) {
                Logger.error("Error in conversation: ${e.message}")
                handleError("Failed to get response")
            }

            dialogWindow?.hideLoading()
        }
    }

    private fun handleClose() {
        coroutineScope.launch {
            dialogWindow?.showLoading()

            try {
                // Process conversation before closing
                val processor = ConversationPostProcessingServices(actionModel, llmService)
                val (updatedBio, metadataInfo, actions) = processor.conversationPostProcessingLoop(
                    conversationText.toString(),
                    currentNpcBio,
                    currentNpcName
                )

                Director.updateNPCContext(
                    currentNpcName,
                    updatedBio,
                    metadataInfo.hasSecret,
                    metadataInfo.conspirators
                )

                actionModel.processNPCReflection(actions.joinToString("\n"), currentNpcName)
            } catch (e: Exception) {
                Logger.error("Error in conversation post-processing: ${e.message}")
            }

            // Clean up
            cleanupDialog()

            // Start cooldown
            cooldownActive = true
            delay(5000)
            cooldownActive = false
        }
    }

    private fun handleError(message: String) {
        dialogWindow?.updateConversation("$conversationText\n[Error: $message]")
    }

    private fun cleanupDialog() {
        _isInDialog  = false
        GameState.isDialogOpen = false
        conversationMessages.clear()
        conversationText.clear()

        dialogWindow?.also { window ->
            window.clear()
            window.parent?.removeChild(window)
        }
    }
}
