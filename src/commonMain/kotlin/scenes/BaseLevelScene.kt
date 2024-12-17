package scenes

import ai.*
import korlibs.korge.scene.Scene
import korlibs.io.file.VfsFile
import korlibs.io.file.std.*
import korlibs.korge.view.*
import utils.*

abstract class BaseLevelScene(val levelId: String) : Scene() {
    lateinit var sceneLoader: SceneLoader

    override suspend fun SContainer.sceneMain() {
        GameState.currentLevel = levelId

        Director.initialize(levelId)
        sceneLoader = SceneLoader(this@BaseLevelScene, this, getLdtkFile(), levelId).loadScene()
        Director.resetContext()
        initializeLevelSpecifics()

        addUpdater {
            sceneLoader.playerMovementController.update()
            sceneLoader.agentInteractionManager.update()
        }
    }

    private fun getLdtkFile(): VfsFile {
        return when (levelId) {
            "scrapheap" -> resourcesVfs["gfx/dungeon_tilesmap_calciumtrice.ldtk"]
            "scrapheap_ext" -> resourcesVfs["gfx/dungeon_tilesmap_calciumtrice.ldtk"]
            // Add more levels as needed
            else -> throw IllegalArgumentException("Unknown levelId: $levelId")
        }
    }
    abstract suspend fun initializeLevelSpecifics()
}
