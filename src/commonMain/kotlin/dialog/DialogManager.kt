package dialog

import agent.core.*
import agent.system.*
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
    private var currentInteraction: AgentInteractionManager.Interaction? = null

    val isInDialog: Boolean get() = _isInDialog
    private var _isInDialog = false

    // Conversation state
    private val conversationMessages = mutableListOf<LLMMessage>()
    private val conversationText = StringBuilder()

    fun showDialog(interaction: AgentInteractionManager.Interaction) {
        if (isInDialog || cooldownActive) return

        currentInteraction = interaction
        val target = interaction.target

        if (dialogWindow == null) {
            dialogWindow = DialogWindow().apply {
                onMessageSend = { message -> handleMessage(message) }
                onClose = { runBlocking { handleClose() }  }
            }
        }

        _isInDialog = true
        GameState.isDialogOpen = true

        dialogWindow?.apply {
            clear()
            showLoading()
        }

        container.addChild(dialogWindow!!)

        coroutineScope.launch {
            startNewConversation()
        }
    }

    private suspend fun startNewConversation() {
        val target = currentInteraction?.target ?: return
        val systemContext = """
            Bio: ${NPCBio.getBioForNPC(target.name)}
            General Context: ${Director.getContext()}
            Faction Context: ${Director.getFactionContext(target.faction)}
            NPC Context: ${Director.getNPCContext(target.name)}
            
            You are ${target.name}. Greet the player and introduce yourself naturally.
            Stay in character and respond as your character would.
        """.trimIndent()

        conversationMessages.clear()
        conversationText.clear()

        conversationMessages.add(SystemMessage(systemContext))

        try {
            val response = llmService.chat(conversationMessages)
            conversationMessages.add(AssistantMessage(response))

            conversationText.append("\n${target.name}: $response")
            dialogWindow?.updateConversation(conversationText.toString())
            dialogWindow?.hideLoading()
        } catch (e: Exception) {
            Logger.error("Error starting conversation: ${e.message}")
            handleError("Failed to start conversation")
        }
    }

    private fun handleMessage(message: String) {
        coroutineScope.launch {
            dialogWindow?.showLoading()
            val target = currentInteraction?.target ?: return@launch

            conversationMessages.add(UserMessage(message))
            conversationText.append("\nPlayer: $message")

            try {
                val response = llmService.chat(conversationMessages)
                conversationMessages.add(AssistantMessage(response))

                conversationText.append("\n${target.name}: $response")
                dialogWindow?.updateConversation(conversationText.toString())
            } catch (e: Exception) {
                Logger.error("Error in conversation: ${e.message}")
                handleError("Failed to get response")
            }

            dialogWindow?.hideLoading()
        }
    }

    private suspend fun processAgentOutput(output: AgentOutput) {
        when (output.decision) {
            is Decision.Accept -> {
                output.actions.forEach { action ->
                    when (action) {
                        is AgentAction.Speak -> {
                            conversationText.append("\n${currentInteraction?.target?.name}: ${action.message}")
                            dialogWindow?.updateConversation(conversationText.toString())
                        }

                        is AgentAction.Move -> actionModel.executeAction(
                            "MOVE",
                            currentInteraction?.target?.id ?: return,
                            "COORDINATE",
                            null,
                            "[${action.destination.x},${action.destination.y}]"
                        )

                        is AgentAction.GiveItem -> actionModel.executeAction(
                            "GIVE",
                            currentInteraction?.target?.id ?: return,
                            action.targetId,
                            action.itemId,
                            null
                        )

                        is AgentAction.TakeItem -> actionModel.executeAction(
                            "TAKE",
                            currentInteraction?.target?.id ?: return,
                            action.targetId,
                            action.itemId,
                            null
                        )

                        else -> Logger.debug("Skipping action: $action")
                    }
                }
            }

            else -> handleClose()
        }
    }

    private suspend fun handleClose() {
        coroutineScope.launch {
            dialogWindow?.showLoading()

            try {
                val processor = ConversationPostProcessingServices(actionModel, llmService)
                val target = currentInteraction?.target ?: return@launch

                val (updatedBio, metadataInfo, actions) = processor.conversationPostProcessingLoop(
                    conversationText.toString(),
                    NPCBio.getBioForNPC(target.name),
                    target.name
                )

                Director.updateNPCContext(
                    target.name,
                    updatedBio,
                    metadataInfo.hasSecret,
                    metadataInfo.conspirators
                )
            } catch (e: Exception) {
                Logger.error("Error in conversation post-processing: ${e.message}")
            }

            cleanupDialog()

            cooldownActive = true
            delay(5000)
            cooldownActive = false
        }
    }

    private fun handleError(message: String) {
        dialogWindow?.updateConversation("$conversationText\n[Error: $message]")
    }

    private fun cleanupDialog() {
        _isInDialog = false
        GameState.isDialogOpen = false
        conversationMessages.clear()
        conversationText.clear()
        currentInteraction = null

        dialogWindow?.also { window ->
            window.clear()
            window.parent?.removeChild(window)
        }
    }
}
