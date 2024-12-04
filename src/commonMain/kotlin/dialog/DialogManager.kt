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
        
        Response Guidelines:
        use two fields to respond: "message" and "action"
        Example: 
            message: Hello, how can I help you?
            action: move to sector A
        Ps: action should have a pattern equals "move to SECTOR xxxx" or "move to COORDINATE [1.0,2.0] or shoot to player xxxx"
        If have no action, just ignore it, put blank or put a message like "action: none"
        DO NOT talk about non-existent characters, items, or locations.
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
        // Use regex patterns to extract "message" and "action" fields
        val messagePattern = """(?i)message\s*:\s*(.+)""".toRegex()
        val actionPattern = """(?i)action\s*:\s*(.+)""".toRegex()

        val messageMatch = messagePattern.find(response)
        val actionMatch = actionPattern.find(response)

        // Process and display the message
        if (messageMatch != null) {
            val message = messageMatch.groupValues[1].trim()
            conversationText.append("\n$currentNpcName: $message")
            dialogWindow?.updateConversation(conversationText.toString())
        } else {
            Logger.debug("No message found in response: $response")
        }

        // Process the action, if present
        if (actionMatch != null) {
            val action = actionMatch.groupValues[1].trim()
            processAction(action)
        } else {
            Logger.debug("No action found in response: $response")
        }
    }

    private fun processAction(action: String) {
        // Define patterns for the supported actions
        val moveToSectorPattern = """(?i)move to sector ([\w\s]+)""".toRegex()
        val moveToCoordinatePattern = """(?i)move to coordinate \[(\d+(\.\d+)?),\s*(\d+(\.\d+)?)\]""".toRegex() // Accepts integers and doubles
        //val shootToPlayerPattern = """(?i)shoot to player \[(\d+\.\d+),(\d+\.\d+)\]""".toRegex()
        // Match and process actions
        when {
            moveToSectorPattern.matches(action) -> {
                val match = moveToSectorPattern.find(action)!!
                val sector = match.groupValues[1]
                Logger.debug("Detected movement command to sector: $sector")
                // Pass sector as subject
                actionModel.executeAction("MOVE", currentNpcName, sector, null, null)
            }
            moveToCoordinatePattern.matches(action) -> {
                val match = moveToCoordinatePattern.find(action)!!
                val x = match.groupValues[1].toDouble()
                val y = match.groupValues[3].toDouble()
                Logger.debug("Detected movement command to coordinates: x=$x, y=$y")
                actionModel.executeAction("MOVE", currentNpcName, "COORDINATE", "[$x,$y]", null)
            }
//            shootToPlayerPattern.matches(action) -> {
//                val match = shootToPlayerPattern.find(action)!!
//                val x = match.groupValues[1]
//                val y = match.groupValues[2]
//                Logger.debug("Detected shooting command to player: $player")
//                actionModel.executeAction("SHOOT", currentNpcName, "player", "[$x,$y]", null)
//            }
            action.equals("none", ignoreCase = true) -> {
                Logger.debug("No action required, 'action: none' detected.")
            }
            else -> {
                // Log unrecognized action formats
                Logger.debug("Unrecognized action format: $action")
            }
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
                Logger.debug("Actions: $actions")

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
