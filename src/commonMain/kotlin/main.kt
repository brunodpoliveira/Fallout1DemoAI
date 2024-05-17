import korlibs.image.color.*
import korlibs.korge.*
import korlibs.math.geom.*
import lvls.*

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
    val demo = JunkDemo()
    demo.setupLevel(this)
}
