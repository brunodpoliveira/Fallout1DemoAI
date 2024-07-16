package ai

object Director {
    private val initialStoryContext = """
        The town of Scrapheap is in turmoil. 
        Two rival gangs, the Crypts and the Fools, 
        are vying for control over the town's power generator.
        While the Crypts have the upper hand in numbers, the Fools are more determined. 
        The non-gang residents fear for their safety and hope for a savior to liberate them.
    """.trimIndent()

    private var storyContext = initialStoryContext
    private var npcContexts: MutableMap<String, String> = mutableMapOf()
    private var factionContexts: MutableMap<String, String> = mutableMapOf()
    private var gameDifficulty: String = "normal"

    private val npcFactions = mapOf(
        "Rayze" to "Crypts",
        "Baka" to "Fools",
        "Lex" to "Non-Gang"
    )

    fun updateContext(newContext: String) {
        println("Updating context with new information: $newContext")
        storyContext += "\n$newContext"
    }

    fun updateNPCContext(npcName: String, newContext: String, isSecretPlan: Boolean = false, conspirators: List<String> = emptyList()) {
        println("Updating context for $npcName with new information: $newContext")
        npcContexts[npcName] = npcContexts.getOrDefault(npcName, "") + "\n" + newContext

        val factionName = npcFactions[npcName]
        if (factionName != null) {
            updateFactionContext(factionName, newContext)
        }

        if (isSecretPlan) {
            updateSecretPlan(conspirators, newContext)
        }
    }

    private fun updateFactionContext(factionName: String, newContext: String) {
        println("Updating faction context for $factionName with new information: $newContext")
        factionContexts[factionName] =
            factionContexts.getOrDefault(factionName, "") + "\n" + newContext
    }

    private fun updateSecretPlan(conspirators: List<String>, newContext: String) {
        conspirators.forEach { npcName ->
            println("Updating secret plan context for $npcName with new information: $newContext")
            updateNPCContext(npcName, newContext)
        }
    }

    fun getContext(): String {
        println("Current context: $storyContext")
        return storyContext
    }

    fun getNPCContext(npcName: String): String {
        println("Current context for $npcName: ${npcContexts[npcName]}")
        return npcContexts[npcName] ?: ""
    }

    fun getFactionContext(factionName: String): String {
        println("Current context for faction $factionName: ${factionContexts[factionName]}")
        return factionContexts[factionName] ?: ""
    }

    fun resetContext() {
        storyContext = initialStoryContext
        npcContexts.clear()
        factionContexts.clear()
    }

    fun setDifficulty(difficulty: String) {
        println("Setting game difficulty to: $difficulty")
        gameDifficulty = difficulty
    }

    fun getDifficulty(): String {
        return gameDifficulty
    }
}
