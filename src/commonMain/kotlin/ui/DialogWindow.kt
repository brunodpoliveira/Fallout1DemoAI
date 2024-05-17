package ui

import ai.*
import korlibs.korge.view.*
import korlibs.image.color.Colors
import korlibs.image.font.DefaultTtfFont

class DialogWindow(parent: Container) : Container() {
    init {
        parent.addChild(this)
    }

    private val dialogBg = solidRect(300, 150, Colors.DARKGRAY) { position(150, 200) }
    private val dialogText = text("", font = DefaultTtfFont, textSize = 16.0) { position(dialogBg.x + 10, dialogBg.y + 10) }

    private lateinit var npcBio: String
    private var inputText = ""

    fun showForNPC(npcName: String) {
        npcBio = getNpcBio(npcName)
        dialogText.text = "Talk to $npcName"
        visible = true
    }

    fun hide() {
        visible = false
        dialogText.text = ""
        inputText = ""
    }

    private fun updateDialog(newText: String) {
        dialogText.text = newText
    }

    fun sendMessage() {
        val response = OpenAIService.getCharacterResponse(inputText, npcBio)
        updateDialog(response)
        inputText = ""
    }

    private fun getNpcBio(npcName: String): String {
        // Return appropriate bio based on NPC name; Simplified example
        return "Rayze is the leader of the Crypts gang. He is known for..."
    }
}
