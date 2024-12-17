package dialog

import agent.core.*
import agent.system.*
import ai.*
import korlibs.korge.view.*
import kotlinx.coroutines.*
import llm.*
import org.json.*
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
   
    Response Guidelines:
    - Always provide a valid JSON response.
    - If the action involves moving or shooting, either "sector" or "coordinate" must be filled.
    - Use "coordinate" for actions requiring specific locations (e.g., [x, y]).
    - If have no valid coordinate detected use "sector" to move to a specific sector.
    - If neither applies, set "sector" and "coordinate" to null.
    Respond strictly using JSON format as described below. Ensure valid JSON syntax.
    {
        "message": "Provide a clear message to the player.",
        "verb": "Specify the action verb, e.g., 'move' or 'shoot'.",
        "sector": "Provide the sector name if applicable, or leave null.",
        "coordinate": "[x, y] format for coordinates, or leave null."
    }
    Example:
    {
        "message": "Hello, how can I help you?",
        "verb": "move",
        "sector": "town",
        "coordinate": [1.0, 2.0]
    }
    
    If there is no action, set "verb", "sector", and "coordinate" to null, and provide a message only.
    Avoid mentioning non-existent characters, items, or locations.
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

    private fun processResponse(response: String) {
        try {
            val json = JSONObject(response)

            Logger.debug("Response JSON: $json")
            // Extract fields from JSON
            val message = json.optString("message", null)
            val verb = json.optString("verb", null)
            val sector = json.optString("sector", null)
            val coordinate = json.optJSONArray("coordinate")

            // Display the message
            if (!message.isNullOrEmpty()) {
                conversationText.append("\n$currentNpcName: $message")
                dialogWindow?.updateConversation(conversationText.toString())
            } else {
                Logger.debug("No message found in response: $response")
            }

            // Process the action based on JSON fields
            if (!verb.isNullOrEmpty()) {
                val actionVerb = ActionVerb.fromString(verb)
                if (actionVerb != null) {
                    processActionVerb(actionVerb, sector, coordinate)
                } else {
                    Logger.debug("Unrecognized verb: $verb")
                }
            } else {
                Logger.debug("No action verb found in response: $response")
            }
        } catch (e: JSONException) {
            Logger.error("Failed to parse response JSON: $response")
            Logger.error(e.stackTraceToString())
        }
    }

    private fun processActionVerb(actionVerb: ActionVerb, sector: String?, coordinate: JSONArray?) {
        when (actionVerb) {
            ActionVerb.MOVE -> {
                if (!sector.isNullOrEmpty()) {
                    Logger.debug("Detected movement to sector: $sector")
                    actionModel.executeAction(ActionVerb.MOVE, currentNpcName, sector, null, null)
                } else if (coordinate != null && coordinate.length() == 2) {
                    val x = coordinate.getDouble(0)
                    val y = coordinate.getDouble(1)
                    Logger.debug("Detected movement to coordinates: [$x, $y]")
                    actionModel.executeAction(ActionVerb.MOVE, currentNpcName, "COORDINATE", null, "[$x,$y]")
                } else {
                    Logger.debug("Move action specified but no valid sector or coordinates found.")
                }
            }

            ActionVerb.SHOOT -> {
                if (coordinate != null && coordinate.length() == 2) {
                    val x = coordinate.getDouble(0)
                    val y = coordinate.getDouble(1)
                    Logger.debug("Detected shooting action at coordinates: [$x, $y]")
                    actionModel.executeAction(ActionVerb.SHOOT, currentNpcName, "COORDINATE", null, "[$x,$y]")
                } else {
                    Logger.debug("Shoot action specified but no valid coordinates found.")
                }
            }

            ActionVerb.GIVE -> TODO()
            ActionVerb.TAKE -> TODO()
            ActionVerb.INTERACT -> TODO()
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

                actionModel.processNPCReflection(actions.joinToString("\n"), currentNpcName)
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
