package ai

object Director {
    private var storyContext = """
        The town of Scrapheap is in turmoil. 
        Two rival gangs, the Crypts and the Fools, 
        are vying for control over the town's power generator.
        While the Crypts have the upper hand in numbers, the Fools are more determined. 
        The non-gang residents fear for their safety and hope for a savior to liberate them.
    """.trimIndent()

    fun updateContext(newContext: String) {
        storyContext += "\n$newContext"
    }

    fun getContext(): String {
        return storyContext
    }
}
