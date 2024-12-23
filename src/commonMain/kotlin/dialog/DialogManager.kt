package dialog

import agent.core.*
import agent.system.*
import ai.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import llm.*
import org.json.*
import ui.*
import utils.*

class DialogManager(
    private val coroutineScope: CoroutineScope,
    private val container: Container,
    private val actionModel: ActionModel,
    private val llmService: LLMService,
    private val ldtkWorld: LDTKWorld
) {
    private var agentMovement:AgentMovement? = null
    private var dialogWindow: DialogWindow? = null
    private var cooldownActive = false
    private var currentInteraction: AgentInteractionManager.Interaction? = null

    val isInDialog: Boolean get() = _isInDialog
    private var _isInDialog = false

    private lateinit var currentNpcName: String
    // Conversation state
    private val conversationMessages = mutableListOf<LLMMessage>()
    private val conversationText = StringBuilder()
    private val responseGuidelines = "Response Guidelines:\n" +
        "        - ALWAYS PROVIDE a valid JSON response, responding strictly using JSON format as described.\n" +
        "        - If the have an action, only these fields should be filled \"sector\" or \"coordinate\" or \"target\"" +
        "        - Target is a npc name, provide from available NPC name list.\n" +
        "        - Use \"coordinate\" for actions requiring specific locations (e.g., [x, y]).\n" +
        "         \n" +
        "    {\n" +
        "        \"message\": \"Provide a clear message to the player.\",\n" +
        "        \"accept\" : \"Provide a boolean value to accept or reject the action.\",\n" +
        "        \"verb\": \"Specify the action verb using a Available Actions Verb, if applicable, or leave null\",\n" +
        "        \"sector\": \"Specify the sector name using a Available Sectors name, or leave null.\",\n" +
        "        \"coordinate\": \"[x, y] format for coordinates, or leave null.\",\n" +
        "        \"target\": \"Provide the target npc name if applicable, or leave null.\"\n" +
        "    }\n" +
        "    Example:\n" +
        "    {\n" +
        "        \"message\": \"Hello, how can I help you?\",\n" +
        "        \"accept\" : true,\n" +
        "        \"verb\": \"move\",\n" +
        "        \"sector\": \"sector name\",\n" +
        "        \"coordinate\": [1.0, 2.0],\n" +
        "        \"target\": \"npc name\"\n" +
        "    }\n" +
        "    Avoid mentioning non-existent characters, items, or locations.\n"

    fun showDialog(interaction: AgentInteractionManager.Interaction) {
        if (isInDialog || cooldownActive) return

        currentInteraction = interaction
        val target = interaction.target

        if (dialogWindow == null) {
            dialogWindow = DialogWindow().apply {
                onMessageSend = { message -> handleMessage(message) }
                onClose = { runBlocking { handleClose() } }
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

        currentNpcName = target.name
        val systemContext = """
            Bio: ${NPCBio.getBioForNPC(target.name)}
            General Context: ${Director.getContext()}
            Faction Context: ${Director.getFactionContext(target.faction)}
            NPC Context: ${Director.getNPCContext(target.name)}
            Available Actions Verb: ${Director.getAvailableActionVerb()}
            Available NPC's name : ${Director.getAllNPCNames()} 
            Available Sectors name:  ${Director.getAvailableSectorNames(ldtkWorld)}
            You are ${target.name}. Greet the player and introduce yourself naturally.
            Stay in character and respond as your character would.
            Follow Strictily response guidelines: $responseGuidelines
            
    """.trimIndent()

        conversationMessages.clear()
        conversationText.clear()

        conversationMessages.add(SystemMessage(systemContext))

        try {
            val response = llmService.chat(conversationMessages)
            Logger.debug("Response : $response")

            val json = extractJson(response)
            Logger.debug("Response JSON: $json")

            val message = json.optString("message", null)

            conversationMessages.add(AssistantMessage(message))

            conversationText.append("\n${target.name}: $message")
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

            val contextWithGuidelines = """
            $message
            
            Follow Strictly Response Guidelines:
            $responseGuidelines
        """.trimIndent()

            try {
                val response = llmService.chat(conversationMessages + SystemMessage(contextWithGuidelines))

                Logger.debug("Raw Response: $response")

                val json = extractJson(response)
                Logger.debug("Parsed JSON: $json")

                val messageResponse = json.optString("message", null)
                val accept = json.optBoolean("accept", false)

                conversationMessages.add(AssistantMessage(messageResponse))
                conversationText.append("\n${target.name}: $messageResponse")
                dialogWindow?.updateConversation(conversationText.toString())

                if (accept) {
                    processAgentOutput(json)
                } else {
                    Decision.Reject("Action not accepted")
                }

            } catch (e: Exception) {
                Logger.error("Error in conversation: ${e.message}")
                handleError("Failed to get response")
            }

            dialogWindow?.hideLoading()
        }
    }

    private fun extractJson(response: String): JSONObject {
        val jsonStartIndex = response.indexOf('{')
        if (jsonStartIndex == -1) throw IllegalArgumentException("No JSON found in response")
        response.substring(jsonStartIndex)
        return JSONObject(response.substring(jsonStartIndex))

    }

    private fun processAgentOutput(json: JSONObject) {

        val verb = json.optString("verb", null)
        val sector = json.optString("sector", null)
        val target = json.optString("target", null)
        val coordinate = json.optJSONArray("coordinate")
        Logger.debug("Coordinate : $coordinate")

        if (!verb.isNullOrEmpty()) {
            val actionVerb = ActionVerb.fromString(verb)
            if (actionVerb != null) {
                processActionVerb(actionVerb, sector, coordinate, target)
            } else {
                Logger.debug("Unrecognized verb: $verb")
            }
        } else {
            Logger.debug("No action verb found in response: $json")
        }
    }

    private fun processActionVerb(actionVerb: ActionVerb, sector: String?, coordinate: JSONArray?, target: String?) {
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
                } else if(!target.isNullOrEmpty()){
                    Logger.debug("Detected movement to target: $target")
                    actionModel.executeAction(ActionVerb.MOVE, currentNpcName, "NPC", null, target)
                }else {
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
    private fun handleClose() {
        cleanupDialog()
        cooldownActive = true
        coroutineScope.launch {
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
