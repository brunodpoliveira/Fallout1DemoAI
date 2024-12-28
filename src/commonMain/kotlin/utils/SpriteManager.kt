package utils

import korlibs.image.atlas.*
import korlibs.image.format.*
import korlibs.io.file.std.*

class SpriteManager(private val atlas: MutableAtlasUnit) {
    private lateinit var spriteConfig: SpriteConfig
    private val loadedSprites = mutableMapOf<String, ImageDataContainer>()

    suspend fun initialize(levelId: String) {
        val gameData = JsonLoader.loadGameData()
        spriteConfig = gameData.levels[levelId]?.spriteConfig
            ?: throw IllegalStateException("No sprite config found for level $levelId")
    }

    suspend fun getSpriteForNPC(npcName: String, faction: String, gender: String): ImageDataContainer {
        val npcInfo = spriteConfig.npcSprites[npcName]

        return when {
            // Case 1: NPC has custom sprite defined
            npcInfo?.sprite != null -> loadSprite(npcInfo.sprite)

            // Case 2: NPC has specific type defined
            npcInfo?.type != null -> spriteConfig.defaults[npcInfo.type]?.let { loadSprite(it) }
                ?: loadDefaultSprite(faction, gender)

            // Case 3: Use faction/gender default
            else -> loadDefaultSprite(faction, gender)
        }
    }

    private suspend fun loadDefaultSprite(faction: String, gender: String): ImageDataContainer {
        val defaultType = "${gender.lowercase()}_${faction.lowercase()}"
        return spriteConfig.defaults[defaultType]?.let { loadSprite(it) }
            ?: loadSprite(spriteConfig.defaults["placeholder"]!!)
    }

    private suspend fun loadSprite(path: String): ImageDataContainer {
        return loadedSprites.getOrPut(path) {
            try {
                resourcesVfs[path].readImageDataContainer(ASE.toProps(), atlas)
            } catch (e: Exception) {
                Logger.error("Failed to load sprite $path: ${e.message}")
                // Load placeholder sprite as fallback
                resourcesVfs[spriteConfig.defaults["placeholder"]!!]
                    .readImageDataContainer(ASE.toProps(), atlas)
            }
        }
    }

    fun clear() {
        loadedSprites.clear()
    }
}
