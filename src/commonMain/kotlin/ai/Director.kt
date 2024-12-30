package ai

import korlibs.korge.ldtk.view.*
import kotlinx.coroutines.*
import utils.*
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.emptyList
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toList

object Director {
    private lateinit var gameData: GameData
    private lateinit var currentLevelData: LevelData
    private var currentLevelId: String = "level_0"
    private var storyContext = ""
    private var npcContexts: MutableMap<String, String> = mutableMapOf()
    private var factionContexts: MutableMap<String, String> = mutableMapOf()
    private var gameDifficulty: String = "normal"
    private var ldtk: LDTKWorld? = null


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

    fun updateNPCContext(
        npcName: String,
        newContext: String,
        isSecretPlan: Boolean = false,
        conspirators: List<String> = emptyList()
    ) {
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

    fun getAvailableActionVerb(): List<String> {
        return ActionVerb.entries.map { it.name }
    }

    fun getAvailableSectorNames(ldtk: LDTKWorld): List<String> {
        val level = ldtk.levelsByName["Level_0"]
            ?: throw IllegalArgumentException("Level 'Level_0' not found")

        val sectorLayer = level.layersByName["Sector"]
            ?: throw IllegalArgumentException("Layer 'Sector' not found in level 'Level_0'.")


        val sectorDefinitions = ldtk.ldtk.defs.layers.find { it.identifier == "Sector" }
            ?.intGridValues
            ?: throw IllegalArgumentException("IntGridValues not found for layer 'Sector'.")

        val sectorMap = sectorDefinitions.associate { it.value to it.identifier }

        val sectors = mutableSetOf<String>() // Evita duplicatas

        Logger.debug("Starting iteration through intGridCSV values in layer 'sector'.")

        sectorLayer.layer.intGridCSV.forEachIndexed { index, value ->
            Logger.debug("processing index $index with value $value.")


            val sectorName = sectorMap[value]?.replace("_", " ")

            if (sectorName != null) {
                Logger.debug("Sector found: $sectorName")
                sectors.add(sectorName)
            } else {
                Logger.debug("No valid sector found for value $value at index $index.")
            }
        }

        val sectorList = sectors.toList()
        Logger.debug("Lista final de setores disponíveis: $sectorList")
        return sectorList
    }

}
