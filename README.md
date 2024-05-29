# Fallout1DemoAI
This project is a proof-of-concept demonstration of the Fallout 1 demo town, Scrapheap, 
built using Korge and Kotlin. The main highlight of the project is the use of GPT-3.5 turbo to 
power the NPC interactions, making them dynamic and conversational.

## Game Overview

### Objective
The player interacts with the inhabitants of Scrapheap, a town dominated by two competing gangs: the Crypts and the Fools. 
The objective is to solve the gang problem plaguing the town. 
The game can be completed in multiple ways:
1. **Side with the Crypts**: Help them wipe out the Fools and maintain control of the generator.
2. **Side with the Fools**: Help them wipe out the Crypts and transfer the rule of the town to them.
3. **Neutral Approach**: Wait for the gangs to wipe each other out, then finish off the survivors.
4. **Destruction**: Use wire cutters to destroy the generator, dooming everyone in the town.

### Controls
- Player movement and interaction are placeholders: WASD for directions and ENTER for entering dialog with a NPC
- Interact with NPCs by typing messages on the Dialog Window, and click the buttons with your mouse to interact with them.
- Future versions will include voice input and additional controls.

### Execution
To run the game, ensure you have [Gradle](https://gradle.org/install/) installed. Execute the following command:

```bash
./gradlew runJvmAutoreload
```
This will compile and run the game with hot-reloading enabled, allowing you to see changes without restarting the application.

### Game Setup
1. Initialization: The game initializes with the player character, NPCs with predefined bios, and a general context for the wider story.
2. Context Injection: At the start of a conversation, the NPC's biography and current game context are sent to the AI service, and updated at the end of it; 
this will ensure that the story and the NPCs evolve organically as the player interacts with them and the world.
3. Dynamic Conversations: Based on player input, the NPC responses are generated dynamically.

## Development Setup
### Prerequisites
1. Java JDK
2. Gradle
3. Kotlin

### Build and Run
1. Clone the repository.
2. Ensure the API key for GPT-3.5 turbo is set in gradle.properties.
3. Run the game using: ./gradlew runJvmAutoreload

### Folder Structure
src/commonMain/kotlin/ai: Holds the AI files, such as the bios, the services (director, summ, openai), and a to-do Action Model.
src/commonMain/kotlin/controls: Placeholder for control logic.
src/commonMain/kotlin/grid: Grid creation logic
src/commonMain/kotlin/lvls: Junkdemo is here, initializing the NPCs and, soon, the objects in the level as well.
src/commonMain/kotlin/manager: Includes ObjectList (empty so far), ObjectManager (to-do), InventoryManager (to-do), EntityManager (Entity, Player, and NPC classes are built here), and CollisionManager.
src/commonMain/kotlin/ui: Includes DialogWindow.
src/commonMain/kotlin: Main entry point main.kt.
src/commonMain/assets/: Placeholder assets for UI and sprites.
build.gradle.kts: Build configuration file.

## Contributing
Contributions are welcome! Please ensure any pull requests include proper documentation and 
adhere to the project's coding standards.

## Acknowledgements
- The Fallout franchise by Bethesda Softworks
- OpenAI and theokanning for providing the GPT-3.5 Turbo API
- Korge developers for the game engine
- GNUGRAF for providing critical feedback and support

## TODO
- create a level using a black-and-white depth map; black for floor white for walls, red for boxes, etc
- Add voice input for player interactions, allowing speech-to-text functionality.
- Implement NPCs thinking out loud their next steps to make interactions more engaging.
- Add a virtual keyboard for better user interaction.
- Integrate controls from a sample Hello World Ktor demo.
- Prepare and conduct a presentation for version 1.
- Move API keys to gradle.properties to enhance security.
- Add sprites and tiles from the RPG starter pack for better visual fidelity.
- Implement collision detection with objects.
- implement said objects in the game-world.
- Develop a main, options, and pause menu for game navigation.
- Create an inventory system for the player.
- Add stats and inventory management to both NPC and player classes.
- add an Action Model: if the NPC says "we'll meet at noon at the town square" 
the Action Model will scan for that and program the NPC to move to the town square, 
if they say "I'll give you my pistol" the game will add a gun to the player's inventory
- Implement a simple combat system: if an NPC and player are in range and no walls are in between, 
enable hit detection.
