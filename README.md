# Fallout1DemoAI

Install Godot Engine:

Download and install the latest version of Godot Engine from the official website. Godot provides builds for both Windows and Linux, so you can easily set it up on your respective systems.
Kotlin for Godot:

For Kotlin support, you'll need to integrate the Kotlin/JVM module for Godot, which allows you to write your game logic in Kotlin. This can be found on GitHub under the Godot Kotlin project. Follow the setup instructions carefully for both Windows and Ubuntu environments.
Ensure you have a compatible JDK installed on both systems for Kotlin development.
OpenAI API Setup:

Register an account with OpenAI and obtain API keys for GPT-3.5 turbo access. This will enable you to integrate AI-driven dialogues and NPC behavior.
Install any necessary libraries for making HTTPS requests in Kotlin, such as Ktor or OkHttp, to facilitate communication with the OpenAI API.
IDE Setup:

For Kotlin development, IntelliJ IDEA is highly recommended. It integrates well with Kotlin and supports both Windows and Linux. Install IntelliJ IDEA on both systems.
Configure IntelliJ IDEA with the JDK you're using and ensure it recognizes your Kotlin projects.
Version Control:

Install Git if it’s not already set up. Use Git for version control to manage your project code, especially when switching between Windows and Ubuntu environments.
GitHub or GitLab can be used to host your repository, enabling easy code sharing and collaboration.
Test OpenAI API Connectivity:

Before diving into the development, make a simple command-line application in Kotlin that makes an API request to OpenAI and outputs the response. This step ensures that your API access and HTTP client setup are functional.
Learning Resources:

Familiarize yourself with the Godot documentation and any tutorials on integrating external libraries with it.
The Kotlin language documentation will be invaluable for specific Kotlin-related questions.
Review OpenAI’s API documentation for guidelines on making requests and understanding response formats.
2. Start Small
   Begin with a small, manageable scope:

Create a basic Godot project: Start by setting up a new project in Godot. Create a simple scene to verify that your Godot installation is correctly set up.

Kotlin Script Integration: Try to write a simple Kotlin class, for instance, a character or an item, and integrate it into your Godot scene. Test if Godot correctly interacts with your Kotlin script.

API Connection Test in Game Context: Implement the OpenAI API call within a Godot project context. For example, create an NPC character that, when interacted with, sends a request to GPT-3.5 to generate a greeting and displays it.

3. Version Control
   Initialize a Git Repository: For your project and push it to GitHub or GitLab. Ensure you commit changes regularly to track your progress and secure your work.
4. Keep Testing
   Cross-Platform Compatibility: Regularly test your game on both Windows 10 and Ubuntu to ensure that there are no platform-specific issues, especially with Kotlin integration and OpenAI API calls.
   By following these steps, you'll have a solid foundation to build your project. Remember, complex projects benefit from an iterative, step-by-step approach. Get the basics right, ensure everything works as expected, and then proceed to build and integrate more complex features.

Fallout 1 Demo Helper
Great! Let’s tackle the next steps one by one. First, we'll add a biography to Rayze to make his replies more in-character. Then, we'll work on setting up an isometric perspective 2D world for the player to walk around. Finally, we’ll create a dialogue window for showing interactions.

Step 1: Character Backstory Integration
First, let's create a bio for Rayze and make sure the responses are in-character. We'll modify the OpenAIService to include this context.

OpenAIService.kt Update
We'll add a new parameter npcBio to our getChatResponse method and prepend it to the user input when making the request:

object OpenAIService {
private const val API_KEY = "your-openai-api-key"
private val service = OpenAiService(API_KEY, Duration.ofSeconds(30))

    private const val RAYZE_BIO = """
        Rayze is the leader of the Crypts gang. He is a tough and strategic individual who values control over the town's power generator. 
        He is cunning and manipulative, often trying to outsmart his rivals. He is known for his leather armor and his tactical prowess.
        He's a man of few words and prefers to get straight to the point. His demeanor can seem cold, but he cares deeply about his gang.
    """

    fun getChatResponse(userInput: String, npcBio: String = RAYZE_BIO): String {
        val message = ChatMessage("system", npcBio + "\nUser: " + userInput)
        val request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(listOf(message))
            .temperature(0.7)
            .maxTokens(100)
            .build()

        val response = service.createChatCompletion(request)
        return response.choices.firstOrNull()?.message?.content ?: "Sorry, I couldn't understand that."
    }
}
Step 2: Enhancing the MainScene for In-Character Responses
Next, modify the MainScene to use the in-character bio for Rayze when fetching responses.

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

        // Initialize the responseTextView here
        responseTextView = text("") {
            position(views.virtualWidth / 2, inputBox.y + 50)
            //anchor(.5, .0)
        }

        addUpdater {
            inputPrompt.text = "Type your message: $inputText"
        }
    }
}
Step 3: Creating an Isometric Perspective 2D World
To create an isometric perspective 2D world, we need to:

