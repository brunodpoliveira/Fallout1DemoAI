package ui

import ai.*
import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*

@OptIn(KorgeExperimental::class)
class DialogWindow : Container() {
    private var userMessageInput: UITextInput
    private var npcMessageDisplay: TextBlock
    private lateinit var currentNpcBio: String
    private lateinit var npcName: String
    private lateinit var factionName: String
    private var sendMessageButton: UIButton
    private var closeButton: UIButton

    init {
        val playerDialogInputContainer = container().apply {
            position(0, 480)
            solidRect(1280, 460) { color = Colors.LIGHTGREY }
        }

        val dialogScrollableContainer = uiScrollable(size = Size(1280, 240)) {
            position(0, 240)
        }

        npcMessageDisplay = textBlock(RichTextData.fromHTML("")) {
            size = Size(1240, 10000)
            fill = Colors.WHITE
        }

        userMessageInput = uiTextInput("Type your message here...") {
            position(20, 40)
            size = Size(1040, 60)
        }

        sendMessageButton = uiButton("Send") {
            position(1080, 40)
            height = 60.0
        }

        closeButton = uiButton("Close") {
            position(1160, 20)
            height = 40.0
        }

        playerDialogInputContainer.addChild(userMessageInput)
        playerDialogInputContainer.addChild(sendMessageButton)

        addChild(dialogScrollableContainer)
        dialogScrollableContainer.container.addChild(npcMessageDisplay)
        addChild(playerDialogInputContainer)
        addChild(closeButton)

        sendMessageButton.onClick {
            sendMessage()
        }

        closeButton.onClick {
            handleCloseConversation()
            OpenAIService.resetConversation()
            closeDialog()
        }
    }

    fun show(container: Container, npcBio: String, npcName: String, factionName: String) {
        this.npcName = npcName
        this.currentNpcBio = npcBio
        this.factionName = factionName
        println("Showing dialog for $npcName with bio: $currentNpcBio")
        container.addChild(this)
        val initialResponse = OpenAIService.getCharacterResponse(npcName,
            factionName, currentNpcBio, "Hi")
        npcMessageDisplay.text = RichTextData("${npcName}: $initialResponse")
    }

    private fun closeDialog() {
        this.removeFromParent()
    }

    private fun sendMessage() {
        val playerInput = userMessageInput.text
        if (playerInput.isNotBlank()) {
            val npcResponse = OpenAIService.getCharacterResponse(npcName, factionName, currentNpcBio,
                playerInput)
            val existingText = npcMessageDisplay.plainText
            npcMessageDisplay.text =
                RichTextData("$existingText\nPlayer: $playerInput\n$npcName: $npcResponse")
            userMessageInput.text = ""
        }
    }

    private fun handleCloseConversation() {
        val conversation = getCurrentConversation()
        val (updatedBio, secretConspiracyPair) =
            ConversationPostProcessingServices().conversationPostProcessingLoop(
                conversation, currentNpcBio
            )

        val (isSecretPlan, conspirators) = secretConspiracyPair

        Director.updateNPCContext(npcName, updatedBio, isSecretPlan, conspirators)
    }

    private fun getCurrentConversation(): String {
        return OpenAIService.msgs.joinToString { it.content }
    }
}
