package dialog

import ai.*
import korlibs.korge.view.*
import kotlinx.coroutines.*
import ui.*

class DialogManager(
    private val coroutineScope: CoroutineScope,
    private val container: Container,
    private val actionModel: ActionModel
) {
    private var dialogWindow: DialogWindow? = null

    fun showDialog(npcName: String, npcBio: String, factionName: String) {
        if (DialogWindow.isInDialog) return

        if (dialogWindow == null) {
            dialogWindow = DialogWindow()
            dialogWindow?.onConversationEnd = { conversation ->
                // Process the conversation outcome
                handleConversationEnd(npcName, npcBio, conversation)
            }
        }

        dialogWindow?.show(container, npcBio, npcName, factionName)
    }

    private fun handleConversationEnd(npcName: String, npcBio: String, conversation: String) {
        val conversationProcessor = ConversationPostProcessingServices(actionModel)
        coroutineScope.launch {
            try {
                val (updatedBio, metadataInfo, actions) =
                    conversationProcessor.conversationPostProcessingLoop(
                        conversation,
                        npcBio,
                        npcName
                    )
                Director.updateNPCContext(
                    npcName,
                    updatedBio,
                    metadataInfo.hasSecret,
                    metadataInfo.conspirators
                )
                println("Updated Bio: $updatedBio")
                println("Is Secret Plan: ${metadataInfo.hasSecret}")
                println("Has Conspiracy: ${metadataInfo.hasConspiracy}")
                println("Conspirators: ${metadataInfo.conspirators}")
                println("Secret Participants: ${metadataInfo.secretParticipants}")
                println("Actions: $actions")
                actionModel.processNPCReflection(actions.joinToString("\n"), npcName)
            } catch (e: Exception) {
                println("Error in conversation post-processing: ${e.message}")
                // Handle the error appropriately
            }
        }
    }
}
