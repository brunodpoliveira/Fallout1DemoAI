package utils

import kotlinx.serialization.json.Json
import korlibs.io.file.std.resourcesVfs
import kotlinx.serialization.Serializable

@Serializable
data class GameData(
    val levels: Map<String, LevelData>
)

@Serializable
data class LevelData(
    val context: String,
    val npcData: Map<String, NpcData>,
    val spriteConfig: SpriteConfig? = null
)

@Serializable
data class NpcData(
    val bio: String,
    val initialPlan: String,
    val faction: String,
    val gender: String = "Male"
)

@Serializable
data class SpriteConfig(
    val defaults: Map<String, String>,
    val npcSprites: Map<String, NPCSpriteInfo>
)

@Serializable
data class NPCSpriteInfo(
    val sprite: String? = null,
    val type: String? = null
)

object JsonLoader {
    suspend fun loadGameData(): GameData {
        val jsonString = resourcesVfs["game_data.json"].readString()
        return Json.decodeFromString(jsonString)
    }
}
