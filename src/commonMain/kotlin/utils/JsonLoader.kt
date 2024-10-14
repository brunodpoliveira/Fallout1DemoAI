package utils

import kotlinx.serialization.json.Json
import korlibs.io.file.std.resourcesVfs
import kotlinx.serialization.Serializable

object JsonLoader {
    suspend fun loadGameData(): GameData {
        val jsonString = resourcesVfs["game_data.json"].readString()
        return Json.decodeFromString(jsonString)
    }
}

@Serializable
data class GameData(
    val levels: Map<String, LevelData>
)

@Serializable
data class LevelData(
    val context: String,
    val npcData: Map<String, NpcData>
)

@Serializable
data class NpcData(
    val bio: String,
    val initialPlan: String,
    val faction: String
)
