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

    private val conversationMessages = mutableListOf<LLMMessage>()
    private val conversationText = StringBuilder()

    fun showDialog(interaction: AgentInteractionManager.Interaction) {
        if (isInDialog || cooldownActive) return
        currentInteraction = interaction

        dialogWindow = dialogWindow ?: DialogWindow().apply {
            onMessageSend = { message -> coroutineScope.launch {
                handleMessage(message) }
            }
            onClose = { handleClose() }
        }

        _isInDialog = true
        GameState.isDialogOpen = true

        dialogWindow?.apply {
            clear()
            showLoading()
        }

        container.addChild(dialogWindow!!)

        coroutineScope.launch {
            startConversation()
        }
    }

    private suspend fun startConversation() {
        val interaction = currentInteraction ?: return
        val target = interaction.target

        val systemContext = """
        Bio: ${target.id}
        General Context: ${Director.getContext()}
        Faction Context: ${Director.getFactionContext(target.faction)}
        NPC Context: ${Director.getNPCContext(target.name)}
        """.trimIndent()

        conversationMessages.clear()
        conversationText.clear()

        conversationMessages.add(SystemMessage(systemContext))

        val initialInput = AgentInput.StartConversation(
            targetId = interaction.initiator.id,
            message = "Hello"
        )

        try {
            val output = target.processInput(initialInput)
            processAgentOutput(output)
            dialogWindow?.hideLoading()
        } catch (e: Exception) {
            Logger.error("Error starting conversation: ${e.message}")
            handleError("Failed to start conversation")
        }
    }

    private suspend fun handleMessage(message: String) {
        val interaction = currentInteraction ?: return

        dialogWindow?.showLoading()
        conversationText.append("\nPlayer: $message")

        try {
            val input = AgentInput.ReceiveMessage(
                fromId = interaction.initiator.id,
                message = message
            )

            val output = interaction.target.processInput(input)
            processAgentOutput(output)
        } catch (e: Exception) {
            Logger.error("Error in conversation: ${e.message}")
            handleError("Failed to get response")
        }

        dialogWindow?.hideLoading()
    }

    private fun processAgentOutput(output: AgentOutput) {
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

    private fun handleClose() {
        coroutineScope.launch {
            dialogWindow?.showLoading()

            try {
                currentInteraction?.let { interaction ->
                    val processor = ConversationPostProcessingServices(actionModel, llmService)
                    val (updatedBio, metadataInfo, actions) = processor.conversationPostProcessingLoop(
                        conversationText.toString(),
                        "Agent conversation summary",
                        interaction.target.name
                    )

                    Director.updateNPCContext(
                        interaction.target.name,
                        updatedBio,
                        metadataInfo.hasSecret,
                        metadataInfo.conspirators
                    )

                    actions.forEach { actionStr ->
                        val parts = actionStr.split(",")
                        if (parts.size >= 3) {
                            actionModel.executeAction(
                                parts[0],
                                interaction.target.id,
                                parts[2],
                                parts.getOrNull(3),
                                parts.getOrNull(4)
                            )
                        }
                    }
                }
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
