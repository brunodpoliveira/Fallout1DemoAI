package ui

import korlibs.image.color.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import utils.*

@OptIn(KorgeExperimental::class)
class InterrogationWindow(
    private val onAskQuestion: suspend (npcName: String, question: String) -> String
) : Container() {
    private lateinit var npcSelector: UIComboBox<String>
    private lateinit var questionInput: UITextInput
    private lateinit var responseArea: UIText
    private lateinit var submitButton: UIButton
    private lateinit var closeButton: UIButton
    private lateinit var loadingIndicator: UIProgressBar

    fun show(container: Container, npcs: List<String>) {
        if (GameState.isInterrogationOpen) return
        GameState.isInterrogationOpen = true

        position(50, 50)
        size(Size(1180, 620))

        // Background
        solidRect(size.width, size.height, Colors["#000000AA"])

        // Loading Indicator
        loadingIndicator = uiProgressBar(size = Size(200, 4)) {
            position(940, 110)
            visible = false
        }

        // NPC Selector
        npcSelector = uiComboBox(
            size = Size(200, 32),
            items = npcs,
            selectedIndex = 0
        ) {
            position(20, 20)
        }

        // Question Input
        questionInput = uiTextInput {
            position(20, 70)
            size(Size(900, 30))
        }

        // Submit Button
        submitButton = uiButton("Ask") {
            position(940, 70)
            size(Size(100, 30))
            onClick { askQuestion() }
        }

        // Response Area
        responseArea = uiText("") {
            position(20, 120)
            size(Size(1140, 450))
        }

        // Close Button
        closeButton = uiButton("Close") {
            position(1080, 20)
            onClick { close(container) }
        }

        container.addChild(this)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun askQuestion() {
        val selectedNPC = npcSelector.selectedItem ?: return
        val question = questionInput.text
        if (question.isNotBlank()) {
            // Disable input while processing
            setLoading(true)

            // Launch coroutine for async operation
            GlobalScope.launch {
                try {
                    val response = onAskQuestion(selectedNPC, question)
                    responseArea.text = "${responseArea.text}\n\nQ: $question\nA: $response"
                    questionInput.text = ""
                } catch (e: Exception) {
                    Logger.error("Error getting response: ${e.message}")
                    responseArea.text = "${responseArea.text}\n\nError: Failed to get response"
                } finally {
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        submitButton.enabled = !loading
        questionInput.enabled = !loading
        loadingIndicator.visible = loading
    }

    private fun close(container: Container) {
        GameState.isInterrogationOpen = false
        container.removeChild(this)
    }
}
