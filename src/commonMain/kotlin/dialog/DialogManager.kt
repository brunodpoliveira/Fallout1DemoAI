package dialog

import ai.*
import korlibs.korge.view.*
import kotlinx.coroutines.*
import ui.*
import utils.*

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
                Logger.debug("Updated Bio: $updatedBio")
                Logger.debug("Is Secret Plan: ${metadataInfo.hasSecret}")
                Logger.debug("Has Conspiracy: ${metadataInfo.hasConspiracy}")
                Logger.debug("Conspirators: ${metadataInfo.conspirators}")
                Logger.debug("Secret Participants: ${metadataInfo.secretParticipants}")
                Logger.debug("Actions: $actions")
                actionModel.processNPCReflection(actions.joinToString("\n"), npcName)
            } catch (e: Exception) {
                Logger.error("Error in conversation post-processing: ${e.message}")
                // Handle the error appropriately
            }
        }
    }
}
