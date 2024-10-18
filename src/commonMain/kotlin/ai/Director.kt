package ai

import kotlinx.coroutines.*
import utils.*

object Director {
    private lateinit var gameData: GameData
    private lateinit var currentLevelData: LevelData
    private var currentLevelId: String = "level_0"
    private var storyContext = ""
    private var npcContexts: MutableMap<String, String> = mutableMapOf()
    private var factionContexts: MutableMap<String, String> = mutableMapOf()
    private var gameDifficulty: String = "normal"

    fun initialize(levelId: String = "level_0") {
        runBlocking {
            gameData = JsonLoader.loadGameData()
        }
        loadLevel(levelId)
    }

    fun loadLevel(levelId: String) {
        currentLevelId = levelId
        currentLevelData = gameData.levels[levelId] ?: throw IllegalArgumentException("Level $levelId not found")
        storyContext = currentLevelData.context
        npcContexts.clear()
        factionContexts.clear()

        currentLevelData.npcData.forEach { (npcName, npcData) ->
            npcContexts[npcName] = npcData.initialPlan
            val faction = npcData.faction.takeIf { it.isNotEmpty() } ?: "Civilian"
            if (!factionContexts.containsKey(faction)) {
                factionContexts[faction] = ""
            }
        }
    }

    fun updateContext(newContext: String) {
        Logger.debug("Updating context with new information: $newContext")
        storyContext += "\n$newContext"
    }

    fun updateNPCContext(npcName: String, newContext: String, isSecretPlan: Boolean = false, conspirators: List<String> = emptyList()) {
        Logger.debug("Updating context for $npcName with new information: $newContext")
        npcContexts[npcName] = npcContexts.getOrDefault(npcName, "") + "\n" + newContext

        val factionName = getNPCFaction(npcName)
        updateFactionContext(factionName, newContext)

        if (isSecretPlan) {
            updateSecretPlan(conspirators, newContext)
        }
    }

    private fun updateFactionContext(factionName: String, newContext: String) {
        Logger.debug("Updating faction context for $factionName with new information: $newContext")
        factionContexts[factionName] = factionContexts.getOrDefault(factionName, "") + "\n" + newContext
    }

    private fun updateSecretPlan(conspirators: List<String>, newContext: String) {
        conspirators.forEach { npcName ->
            Logger.debug("Updating secret plan context for $npcName with new information: $newContext")
            updateNPCContext(npcName, newContext)
        }
    }

    fun getContext(): String {
        Logger.debug("Current context: $storyContext")
        return storyContext
    }

    fun getNPCContext(npcName: String): String {
        return npcContexts[npcName] ?: ""
    }

    fun getNPCBio(npcName: String): String {
        return currentLevelData.npcData[npcName]?.bio ?: ""
    }

    fun getAllNPCNames(): List<String> {
        return currentLevelData.npcData.keys.toList()
    }

    fun getNPCFaction(npcName: String): String {
        return currentLevelData.npcData[npcName]?.faction?.takeIf { it.isNotEmpty() } ?: "Civilian"
    }

    fun getFactionContext(factionName: String): String {
        Logger.debug("Current context for faction $factionName: ${factionContexts[factionName]}")
        return factionContexts[factionName] ?: ""
    }

    fun resetContext() {
        loadLevel(currentLevelId)
    }

    fun setDifficulty(difficulty: String) {
        Logger.debug("Setting game difficulty to: $difficulty")
        gameDifficulty = difficulty
    }

    fun getDifficulty(): String {
        return gameDifficulty
    }
}
