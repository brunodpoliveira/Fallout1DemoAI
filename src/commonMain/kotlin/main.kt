import korlibs.event.*
import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()

	sceneContainer.changeTo { MainScene() }
}

class MainScene : Scene() {
    private var inputText = ""
    private lateinit var responseTextView: Text
    private val npcName = "Rayze"
    private val npcBio = """
Rayze is the leader of the Crypts gang. He is a tough and strategic individual who values control over the town's power generator.
He is cunning and manipulative, often trying to outsmart his rivals. He is known for his leather armor and his tactical prowess.
He's a man of few words and prefers to get straight to the point. His demeanor can seem cold, but he cares deeply about his gang.
"""

    override suspend fun SContainer.sceneMain() {
        text("Talk to $npcName") {
            position(views.virtualWidth / 2, views.virtualHeight / 2)
            //anchor(.5, .5)
        }

        val inputPrompt = text("Click to type:") {
            position(views.virtualWidth / 2, views.virtualHeight / 2 + 50)
            //anchor(.5, .5)
        }

        val inputBox = text("") {
            position(views.virtualWidth / 2, views.virtualHeight / 2 + 100)
            //anchor(.5, .5)
            addUpdater { text = inputText }
            keys {
                down {
                    when (it.key) {
                        Key.ENTER -> {
                            println("Enter pressed. Sending input to OpenAIService.")
                            val userInput = inputText
                            println("User input: $userInput")
                            inputText = ""
                            val response = OpenAIService.getChatResponse(userInput, npcBio)
                            println("Response from OpenAIService: $response")

                            responseTextView.text = response
                        }
                        Key.BACKSPACE -> {
                            if (inputText.isNotEmpty()) {
                                inputText = inputText.dropLast(1)
                                println("Backspace pressed. Updated inputText: $inputText")
                            }
                        }
                        else -> {
                            if (it.character.isLetterOrDigit() || it.character.isWhitespace()) {
                                inputText += it.character
                                println("Key pressed: ${it.character}. Updated inputText: $inputText")
                            }
                        }
                    }
                }
            }
        }

        responseTextView = text("") {
            position(views.virtualWidth / 2, inputBox.y + 50)
            //anchor(.5, .0)
        }

        addUpdater {
            inputPrompt.text = "Type your message: $inputText"
        }
    }
}
