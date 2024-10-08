package ai

import ai.OpenAIService.msgs
import ai.OpenAIService.sendMessage
import com.theokanning.openai.completion.chat.*
import img.TextDisplayManager

class ConversationPostProcessingServices (private val actionModel: ActionModel){

    private fun npcSelfReflect(conversation: String): String {
        val prompt = """
            Have the character below self-reflect and gauge their opinions and thoughts
            of the below conversation. Have the self-reflection be in first-person and in-character:
            
            Conversation: $conversation
            
            Self-Reflection:
        """.trimIndent()

        val assistantMessage = ChatMessage("assistant", prompt)
        msgs.add(assistantMessage)

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(msgs)
            .temperature(.9)
            .maxTokens(1024)
            .topP(1.0)
            .frequencyPenalty(.8)
            .presencePenalty(.8)
            .build()

        val httpResponse = sendMessage(chatCompletionRequest)
        val choices = httpResponse.choices.mapNotNull { it.message }

        if (choices.isNotEmpty()) {
            msgs.add(choices[0])
            return choices[0].content
        } else {
            return conversation
        }
    }

    private fun thinkOfNextSteps(selfReflection: String, npcBio: String): String {
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

        val assistantMessage = ChatMessage("assistant", prompt)
        msgs.add(assistantMessage)

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(msgs)
            .temperature(.9)
            .maxTokens(1024)
            .topP(1.0)
            .frequencyPenalty(.8)
            .presencePenalty(.8)
            .build()

        val httpResponse = sendMessage(chatCompletionRequest)
        val choices = httpResponse.choices.mapNotNull { it.message }

        if (choices.isNotEmpty()) {
            msgs.add(choices[0])
            return choices[0].content
        } else {
            return selfReflection
        }
    }

    private fun summarizeConversation(conversation: String): String {
        val prompt = """
            Summarize the following conversation in a concise way:
            
            Conversation: $conversation
            
            Summary:
            Actions:
            <List of Actions>
        """.trimIndent()

        val assistantMessage = ChatMessage("assistant", prompt)
        msgs.add(assistantMessage)

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(msgs)
            .temperature(.9)
            .maxTokens(1024)
            .topP(1.0)
            .frequencyPenalty(.8)
            .presencePenalty(.8)
            .build()

        val httpResponse = sendMessage(chatCompletionRequest)
        val choices = httpResponse.choices.mapNotNull { it.message }

        if (choices.isNotEmpty()) {
            msgs.add(choices[0])
            return choices[0].content
        } else {
            return conversation
        }
    }

    private fun checkForSecretsOrConspiracy(metadata: String): Pair<Boolean, List<String>> {
        return if (metadata.contains("SECRET")) {
            Pair(true, emptyList())
        } else if (metadata.contains("CONSPIRACY")) {
            val conspirators = """CONSPIRACY - \[(.*?)]""".toRegex().find(metadata)?.groups?.get(1)?.value?.split(", ") ?: emptyList()
            Pair(false, conspirators)
        } else {
            Pair(false, emptyList())
        }
    }

    private fun editNPCBio(summary: String, npcBio: String): String {
        val prompt = """
            Edit the following NPC Bio to reflect the summary of this conversation and the NPC's personality:
            
            Summary: $summary
            
            NPCBio: $npcBio
            
            Edited Bio:
        """.trimIndent()

        val assistantMessage = ChatMessage("assistant", prompt)
        msgs.add(assistantMessage)

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(msgs)
            .temperature(.9)
            .maxTokens(1024)
            .topP(1.0)
            .frequencyPenalty(.8)
            .presencePenalty(.8)
            .build()

        val httpResponse = sendMessage(chatCompletionRequest)
        val choices = httpResponse.choices.mapNotNull { it.message }

        if (choices.isNotEmpty()) {
            msgs.add(choices[0])
            return choices[0].content
        } else {
            return npcBio
        }
    }

    fun conversationPostProcessingLoop(conversation: String, npcBio: String): Triple<String, Pair<Boolean, List<String>>, List<String>> {
        val summary = summarizeConversation(conversation)
        val selfReflection = npcSelfReflect(summary)
        val nextSteps = thinkOfNextSteps(selfReflection, npcBio)

        val parts = nextSteps.split("Metadata:")
        val actions = parts[0].trim()
        val metadata = parts.getOrNull(1)?.trim() ?: ""

        val (isSecretPlan, conspirators) = checkForSecretsOrConspiracy(metadata)
        val actionModels = actionModel.processNPCReflection(actions)

        Director.updateContext(summary)
        println("Summary: $summary")
        println("Self-Reflection: $selfReflection")
        println("Next Steps: $actions")

        if (Director.getDifficulty() == "easy") {
            TextDisplayManager.directorText?.text = "Director:\n${Director.getContext()}"
            TextDisplayManager.selfReflectionText?.text = "Self-Reflection:\n$selfReflection"
            TextDisplayManager.nextStepsText?.text = "Next Steps:\n$actions"
        }

        actionModels.forEach { action -> println("Action Model: $action") }
        val updatedBio = editNPCBio(summary, npcBio)
        return Triple(updatedBio, Pair(isSecretPlan, conspirators), actionModels)
    }
}
