package ai

import agent.core.*
import agent.system.*
import korlibs.datastructure.*
import korlibs.korge.ldtk.view.*
import kotlinx.coroutines.*
import utils.*

class ActionModel(
    private val ldtk: LDTKWorld,
    private val grid: IntIArray2,
    private val agentManager: AgentManager,
    private val playerInventory: Inventory,
    private val coroutineScope: CoroutineScope
) {

    private fun executeActions(actions: List<String>) {
        actions.forEach { action ->
            Logger.debug("Executing action: $action")
            val parts = action.split(",")
            Logger.debug("Action parts: ${parts.joinToString(", ")}")
            if (parts.size >= 3) {
                val actionVerb = ActionVerb.fromString(parts[0])
                val actor = parts[1]
                val subject = parts[2]
                val item = if (parts.size > 3 && parts[3].isNotEmpty()) parts[3] else null
                val target = if (parts.size > 4 && parts[4].isNotEmpty()) parts[4] else null
                if (actionVerb != null) {
                    executeAction(actionVerb, actor, subject, item, target)
                } else {
                    Logger.debug("Unknown action verb: ${parts[0]}")
                }
            }
        }
    }

    fun executeAction(
        actionType: ActionVerb,
        actor: String,
        subject: String,
        item: String?,
        target: String?
    ) {
        Logger.debug("Executing action: type=$actionType, actor=$actor, subject=$subject, item=$item, target=$target")
        when (actionType) {

            ActionVerb.MOVE  -> {
                if (subject == "NPC" ) {
                    Logger.warn("Moving NPC to another NPC")
                    handleMoveAction(actor, null, item) // Use 'item' as the target NPC
                } else if (subject == "COORDINATE") {
                    Logger.warn("Moving player to another coordinate")
                    handleMoveAction(actor, target, null)
                }else {
                    handleMoveAction(actor, subject, null)
                }
            }
            ActionVerb.GIVE  -> handleGiveAction(actor, subject, item)
            ActionVerb.TAKE  -> handleTakeAction(actor, subject, item)
            ActionVerb.INTERACT -> handleInteractAction(actor, subject)
            else -> {
                Logger.debug("Unknown action type: $actionType")
            }
        }
    }

    private fun handleMoveAction(actor: String, location: String?, targetNPC: String?) {
        Logger.debug("Handling MOVE action: actor=$actor, location=$location, targetNPC=$targetNPC")
        val entityToMove = agentManager.entityViews[actor]
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
                    val targetEntity = agentManager.entityViews[targetNPC]
                    if (targetEntity != null) {
                        Logger.debug("$actor is moving towards $targetNPC")
                        movement.moveToPoint(targetEntity.x, targetEntity.y)
                    } else {
                        Logger.debug("Unable to move $actor towards $targetNPC: Target NPC not found")
                    }
                }
                location != null -> {
                    val coordinatePattern = """\[(\d+\.\d+),(\d+\.\d+)\]""".toRegex()
                    val matchResult = coordinatePattern.find(location)
                    if (matchResult != null) {
                        val targetX = matchResult.groupValues[1].toDouble()
                        val targetY = matchResult.groupValues[2].toDouble()

                        movement.moveToPoint(targetX, targetY)
                    } else {
                        movement.moveToSector(ldtk, location, grid)

                    }
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
        Logger.debug("Entering handleGiveAction: Giver=$giver, Receiver=$receiver, Item=$item")
        if (item != null) {
            val giverInventory = agentManager.getAgentInventory(giver)
            val receiverInventory = agentManager.getAgentInventory(receiver)

            Logger.debug("Giver inventory before: ${giverInventory?.getItems()}")
            Logger.debug("Receiver inventory before: ${receiverInventory?.getItems()}")

            if (giverInventory != null && receiverInventory != null) {
                if (giverInventory.hasItem(item)) {
                    val removed = giverInventory.removeItem(item)
                    Logger.debug("Item removed from giver: $removed")
                    if (removed) {
                        receiverInventory.addItem(item)
                        Logger.debug("$giver gave $item to $receiver")
                    } else {
                        Logger.debug("Failed to remove $item from $giver's inventory")
                    }
                } else {
                    Logger.debug("$giver doesn't have $item to give")
                }
            } else {
                Logger.debug("Invalid NPCs for item exchange: giverInventory=$giverInventory, receiverInventory=$receiverInventory")
            }

            Logger.debug("Giver inventory after: ${giverInventory?.getItems()}")
            Logger.debug("Receiver inventory after: ${receiverInventory?.getItems()}")
        } else {
            Logger.debug("No item specified for GIVE action")
        }
        Logger.debug("Exiting handleGiveAction")
    }

    private fun handleTakeAction(taker: String, giver: String, item: String?) {
        if (item != null) {
            val takerInventory = agentManager.getAgentInventory(taker)
            val giverInventory = agentManager.getAgentInventory(giver)

            if (takerInventory != null && giverInventory != null) {
                if (giverInventory.hasItem(item)) {
                    if (giverInventory.removeItem(item)) {
                        takerInventory.addItem(item)
                        Logger.debug("$taker took $item from $giver")
                    }
                } else {
                    Logger.debug("$giver doesn't have $item to take")
                }
            } else {
                Logger.debug("Invalid NPCs for item exchange")
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
        val movePattern = """(?i)(I'll|I will|Let's|We'll|We will) (move towards|go to) (\w+)'s location""".toRegex()
        val interactPattern = """(?i)(I'll|I will) (initiate a conversation|talk|speak|interact) with (\w+)""".toRegex()
        val givePattern = """(?i)(I'll|I will) give ([\w\s]+) to (\w+)""".toRegex()
        val takePattern = """(?i)(I'll|I will) take ([\w\s]+) from (\w+)""".toRegex()
        val secretPattern = """(?i)SECRET""".toRegex()
        val conspiracyPattern = """(?i)CONSPIRACY - \[(.*?)]""".toRegex()

        nextSteps.lines().forEach { line ->
            when {
                movePattern.containsMatchIn(line) -> {
                    val match = movePattern.find(line)
                    val targetNPC = match?.groupValues?.get(3)
                    Logger.debug("Detected MOVE action: targetNPC=$targetNPC")
                    actionList.add("MOVE,$npcName,NPC,$targetNPC")
                }
                interactPattern.containsMatchIn(line) -> {
                    val match = interactPattern.find(line)
                    val targetNPC = match?.groupValues?.get(3)
                    actionList.add("INTERACT,$npcName,$targetNPC")
                }
                givePattern.containsMatchIn(line) -> {
                    val match = givePattern.find(line)
                    val item = match?.groupValues?.get(2)?.trim()
                    val receiver = match?.groupValues?.get(3)
                    Logger.debug("Detected GIVE action: item=$item, receiver=$receiver")
                    actionList.add("GIVE,$npcName,$receiver,$item")
                }
                takePattern.containsMatchIn(line) -> {
                    val match = takePattern.find(line)
                    val item = match?.groupValues?.get(2)?.trim()
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
    private val movements: MutableMap<String, AgentMovement> = mutableMapOf()

    fun addMovementForNPC(npc: String, movement: AgentMovement) {
        movements[npc] = movement
    }

    fun getMovementForNPC(npc: String): AgentMovement? {
        return movements[npc]
    }
}
