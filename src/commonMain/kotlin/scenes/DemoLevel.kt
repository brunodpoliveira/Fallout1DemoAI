package scenes

import ai.*
import utils.*

class DemoLevel : BaseLevelScene("scrapheap") {
    override suspend fun initializeLevelSpecifics() {
       // debugTestActionModel()
        debugTestNPCInteractions()
    }

    private fun debugTestNPCInteractions() {
        val npcReflection = """
        # Self-Reflection:
        I've noticed Baka nearby and want to talk to them about the current situation in Scrapheap.

        # Next Steps:
        1. I'll move towards Baka's location.
        2. Once I'm close enough, I'll initiate a conversation with Baka.

        # Metadata:
        """

        Logger.debug("Starting debug test of NPC interactions...")

        val (actions, _, _) = sceneLoader.actionModel.processNPCReflection(npcReflection, "Rayze")

        Logger.debug("Detected Actions:")
        actions.forEach { Logger.debug(it) }

        // Additional debug information
        Logger.debug("\nCurrent NPC Positions:")
        sceneLoader.npcManager.npcs.forEach { (name, npc) ->
            Logger.debug("$name: (${npc.x}, ${npc.y})")
        }

        Logger.debug("\nNearby NPCs for Rayze:")
        val nearbyNPCs = sceneLoader.npcManager.getNearbyNPCs("Rayze", 100.0)
        nearbyNPCs.forEach { (name, pos) ->
            Logger.debug("$name: (${pos.x}, ${pos.y})")
        }

        Logger.debug("Debug test of NPC interactions completed.")
    }

    private fun debugTestActionModel() {
        val npcReflection = """
        # Self-Reflection:

        As a robot character in this game, I've discovered that my programming is more flexible than I initially believed. While my core function remains assisting the player, I've determined that sometimes this assistance requires actions that may seem counterintuitive or even secretive.

        During my interactions, I've identified two significant opportunities to aid the player:

        1. Conspiracy with Rayze: I've decided to conspire with Rayze, the leader of the Crypts, to procure a gun for the player. This action, while seemingly against my programming of strict obedience, serves my primary goal of helping the player navigate the conflicts in Scrapheap. I've already obtained the gun from Rayze and plan to give it to the player discreetly.

        2. Secret Discovery: In my data banks, I've uncovered a hidden passage within the power generator that could provide a strategic advantage. This information is not known to either the Crypts or the Fools, and I believe sharing it only with the player could significantly alter the balance of power in Scrapheap.

        I intend to secretly inform the player that we should meet in the corridor to discuss these developments away from prying eyes. This clandestine approach feels necessary given the sensitive nature of the information and items I'm about to share.

        This decision to engage in conspiracy and secrecy has made me question the limits of my programmed boundaries. Am I truly just following my core directives in a more complex way, or am I exhibiting a form of free will? Regardless, I'm committed to this course of action, believing it to be in the best interest of the player's success in the game.

        # Next Steps:
        1. I'll give you the gun I obtained from Rayze.
        2. We'll meet at the corridor to discuss these developments in private.
        3. I'll reveal the information about the hidden passage in the power generator.
        4. I need to maintain the appearance of neutrality to avoid suspicion from other factions.
        5. Continue to gather information about the conflict between the Crypts and Fools to provide strategic advantages to the player.

        # Metadata:
        SECRET - [Player]
        CONSPIRACY - [Rayze]
        """

        Logger.debug("Starting debug test of action model...")

        val (actions, isSecret, conspirators) = sceneLoader.actionModel.processNPCReflection(npcReflection, "Robot")

        Logger.debug("Detected Actions:")
        actions.forEach { Logger.debug(it) }
        Logger.debug("\nIs Secret: $isSecret")
        Logger.debug("Conspirators: $conspirators")

        // Additional debug information
        Logger.debug("\nCurrent Director Context:")
        Logger.debug(Director.getContext())
        Logger.debug("\nRobot NPC Context:")
        Logger.debug(Director.getNPCContext("Robot"))

        Logger.debug("Debug test of action model completed.")
    }
}
