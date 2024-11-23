package ui

import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.io.async.launch
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

@OptIn(KorgeExperimental::class)
class DialogWindow : Container() {
    private var userMessageInput: UITextInput
    private var npcMessageDisplay: TextBlock
    private var sendMessageButton: UIButton
    private var closeButton: UIButton

    private var loadingProgressBar: UIProgressBar =
        uiProgressBar(size = Size(256, 8), current = 0f, maximum = 100f) {
        position(128.0, 240.0)
        visible = false
    }

    private var loadingJob: Job? = null

    var onMessageSend: ((String) -> Unit)? = null
    var onClose: (() -> Unit)? = null

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
            val message = userMessageInput.text
            if (message.isNotBlank()) {
                onMessageSend?.invoke(message)
                userMessageInput.text = ""
            }
        }

        closeButton.onClick {
            onClose?.invoke()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun showLoading() {
        loadingProgressBar.visible = true
        loadingProgressBar.current = 0.0
        disableButtons()

        loadingJob = GlobalScope.launch {
            while (loadingProgressBar.current < loadingProgressBar.maximum) {
                loadingProgressBar.current += 1
                delay(30)
            }
            loadingProgressBar.visible = false
        }
    }

    fun hideLoading() {
        loadingJob?.cancel()
        loadingProgressBar.visible = false
        enableButtons()
    }

    fun updateConversation(text: String) {
        npcMessageDisplay.text = RichTextData(text)
    }

    private fun disableButtons() {
        sendMessageButton.disable()
        closeButton.disable()
    }

    private fun enableButtons() {
        sendMessageButton.enable()
        closeButton.enable()
    }

    fun clear() {
        userMessageInput.text = ""
        npcMessageDisplay.text = RichTextData("")
    }
}
