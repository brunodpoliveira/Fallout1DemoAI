import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.math.geom.*
import scenes.*

suspend fun main() = Korge(windowSize = Size(1280, 720), backgroundColor = Colors["#2b2b2b"], displayMode = KorgeDisplayMode.TOP_LEFT_NO_CLIP) {

    val sceneContainer = sceneContainer()

    injector.mapSingleton { MainMenuScene() }
    injector.mapSingleton { GameOverScene() }
    injector.mapPrototype { DemoLevel() }

    sceneContainer.changeTo { MainMenuScene() }
}
