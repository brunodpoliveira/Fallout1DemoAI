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

    fun updateContext(newContext: String) {
        println("Updating context with new information: $newContext")
        storyContext += "\n$newContext"
    }

    fun getContext(): String {
        println("Current context: $storyContext")
        return storyContext
    }

    fun resetContext() {
        storyContext = initialStoryContext
    }
}
