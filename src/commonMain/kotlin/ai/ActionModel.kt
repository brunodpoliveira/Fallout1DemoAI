package ai

import korlibs.datastructure.*
import korlibs.korge.ldtk.view.*
import kotlinx.coroutines.*
import npc.*
import utils.*

class ActionModel(
    private val ldtk: LDTKWorld,
    private val grid: IntIArray2,
    private val npcManager: NPCManager,
    private val playerInventory: Inventory,
    private val coroutineScope: CoroutineScope
) {

    private fun executeActions(actions: List<String>) {
        actions.forEach { action ->
            val parts = action.split(",")
            if (parts.size >= 3) {
                val actionType = parts[0]
                val actor = parts[1]
                val subject = parts[2]
                val location = if (parts.size > 3 && parts[3].isNotEmpty()) parts[3] else null
                val item = if (parts.size > 4 && parts[4].isNotEmpty()) parts[4] else null
                executeAction(actionType, actor, subject, location, item)
            }
        }
    }

    private fun executeAction(
        actionType: String,
        actor: String,
        subject: String,
        target: String?,
        item: String?
    ) {
        when (actionType) {
            "MOVE" -> {
                if (subject == "NPC") {
                    handleMoveAction(actor, null, target)
                } else {
                    handleMoveAction(actor, subject, null)
                }
            }
            "GIVE" -> handleGiveAction(actor, subject, item)
            "TAKE" -> handleTakeAction(actor, subject, item)
            "INTERACT" -> handleInteractAction(actor, subject)
            else -> {
                Logger.debug("Unknown action type: $actionType")
            }
        }
    }

    private fun handleMoveAction(actor: String, location: String?, targetNPC: String?) {
        val entityToMove = npcManager.npcs[actor]
        if (entityToMove == null) {
            Logger.debug("Unable to move $actor: NPC not found")
            return
        }

        coroutineScope.launch {
            val movement = MovementRegistry.getMovementForNPC(actor)
            if (movement == null) {
                Logger.debug("Unable to move $actor: Movement not registered")
                return@launch
            }

            when {
                targetNPC != null -> {
                    val targetEntity = npcManager.npcs[targetNPC]
                    if (targetEntity != null) {
                        Logger.debug("$actor is moving towards $targetNPC")
                        movement.moveToPoint(targetEntity.x, targetEntity.y)
                    } else {
                        Logger.debug("Unable to move $actor towards $targetNPC: Target NPC not found")
                    }
                }
                location != null -> {
                    Logger.debug("$actor is moving to sector $location")
                    movement.moveToSector(ldtk, location, grid)
                }
                else -> {
                    Logger.debug("Unable to move $actor: No valid destination provided")
                }
            }
        }
    }

    private fun handleInteractAction(actor: String, subject: String) {
        Logger.debug("$actor is interacting with $subject")
    }

    private fun handleGiveAction(giver: String, receiver: String, item: String?) {
        if (item != null) {
            if (giver != "Player" && receiver == "Player") {
                playerInventory.addItem(item)
                Logger.debug("$giver gave $item to the player")
            } else {
                Logger.debug("$giver gave $item to $receiver")
            }
        }
    }

    private fun handleTakeAction(taker: String, giver: String, item: String?) {
        if (item != null) {
            if (taker == "Player" && giver != "Player") {
                if (playerInventory.getItems().contains(item)) {
                    playerInventory.removeItem(item)
                    Logger.debug("Player took $item from $giver")
                }
            } else {
                Logger.debug("$taker took $item from $giver")
            }
        }
    }

    fun processNPCReflection(npcReflection: String, npcName: String): Triple<List<String>, Boolean, List<String>> {
        val (actions, isSecret, conspirators) = translateNextStepsToActionModel(npcReflection, npcName)
        coroutineScope.launch {
            executeActions(actions)
        }
        return Triple(actions, isSecret, conspirators)
    }

    private fun translateNextStepsToActionModel(nextSteps: String, npcName: String): Triple<List<String>, Boolean, List<String>> {
        val actionList = mutableListOf<String>()
        var isSecret = false
        val conspirators = mutableListOf<String>()

        // Regex patterns to match action phrases and metadata
        val movePattern = "(?i)(I'll|I will|Let's|We'll|We will) (move towards|go to) (\\w+)'s location".toRegex()
        val interactPattern = "(?i)(I'll|I will) (initiate a conversation|talk|speak|interact) with (\\w+)".toRegex()
        val givePattern = "(?i)(I'll|I will) give (\\w+) to (\\w+)".toRegex()
        val takePattern = "(?i)(I'll|I will) take (\\w+) from (\\w+)".toRegex()
        val secretPattern = "(?i)SECRET".toRegex()
        val conspiracyPattern = "(?i)CONSPIRACY - \\[(.*?)]".toRegex()

        nextSteps.lines().forEach { line ->
            when {
                movePattern.containsMatchIn(line) -> {
                    val match = movePattern.find(line)
                    val targetNPC = match?.groupValues?.get(3)
                    actionList.add("MOVE,$npcName,NPC,$targetNPC")
                }
                interactPattern.containsMatchIn(line) -> {
                    val match = interactPattern.find(line)
                    val targetNPC = match?.groupValues?.get(3)
                    actionList.add("INTERACT,$npcName,$targetNPC")
                }
                givePattern.containsMatchIn(line) -> {
                    val match = givePattern.find(line)
                    val item = match?.groupValues?.get(2)
                    val receiver = match?.groupValues?.get(3)
                    actionList.add("GIVE,$npcName,$receiver,$item")
                }
                takePattern.containsMatchIn(line) -> {
                    val match = takePattern.find(line)
                    val item = match?.groupValues?.get(2)
                    val giver = match?.groupValues?.get(3)
                    actionList.add("TAKE,$npcName,$giver,$item")
                }
                secretPattern.containsMatchIn(line) -> {
                    isSecret = true
                }
                conspiracyPattern.containsMatchIn(line) -> {
                    val match = conspiracyPattern.find(line)
                    conspirators.addAll(match?.groupValues?.get(1)?.split(",")?.map { it.trim() } ?: emptyList())
                }
                else -> {
                    Logger.debug("No matching action found in line: $line")
                }
            }
        }
        return Triple(actionList, isSecret, conspirators)
    }
}

//TODO add the capacity to remove movement so they can stop, esp if patrols are triggered
object MovementRegistry {
    private val movements: MutableMap<String, Movement> = mutableMapOf()

    fun addMovementForNPC(npc: String, movement: Movement) {
        movements[npc] = movement
    }

    fun getMovementForNPC(npc: String): Movement? {
        return movements[npc]
    }
}
