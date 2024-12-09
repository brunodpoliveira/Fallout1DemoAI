package dialog

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
        conversationMessages.add(UserMessage("Hi"))

        try {
            val response = llmService.chat(conversationMessages)
            conversationMessages.add(AssistantMessage(response))

            processResponse(response)

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

            conversationMessages.add(UserMessage(message))
            conversationText.append("\nPlayer: $message")

            try {
                val response = llmService.chat(conversationMessages)
                conversationMessages.add(AssistantMessage(response))

                processResponse(response)
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
