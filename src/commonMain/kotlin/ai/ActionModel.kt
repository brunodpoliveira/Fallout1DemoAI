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
        location: String?,
        item: String?
    ) {
        when (actionType) {
            "MOVE" -> handleMoveAction(actor, location)
            "GIVE" -> handleGiveAction(actor, subject, item)
            "TAKE" -> handleTakeAction(actor, subject, item)
            else -> {
                println("Unknown action type: $actionType")
            }
        }
    }

    private fun handleMoveAction(actor: String, location: String?) {
        if (location != null) {
            val entityToMove = npcManager.npcs[actor]
            entityToMove?.let { entity ->
                coroutineScope.launch {
                    val movement = Movement(entity, npcManager.pathfinding)
                    movement.moveToSector(ldtk, location, grid)
                }
            }
        }
    }

    private fun handleGiveAction(giver: String, receiver: String, item: String?) {
        if (item != null) {
            if (giver != "Player" && receiver == "Player") {
                playerInventory.addItem(item)
                println("$giver gave $item to the player")
            } else {
                println("$giver gave $item to $receiver")
            }
        }
    }

    private fun handleTakeAction(taker: String, giver: String, item: String?) {
        if (item != null) {
            if (taker == "Player" && giver != "Player") {
                if (playerInventory.getItems().contains(item)) {
                    playerInventory.removeItem(item)
                    println("Player took $item from $giver")
                }
            } else {
                println("$taker took $item from $giver")
            }
        }
    }

    fun processNPCReflection(npcReflection: String, npcName: String): List<String> {
        val actions = translateNextStepsToActionModel(npcReflection, npcName)
        coroutineScope.launch {
            executeActions(actions)
        }
        return actions
    }

    private fun translateNextStepsToActionModel(nextSteps: String, npcName: String): List<String> {
        val actionList = mutableListOf<String>()

        // Simple regex patterns to match action phrases
        val movePattern = "(?i)(I'll|I will|Let's|We'll|We will) meet at the (\\w+)".toRegex()
        val givePattern = "(?i)(I'll|I will) give you (my |the )?(\\w+)".toRegex()

        nextSteps.lines().forEach { line ->
            when {
                movePattern.containsMatchIn(line) -> {
                    val match = movePattern.find(line)
                    val location = match?.groupValues?.get(2)?.uppercase()
                    actionList.add("MOVE,$npcName,Player,$location,")
                    println("Adding move action: MOVE,$npcName,Player,$location,")
                }
                givePattern.containsMatchIn(line) -> {
                    val match = givePattern.find(line)
                    val item = match?.groupValues?.get(3)?.uppercase()
                    actionList.add("GIVE,$npcName,Player,,$item")
                    println("Adding give action: GIVE,$npcName,Player,,$item")
                }
                else -> {
                    println("No matching action found in line: $line")
                }
            }
        }

        return actionList
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
