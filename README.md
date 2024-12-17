# Fallout1DemoAI
This project is a proof-of-concept demonstration of the Fallout 1 demo town, Scrapheap,
built using Korge and Kotlin. The main highlight of the project is the use of LLMs to
power the NPC interactions, making them dynamic and conversational.

## Collaborating
You're welcome to add any of the TODO features listed at the bottom of this Readme.
Please ensure that your code is functioning correctly, and is properly documented.
It is heavily recommended that any of your collaborations be pushed to a new branch.
Pay attention that you're developing on top of the latest commit, available in the dev branch.
GitHub link: [Link](https://github.com/brunodpoliveira/Fallout1DemoAI)

## Game Overview

### Objective
The player interacts with the inhabitants of Scrapheap, a town dominated by two competing gangs: the Crypts
and the Fools. The objective is to solve the gang problem plaguing the town.
The game can be completed in multiple ways:
1. **Side with the Crypts**: Help them wipe out the Fools and maintain control of the generator.
2. **Side with the Fools**: Help them wipe out the Crypts and transfer the rule of the town to them.
3. **Side with the Civilians**: Negotiate a peace-deal or wipe out both gangs to return the city to the townsfolk
4. **Neutral Approach**: Wait for the gangs to wipe each other out, then finish off the survivors.
5. **Destruction**: Use wire cutters to destroy the generator, dooming everyone in the town.

### Controls
#TODO virtualcontroller explanations
- Player movement and interaction are placeholders: Arrow keys for directions and Z/West Button (the bottom button) for entering dialog with a NPC
- Press the Return/North Button (the middle button) to pause the game. You can access the Notes from this menu
- Press Space/South Button (the top button) to attack and interact with objects, such as chests
- In the Dialog Window, interact with NPCs by typing messages on the Dialog Window, and click the buttons  with your mouse to interact with them.
- Hold and drag the right mouse button in the Dialog message window to scroll through it
- Future versions will include voice input and additional controls.

### Execution
To run the game, ensure you have [Gradle](https://gradle.org/install/) installed. Execute the following
command:

```bash
./gradlew runJvmAutoreload
```
This will compile and run the game with hot-reloading enabled, allowing you to see changes without
restarting the application.

### Game Setup
1. Initialization: The game initializes with the player character, NPCs with predefined bios, and a general context for the wider story.
2. Context Injection: At the start of a conversation, the NPC's biography and current game context are sent to the AI service, and updated at the end of it; this will ensure that the story and the NPCs evolve organically as the player interacts with them and the world.
3. Dynamic Conversations: Based on player input, the NPC responses are generated dynamically.

## Development Setup
### Prerequisites
1. Java JDK
2. Gradle
3. Kotlin

### Build and Run
1. Clone the repository.
2. Ensure the API key for OpenAI services is set in gradle.properties and config.properties.
3. Run the game using: ./gradlew runJvmAutoreload
4. Deploy using ./gradlew packageJvmFatJar. Run it using java -jar build/libs/Fallout1DemoAI-all.jar

### Folder Structure
#TODO update folder structure
src/
├── commonMain/
│   └── kotlin/
│       ├── ai/
│       │   ├── ActionModel.kt
│       │   ├── ConversationPostProcessingServices.kt
│       │   ├── Director.kt
│       │   ├── NPCBio.kt
│       │   └── OpenAIService.kt
│       ├── bvh/
│       │   ├── BvhEntity.kt
│       │   └── BvhWorld.kt
│       ├── combat/
│       │   ├── Combat.kt
│       │   └── CombatManager.kt
│       ├── controls/
│       │   ├── InputManager.kt
│       │   └── VirtualControllerManager.kt
│       ├── dialog/
│       │   └── DialogManager.kt
│       ├── img/
│       │   ├── ImageAnimationView2.kt
│       │   ├── ImageDataView2.kt
│       │   ├── PixelAnchorable.kt
│       │   └── TextDisplayManager.kt
│       ├── interactions/
│       │   └── InteractionManager.kt
│       ├── maps/
│       │   └── MapManager.kt
│       ├── movement/
│       │   └── PlayerMovementController.kt
│       ├── npc/
│       │   ├── Movement.kt
│       │   ├── NPCManager.kt
│       │   └── Pathfinding.kt
│       ├── player/
│       │   └── PlayerManager.kt
│       ├── raycasting/
│       │   └── Raycaster.kt
│       ├── scenes/
│       │   ├── GameOverScene.kt
│       │   ├── JunkDemoScene.kt
│       │   ├── MainMenuScene.kt
│       │   ├── OptionsScene.kt
│       │   └── SceneLoader.kt
│       ├── ui/
│       │   ├── DialogWindow.kt
│       │   ├── PauseMenu.kt
│       │   ├── PlayerStatsUI.kt
│       │   └── UIManager.kt
│       └── utils/
│           ├── EntityStats.kt
│           ├── Inventory.kt
│           └── RayResultExtensions.kt
└── main.kt

- ai: Contains AI-related logic and models
- bvh: Bounding Volume Hierarchy implementation
- combat: Combat-related classes
- controls: Input and controller management
- dialog: Dialog management system
- img: Image and animation-related utilities
- interactions: Interaction management
- maps: Map-related functionality
- movement: Player movement control
- npc: NPC-related classes and pathfinding
- player: Player management
- raycasting: Raycasting implementation
- scenes: Different game scenes and scene loading
- ui: User interface components
- utils: Utility classes and extensions

The main.kt file serves as the entry point for the application.

## Contributing
Contributions are welcome! Please ensure any pull requests include proper documentation and
adhere to the project's coding standards.

## Acknowledgements
- The Fallout franchise by Bethesda Softworks/Interplay
- OpenAI, theokanning, and Lambdua for providing the GPT Tech and API
- Korge developers for the game engine, support in their Discord server, and the Dungeon Starter Kit
- GNUGRAF for providing critical feedback and support
- mikiz from itch.io for their crosshair pack
- szadiart from itch.io for their Post Apo tile pack

## Troubleshooting: Run a .jar file
The game is packaged as a .jar. Below is how to run it:

Linux:

If you have Java installed, you should be able to run it. Check if Java is installed with:

```
java --version
```

If you see an error instead of the version number,
install Java runtime environment using the following command:

```
sudo apt install default-jre
```

The first step is to open the file manager from the system menu and navigate to the jar
file which you want to run.

Then, right-click on the jar app and select Properties.
From there, select Permissions and enable Allow executing file as program.
That made the file executable. But you have yet to select which app it should use to run the jar files.

To select an Application to start the jar files, again, click on the jar file and choose the second option (Open with Other Application) and choose the OpenJDK Java Runtime option That's it. Now, you can start the jar application like you do with any other files by pressing the Enter key.

Windows:

To run a JAR file on Windows 10 or Windows 11, right-click it, then select Open With > Java Platform SE Binary.

Step 1: Check if Java Is Already Installed on Your PC

It's worth checking if you have Java installed on your PC already, and if so, you can skip the Step 2 below.

To do that, first, launch the Run box by pressing Windows+R.
Then, Type "cmd" in the Run box and press Enter.

In the Command Prompt window that opens, type the following command and press Enter:

```
java -version
```

If you have Java installed on your PC, you'll see your Java version.
In this case, skip Step 2 below and head straight to Step 3.

If Command Prompt displays the following message:
"'java' is not recognized as an internal or external command",
then you don't have Java installed. In this case, install the utility using Step 2 below.

Step 2: Download and Install Java on Windows

You can download and install Java for free on your Windows PC. To do that, first, launch a web browser on your computer and open the Java download web page.
There, in the "Windows" section, download the appropriate file for your machine.

When Java is downloaded, double-click the installer file to install the utility on your PC.
Then, restart your Windows 10 or Windows 11 PC.

Step 3: Run JAR Files on Windows 10 or Windows 11

Now that you have Java installed, you're ready to launch your JAR files. To do that, first, open a File Explorer window and locate your JAR file. Find the JAR file. Right-click your JAR file and choose Open With > Java(TM) Platform SE Binary. If you don't see that option in the "Open With" menu, then click "Choose Another App" and you'll see the option.
Select Open With > Java(TM) Platform SE Binary from the menu. Your JAR file will launch,
and you'll be able to interact with it. And that's all there is to it.

Mac:

1: Install Java if you don't already have it on your computer.
You cannot open JAR files without Java installed. To install it, go to https://www.java.com/br/download/ and click Download below the most current version of Java, then open it when the download is complete.
To install a non-Apple file on your Mac, first click OK on the prompt, then on the Apple menu, click System Preferences, click Security & Privacy, unlock the menu, click Open Anyway next to the file name file and then select Open when prompted.

2: Try double-clicking the JAR file to open it.
Try double-clicking the JAR file to open it. If it is an executable and Java is installed,
the file should open. If it still doesn't open...

3: Update Java.
If the JAR file displays an error when double-clicking it, you may need to update Java.
To do it: Open the Apple Mac Apple menu. Click System Preferences. Click Java. Click on the Update
tab. Click Update now.

### Attach a Debugger
To attach a debugger in a .jar running outside an IDE or equivalent development environment, run the below command:

```
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar Fallout1DemoAI-all.jar
```

Or:
go to config.properties and set korge.env to development. You can also activate or deactivate the creation of a log file as well by setting logging.file.enabled to true

## TODO 
------------
1. Project Overview
    - Augment action model for NPC-to-NPC interactions
    - Enable autonomous NPC behavior (conversations, routines, interactions)
    - Allow for zero-player game scenarios

2. Key Components to Modify/Extend
    - NPCManager: Handle NPC-NPC interactions and awareness
    - ActionModel: Incorporate NPC-NPC actions and planning
    - DialogManager: Handle NPC-NPC conversations
    - Director: Orchestrate overall NPC behaviors and events

3. Required Systems/Features
   3.1 NPC Awareness
   3.2 Decision-Making
   3.3 Pathfinding
   3.4 Interaction System
   3.5 AI Planning
   3.6 Event Broadcasting
   3.7 Item Exchange
   3.8 Scheduling
   3.9 State Management
   3.10 Conflict Resolution

4. Performance Considerations
    - Optimize for multiple autonomous NPCs

5. Next Steps for Development
    - Implement complex NPC-to-NPC interactions
    - Develop NPC-to-NPC dialogue system
    - Create scheduling system for NPC actions
    - Implement item exchange mechanics (doing) --> Implement TAKE command testing
    - Develop goal-driven NPC behavior

6. Key Files to Focus On
    - ActionModel.kt
    - NPCManager.kt
    - Movement.kt
    - NPC behavior and AI decision-making files

7. Challenges to Address
    - Dynamic event response and action interruption
    - Managing multiple simultaneous NPC actions
    - Balancing autonomy and performance

8. Integration Points
    - NPC interactions affecting game state and player experience
    - Integrating with existing dialogue system

9. Future Goals
    - Implement advanced AI behavior (e.g., behavior trees, utility AI)
    - Create NPC relationship and alliance system

-----------
