package ai

import ai.OpenAIService.msgs
import ai.OpenAIService.sendMessage
import com.theokanning.openai.completion.chat.*

object ActionVerbs {
    val verbs = listOf("MOVE", "GIVE", "TAKE", "ATTACK", "DEFEND", "CONSPIRE",
        "PLAN", "GATHER_RESOURCE", "GATHER_INTELLIGENCE")
    val locations = listOf("TOWN_SQUARE", "BAR", "CURRENT_LOCATION", "FARM", "GENERATOR_ROOM")
    val items = listOf("GUN", "AMMUNITION", "FOOD", "WATER")
}

class ConversationPostProcessingServices {

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
            println(choices[0].content)
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
            
            Next Steps:
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
            println(choices[0].content)
            msgs.add(choices[0])
            return choices[0].content
        } else {
            return selfReflection
        }
    }

    private fun translateNextStepsToActionModel(nextSteps: String): List<String> {
        val prompt = """
            Translate the following next steps into an action model command(s) in the following format:
            [ACTION],[SUBJECT],[LOCATION],[ITEM]
            
            Explanation:
            [ACTION]: What will the character DO
            [SUBJECT]: WHO will they do it to
            [LOCATION]: WHERE will they do it
            [ITEM]: (optional) Write down the ITEM that the character will want to act with here. 
            Mostly used in the GIVE, TAKE, and GATHER_RESOURCE commands
            
            If there are multiple subjects (For example, CONSPIRE (secret planning) and PLAN (open planning) will 
            usually involve multiple subjects) make one action for each that needs to be informed. 
            Example: CONSPIRE CHAR1 CURRENT_LOCATION, CONSPIRE CHAR2 CURRENT_LOCATION, etc
            Same deal with GIVE: If the character wants to give multiple objects, put each object 
            to be given in its own action model command.
            Example: GIVE PLAYER CURRENT_LOCATION WEAPON, GIVE PLAYER CURRENT_LOCATION AMMUNITION, etc

            Use ONLY the following verbs: ${ActionVerbs.verbs.joinToString(", ")}
            Use ONLY these possible locations: ${ActionVerbs.locations.joinToString(", ")}
            Use ONLY these possible items, if applicable: ${ActionVerbs.items.joinToString(", ")}

            Next Steps: $nextSteps

            Action Model (one per line):
        """.trimIndent()

        val assistantMessage = ChatMessage("assistant", prompt)
        msgs.add(assistantMessage)

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(msgs)
            .temperature(.5)
            .maxTokens(512)
            .topP(1.0)
            .frequencyPenalty(.8)
            .presencePenalty(.8)
            .build()

        val httpResponse = sendMessage(chatCompletionRequest)
        val choices = httpResponse.choices.mapNotNull { it.message }

        if (choices.isNotEmpty()) {
            msgs.add(choices[0])
            return choices[0].content.lines().filter { it.isNotBlank() }
        } else {
            return listOf("No action defined")
        }
    }

    private fun summarizeConversation(conversation: String): String {
        val prompt = """
            Summarize the following conversation in a concise way:
            
            Conversation: $conversation
            
            Summary:
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
            println(choices[0].content)
            msgs.add(choices[0])
            return choices[0].content
        } else {
            return conversation
        }
    }

    fun editNPCBio(summary: String, npcBio: String): String {
        val prompt = """
            Edit the following NPC Bio to reflect the summary of this conversation:
            
            Summary: $summary
            
            NPCBio: $npcBio
            
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
            println(choices[0].content)
            msgs.add(choices[0])
            return choices[0].content
        } else {
            return npcBio
        }
    }

    fun conversationPostProcessingLoop(conversation: String, npcBio: String): String {
        val summary = summarizeConversation(conversation)
        val selfReflection = npcSelfReflect(conversation)
        val nextSteps = thinkOfNextSteps(selfReflection, npcBio)
        val actionModels = translateNextStepsToActionModel(nextSteps)
        Director.updateContext(summary)

        if (Director.getDifficulty() == "easy") {
            println("Summary: $summary")
            println("Self-Reflection: $selfReflection")
            println("Next Steps: $nextSteps")

            TextDisplayManager.directorText?.text = "Director:\n${Director.getContext()}"
            TextDisplayManager.selfReflectionText?.text = "Self-Reflection:\n$selfReflection"
            TextDisplayManager.nextStepsText?.text = "Next Steps:\n$nextSteps"
        }

        actionModels.forEach { action -> println("Action Model: $action") }
        return summary
    }
}
