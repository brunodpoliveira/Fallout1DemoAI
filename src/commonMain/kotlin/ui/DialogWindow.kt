package ui

import ai.*
import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import kotlin.properties.Delegates
import korlibs.korge.annotations.*
import korlibs.math.geom.*

@OptIn(KorgeExperimental::class)
class DialogWindow : Container() {
    private var userMessageInput: UITextInput
    private var npcMessageDisplay: UIText
    private var currentNpcBio by Delegates.notNull<String>()
    private lateinit var npcName: String

    init {
        val dialogBackground = solidRect(1280, 720) {
            color = Colors.DARKGREY
        }

        npcMessageDisplay = uiText("") {
            position(20, 20)
            size = Size(128, 64)
            width = dialogBackground.width - 40
        }

        userMessageInput = uiTextInput("Type your message here...") {
            position(20, dialogBackground.height - 60)
            width = dialogBackground.width - 200
        }

        val sendMessageButton = uiButton("SEND") {
            position(dialogBackground.width - 160, dialogBackground.height - 60)
        }

        sendMessageButton.onClick {
            val playerInput = userMessageInput.text
            if (playerInput.isNotBlank()) {
                val npcResponse = OpenAIService.getCharacterResponse(currentNpcBio, playerInput)
                npcMessageDisplay.text += "\nPlayer: $playerInput\n$npcName: $npcResponse"
                userMessageInput.text = ""
            }
        }

        val closeButton = uiButton("Close") {
            position(dialogBackground.width - 70, 20)
        }

        closeButton.onClick {
            summarizeAndUpdateCharacterBio()
            this.removeFromParent()
        }

        addChild(dialogBackground)
        addChild(npcMessageDisplay)
        addChild(userMessageInput)
        addChild(sendMessageButton)
        addChild(closeButton)
    }

    private fun summarizeAndUpdateCharacterBio() {
        println("summarizeAndUpdateCharacterBio reached")
        val conversation = OpenAIService.msgs.joinToString { it.content }
        val summary = SummarizerService().summarizeConversation(conversation)
        Director.updateContext(summary)
    }

    fun show(container: Container, npcBio: String, npcName: String) {
        this.npcName = npcName

        currentNpcBio = npcBio
        this.addTo(container)
        this.centerOnStage()

        npcMessageDisplay.text = "$npcName: "
        val greeting = OpenAIService.getCharacterResponse(currentNpcBio, "Hi")
        npcMessageDisplay.text += greeting
    }
}
