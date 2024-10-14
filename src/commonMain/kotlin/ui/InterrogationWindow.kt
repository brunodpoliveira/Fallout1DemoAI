package ui

import ai.*
import korlibs.image.color.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import utils.*

@OptIn(KorgeExperimental::class)
class InterrogationWindow : Container() {
    private lateinit var npcSelector: UIComboBox<String>
    private lateinit var questionInput: UITextInput
    private lateinit var responseArea: UIText
    private lateinit var submitButton: UIButton
    private lateinit var closeButton: UIButton

    fun show(container: Container, npcs: List<String>) {
        if (GameState.isInterrogationOpen) return
        GameState.isInterrogationOpen = true

        position(50, 50)
        size(Size(1180, 620))

        // Background
        solidRect(size.width, size.height, Colors["#000000AA"])

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

    private fun askQuestion() {
        val selectedNPC = npcSelector.selectedItem ?: return
        val question = questionInput.text
        if (question.isNotBlank()) {
            val response = OpenAIService.getCharacterResponse(selectedNPC, null, "", question)
            responseArea.text = "${responseArea.text}\n\nQ: $question\nA: $response"
            questionInput.text = ""
        }
    }

    private fun close(container: Container) {
        GameState.isInterrogationOpen = false
        container.removeChild(this)
    }
}
