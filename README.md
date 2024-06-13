# Fallout1DemoAI
This project is a proof-of-concept demonstration of the Fallout 1 demo town, Scrapheap,
built using Korge and Kotlin. The main highlight of the project is the use of GPT-3.5 Turbo to
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
#TODO virtualcontroller explanations
- Player movement and interaction are placeholders: Arrow keys for directions and ENTER/Button West for entering dialog with a NPC
- Interact with NPCs by typing messages on the Dialog Window, and click the buttons with your mouse to interact with them.
- Hold and the right mouse button to in the Dialog message window to scroll through it 
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
4. Deploy using ./gradlew packageJvmFatJarProguard. Run it using build/libs/java -jar Fallout1DemoAI-all.jar
   
### Folder Structure
TODO redo this section
src/commonMain/kotlin/ai: Holds the AI files, such as the bios, the services (director, summ, openai), and a to-do Action Model.
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
- Korge developers for the game engine, and the Dungeon Starter Kit
- GNUGRAF for providing critical feedback and support

## TODO
- Refine the director to only give information to the correct NPC. For ex, If the player had a secret
  meeting with someone, and they were both sworn to secrecy, the director will not send the information
  to the other NPCs. If the character plans an ambush, only the conspirators will know about it.
  Otherwise, the information will be broadcast to everyone
- Prompt engineering to refine the NPCs: Give them OCEAN personality to make them more
  realistic and avoid making too much of a stonewall or a pushover and to avoid talking about
  characters, items, and locations that do not exist.
- Edit NPCBio to ensure it reflects current personality of the char in story, including relation with
  the main character
- Refine the Dialog Window input by including a virtual keyboard, and allowing the user
  to utilize the virtualcontroller buttons to send the messages and close the dialog.
- Refine the Dialog Window to make it block movement and interaction while it is open
- Refine the Dialog Window by making its upper portion focus on the characters and add the NPC mugshot
  like the Fallout's talking heads.
- Refine the game by adding UIProgressBar loading screen when it comes in and out of the DialogWindow
- Add movement and pathfinding logic to the NPCs so they can move from point to point
- Add stats and inventory management to both NPC and player classes.
- Create an inventory system and menu for the player.
- Develop a main, options, and pause menu for game navigation.
- Add the easy mode texts to the pause menu to make it more readable
- Add an Action Model: if the NPC says "we'll meet at the town square"
  the Action Model will set the NPC to move to the town square,
  if they say "I'll give you my pistol" the game will add a gun to the player's inventory
- Implement a simple combat system: if an NPC and player are in range and no walls are in between,
  enable hit detection.
- Add voice input for player interactions, allowing speech-to-text functionality.
- Move API keys to gradle.properties to enhance security.
- Use custom Fallout-themed sprites
- Create a custom level for this demo
- Add sound effects to the demo
- Add music to the demo
