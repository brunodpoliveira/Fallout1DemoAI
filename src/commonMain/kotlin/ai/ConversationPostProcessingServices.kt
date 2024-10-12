package ai

import ai.OpenAIService.createChatCompletionRequest
import ai.OpenAIService.sendMessage
import com.theokanning.openai.completion.chat.*
import img.*
import kotlinx.coroutines.*
import utils.*

class ConversationPostProcessingServices (private val actionModel: ActionModel) {

    private suspend fun npcSelfReflect(conversation: String): String = withContext(Dispatchers.Default) {
        val prompt = """
            Have the character below self-reflect and gauge their opinions and thoughts
            of the below conversation. Have the self-reflection be in first-person and in-character:
            
            Conversation: $conversation
            
            Self-Reflection:
        """.trimIndent()

        val messages = listOf(
            SystemMessage("You are an AI assistant helping with character self-reflection."),
            UserMessage(prompt)
        )

        val chatCompletionRequest = createChatCompletionRequest(messages, 1024)

        try {
            val response = sendMessage(chatCompletionRequest)
            response.choices.firstOrNull()?.message?.content
                ?: "Unable to generate self-reflection."
        } catch (e: Exception) {
            println("Error in npcSelfReflect: ${e.message}")
            "Error occurred during self-reflection."
        }
    }

    private suspend fun thinkOfNextSteps(selfReflection: String, npcBio: String): String {
        val prompt = """
            Based on the self-reflection and the NPC bio below, think of the next steps the character should take. 
            Write it in a way that can be translated to actions later. There can be multiple actions:
            
            Self-Reflection: $selfReflection
            NPCBio: $npcBio
            
            Next Steps and Metadata:
            Actions:
            <List of Actions>
            Metadata:
            <SECRET or CONSPIRACY - [LIST OF CONSPIRATORS] if applicable>
        """.trimIndent()

        val messages = listOf(
            SystemMessage("You are an AI assistant helping to determine a character's next actions."),
            UserMessage(prompt)
        )

        val chatCompletionRequest = createChatCompletionRequest(messages, 1024)

        val response = sendMessage(chatCompletionRequest)
        return response.choices.firstOrNull()?.message?.content
            ?: selfReflection
    }

    private suspend fun summarizeConversation(conversation: String): String {
        val prompt = """
            Summarize the following conversation in a concise way:
            
            Conversation: $conversation
            
            Summary:
            Actions:
            <List of Actions>
        """.trimIndent()

        val messages = listOf(
            SystemMessage("You are an AI assistant tasked with summarizing conversations."),
            UserMessage(prompt)
        )

        val chatCompletionRequest = createChatCompletionRequest(messages, 1024)

        val response = sendMessage(chatCompletionRequest)
        return response.choices.firstOrNull()?.message?.content
            ?: conversation
    }

    private fun checkForSecretsOrConspiracy(metadata: String): Result<MetadataInfo> {
        return try {
            val secretPattern = """SECRET - \[(.*?)]""".toRegex()
            val conspiracyPattern = """CONSPIRACY - \[(.*?)]""".toRegex()

            val secretMatch = secretPattern.find(metadata)
            val conspiracyMatch = conspiracyPattern.find(metadata)

            val secretParticipants = secretMatch?.groups?.get(1)?.value?.split(", ") ?: emptyList()
            val conspirators = conspiracyMatch?.groups?.get(1)?.value?.split(", ") ?: emptyList()

            val hasSecret = secretMatch != null
            val hasConspiracy = conspiracyMatch != null

            Result.success(MetadataInfo(hasSecret, hasConspiracy, secretParticipants, conspirators))
        } catch (e: Exception) {
            Result.failure(Exception("Error parsing metadata: ${e.message}"))
        }
    }

    private suspend fun editNPCBio(summary: String, npcBio: String): String {
        val prompt = """
            Edit the following NPC Bio to reflect the summary of this conversation and the NPC's personality:
            
            Summary: $summary
            
            NPCBio: $npcBio
            
            Edited Bio:
        """.trimIndent()

        val messages = listOf(
            SystemMessage("You are an AI assistant responsible for updating character biographies."),
            UserMessage(prompt)
        )

        val chatCompletionRequest = createChatCompletionRequest(messages, 1024)

        return try {
            val response = sendMessage(chatCompletionRequest)
            response.choices.firstOrNull()?.message?.content ?: npcBio
        } catch (e: Exception) {
            println("Error in editNPCBio: ${e.message}")
            npcBio // Return the original bio if there's an error
        }
    }

    suspend fun conversationPostProcessingLoop(
        conversation: String,
        npcBio: String,
        npcName: String
    ): Triple<String, MetadataInfo, List<String>> {
        val summary = summarizeConversation(conversation)
        val selfReflection = npcSelfReflect(summary)
        val nextSteps = thinkOfNextSteps(selfReflection, npcBio)

        val parts = nextSteps.split("Metadata:")
        val actions = parts[0].trim()
        val metadata = parts.getOrNull(1)?.trim() ?: ""

        val metadataInfo = checkForSecretsOrConspiracy(metadata).getOrElse {
            println("Warning: ${it.message}")
            MetadataInfo(
                hasSecret = false,
                hasConspiracy = false,
                secretParticipants = emptyList(),
                conspirators = emptyList()
            )
        }

        val (actionModels, _, _) = actionModel.processNPCReflection(actions, npcName)

        Director.updateContext(summary)
        println("Summary: $summary")
        println("Self-Reflection: $selfReflection")
        println("Next Steps: $actions")
        println("Metadata Info: $metadataInfo")

        if (Director.getDifficulty() == "easy") {
            TextDisplayManager.directorText?.text = "Director:\n${Director.getContext()}"
            TextDisplayManager.selfReflectionText?.text = "Self-Reflection:\n$selfReflection"
            TextDisplayManager.nextStepsText?.text = "Next Steps:\n$actions"
        }

        actionModels.forEach { action -> println("Action Model: $action") }
        val updatedBio = editNPCBio(summary, npcBio)

        return Triple(updatedBio, metadataInfo, actionModels)
    }
}
