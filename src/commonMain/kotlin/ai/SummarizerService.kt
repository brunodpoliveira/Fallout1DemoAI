package ai

import ai.OpenAIService.msgs
import ai.OpenAIService.sendMessage
import com.theokanning.openai.completion.chat.*

class SummarizerService {

    //TODO ADD selfreflect to the summary
    fun npcSelfReflect(conversation: String): String {
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
            println(choices[0].content.toString())
            msgs.add(choices[0])
            return choices[0].content
        } else {
            return conversation
        }
    }

    fun summarizeConversation(conversation: String): String {
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
            println(choices[0].content.toString())
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
            println(choices[0].content.toString())
            msgs.add(choices[0])
            return choices[0].content
        } else {
            return npcBio
        }
    }
}
