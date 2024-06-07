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
    var sendMessageButton: UIButton
    var closeButton: UIButton

    init {
        //PlayerControls.isDialogActive = true

        val npcDialogContainer = container().apply {
            position(0, 240)
            solidRect(1280, 240) { color = Colors.DARKGRAY }
        }
        val playerDialogContainer = container().apply {
            position(0, 480)
            solidRect(1280, 460) { color = Colors.LIGHTGREY }
        }

        npcMessageDisplay = textBlock(RichTextData.fromHTML("")) {
            position(20, 20)
            size(Size(1240, 200))
        }

        userMessageInput = uiTextInput("Type your message here...") {
            position(20, 40)
            size = Size(1040, 60)
        }

        sendMessageButton = uiButton("SEND") {
            position(1080, 40)
            height = 60.0
        }

        closeButton = uiButton("Close") {
            position(1160, 20)
            height = 40.0
        }

        npcDialogContainer.addChild(npcMessageDisplay)
        playerDialogContainer.addChild(userMessageInput)
        playerDialogContainer.addChild(sendMessageButton)

        addChild(npcDialogContainer)
        addChild(playerDialogContainer)
        addChild(closeButton)

        sendMessageButton.onClick {
            sendMessage()
        }

        closeButton.onClick {
            summarizeAndUpdateCharacterBio()
            OpenAIService.resetConversation()
            closeDialog()
        }
    }

    fun show(container: Container, npcBio: String, npcName: String) {
        // PlayerControls.isDialogActive = true
        this.npcName = npcName
        this.currentNpcBio = npcBio
        println("Showing dialog for $npcName with bio: $currentNpcBio")
        container.addChild(this)
        val initialResponse = OpenAIService.getCharacterResponse(currentNpcBio, "Hi")
        npcMessageDisplay.text = RichTextData("${npcName}: $initialResponse")
    }

    fun closeDialog() {
        //PlayerControls.isDialogActive = false
        this.removeFromParent()
    }

    fun sendMessage() {
        val playerInput = userMessageInput.text
        if (playerInput.isNotBlank()) {
            val npcResponse = OpenAIService.getCharacterResponse(currentNpcBio, playerInput)
            val existingText = npcMessageDisplay.plainText
            println(existingText)
            npcMessageDisplay.text = RichTextData("$existingText\nPlayer: $playerInput\n$npcName: $npcResponse")
            userMessageInput.text = ""
        }
    }

    private fun summarizeAndUpdateCharacterBio() {
        val conversation = OpenAIService.msgs.joinToString { it.content }
        val summary = SummarizerService().summarizeConversation(conversation)
        Director.updateContext(summary)
        currentNpcBio += "\n" + summary
    }
}
