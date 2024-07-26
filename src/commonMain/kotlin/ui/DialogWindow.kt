package ui

import ai.*
import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.io.async.launch
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import scenes.*

@OptIn(KorgeExperimental::class)
class DialogWindow : Container() {
    private var userMessageInput: UITextInput
    private var npcMessageDisplay: TextBlock
    private lateinit var currentNpcBio: String
    private lateinit var npcName: String
    private lateinit var factionName: String
    private var sendMessageButton: UIButton
    private var closeButton: UIButton
    private var loadingProgressBar: UIProgressBar = uiProgressBar(size = Size(256, 8), current = 0f, maximum = 100f) {
        position(128.0, 240.0)
        visible = false
    }
    private var loadingJob: Job? = null
    private var cooldownActive = false

    init {
        addChild(loadingProgressBar)

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
            if (isInDialog) {
                sendMessage()
            }
        }

        closeButton.onClick {
            if (isInDialog) {
                handleCloseConversation()
                OpenAIService.resetConversation()
                disableButtons()
                showLoadingScreen(reversing = true)
            }
        }
    }

    fun show(container: Container, npcBio: String, npcName: String, factionName: String) {
        if (cooldownActive || isInDialog) return
        isInDialog = true

        this.npcName = npcName
        this.currentNpcBio = npcBio
        this.factionName = factionName

        println("Showing dialog for $npcName with bio: $currentNpcBio")
        container.addChild(this)
        showLoadingScreen(reversing = false)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showLoadingScreen(reversing: Boolean) {
        loadingProgressBar.visible = true
        loadingProgressBar.current = 0.0
        JunkDemoScene.dialogIsOpen = true

        loadingJob = GlobalScope.launch {
            while (loadingProgressBar.current < loadingProgressBar.maximum) {
                loadingProgressBar.current += 1
                delay(30)
            }

            loadingProgressBar.visible = false

            if (reversing) {
                finishCloseDialog()
            } else {
                startDialog()
            }
        }
    }

    private fun startDialog() {
        val initialResponse = OpenAIService.getCharacterResponse(npcName, factionName, currentNpcBio, "Hi")
        npcMessageDisplay.text = RichTextData("${npcName}: $initialResponse")
    }

    private fun handleCloseConversation() {
        val conversation = getCurrentConversation()
        val (updatedBio, secretConspiracyPair) = ConversationPostProcessingServices().conversationPostProcessingLoop(conversation, currentNpcBio)
        val (isSecretPlan, conspirators) = secretConspiracyPair
        Director.updateNPCContext(npcName, updatedBio, isSecretPlan, conspirators)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun finishCloseDialog() {
        this.removeFromParent()
        loadingJob?.cancel()
        JunkDemoScene.dialogIsOpen = false
        isInDialog = false

        // Start cooldown
        GlobalScope.launch {
            cooldownActive = true
            delay(5000)  // 5-second cooldown
            cooldownActive = false
        }
    }

    private fun sendMessage() {
        val playerInput = userMessageInput.text
        if (playerInput.isNotBlank()) {
            val npcResponse = OpenAIService.getCharacterResponse(npcName, factionName, currentNpcBio, playerInput)
            val existingText = npcMessageDisplay.plainText
            npcMessageDisplay.text = RichTextData("$existingText\nPlayer: $playerInput\n$npcName: $npcResponse")
            userMessageInput.text = ""
        }
    }

    private fun getCurrentConversation(): String {
        return OpenAIService.msgs.joinToString { it.content }
    }

    private fun disableButtons() {
        sendMessageButton.disable()
        closeButton.disable()
    }

    companion object {
        var isInDialog: Boolean = false  // Shared flag for dialog state
    }
}