Set up the isometric grid and tiles.
Add the player character (Max Stone) and allow movement.
Sprite and Tile Setup
Let's start by setting up a basic isometric grid and player sprite. For simplicity, we can use colored rectangles to represent the tiles and character.

import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.korge.input.*
import korlibs.korge.render.*
import korlibs.math.geom.vector.*

suspend fun main() = Korge(windowSize = Size(800, 600), backgroundColor = Colors["#2b2b2b"]) {
val sceneContainer = sceneContainer()
sceneContainer.changeTo { MainScene() }
}

class MainScene : Scene() {
private var inputText = ""
private lateinit var responseTextView: Text
private val npcName = "Rayze"
private lateinit var player: Sprite

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

        // Initialize the responseTextView here
        responseTextView = text("") {
            position(views.virtualWidth / 2, inputBox.y + 50)
            //anchor(.5, .0)
        }

        addUpdater {
            inputPrompt.text = "Type your message: $inputText"
        }

        // Create isometric grid
        createIsometricGrid()

        // Add player character
        player = sprite(texture = createRect(20, 20, Colors.RED)) {
            position(50, 50)
        }

        // Add player movement
        addUpdater {
            val speed = 2
            if (input.keys[Key.LEFT]) player.x -= speed
            if (input.keys[Key.RIGHT]) player.x += speed
            if (input.keys[Key.UP]) player.y -= speed
            if (input.keys[Key.DOWN]) player.y += speed
        }
    }

    private fun SContainer.createIsometricGrid() {
        val gridSize = 10
        val tileWidth = 40.0
        val tileHeight = 20.0

        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                val posX = (x - y) * tileWidth / 2
                val posY = (x + y) * tileHeight / 2
                graphics {
                    fill(Colors.GRAY) {
                        moveTo(posX, posY)
                        lineTo(posX + tileWidth / 2, posY + tileHeight / 2)
                        lineTo(posX, posY + tileHeight)
                        lineTo(posX - tileWidth / 2, posY + tileHeight / 2)
                        close()
                    }
                }
            }
        }
    }
}
Step 4: Create a Dialog Window
Adding a simple dialog window to display interactions makes the user experience better. We can use a basic Container to hold our text elements.

class DialogWindow : Container() {
private val dialogBg = solidRect(300, 150, Colors.DARKGRAY) {
position(views.virtualWidth / 2 - 150, views.virtualHeight - 200)
}
private val dialogText = text("", font = DefaultTtfFont, textSize = 16.0) {
position(dialogBg.x + 10, dialogBg.y + 10)
}

    fun updateDialog(newText: String) {
        dialogText.text = newText
    }
}

// In MainScene
class MainScene : Scene() {
private var inputText = ""
private lateinit var dialogWindow: DialogWindow
private lateinit var player: Sprite

    // As before
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

                            dialogWindow.updateDialog(response) // Update dialog window
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

        // Initialize the responseTextView here
        responseTextView = text("") {
            position(views.virtualWidth / 2, inputBox.y + 50)
            //anchor(.5, .0)
        }

        addUpdater {
            inputPrompt.text = "Type your message: $inputText"
        }

        // Create isometric grid
        createIsometricGrid()

        // Add player character
        player = sprite(texture = createRect(20, 20, Colors.RED)) {
            position(50, 50)
        }

        // Add player movement
        addUpdater {
            val speed = 2
            if (input.keys[Key.LEFT]) player.x -= speed
            if (input.keys[Key.RIGHT]) player.x += speed
            if (input.keys[Key.UP]) player.y -= speed
            if (input.keys[Key.DOWN]) player.y += speed
        }

        // Add dialog window
        dialogWindow = DialogWindow().addTo(this)
    }

    private fun SContainer.createIsometricGrid() {
        val gridSize = 10
        val tileWidth = 40.0
        val tileHeight = 20.0

        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                val posX = (x - y) * tileWidth / 2
                val posY = (x + y) * tileHeight / 2
                graphics {
                    fill(Colors.GRAY) {
                        moveTo(posX, posY)
                        lineTo(posX + tileWidth / 2, posY + tileHeight / 2)
                        lineTo(posX, posY + tileHeight)
                        lineTo(posX - tileWidth / 2, posY + tileHeight / 2)
                        close()
                    }
                }
            }
        }
    }
}
This implementation ensures that:

Rayze’s Bio: Is used to generate in-character responses.
Isometric Grid: An isometric grid is created where the player can move.
Dialog Window: A dialog window displays interactions with NPCs.
Run the updated code to see these new features in action. If you have any further questions or run into any issues, feel free to ask!


