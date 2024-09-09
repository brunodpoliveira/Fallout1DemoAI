package ai

import korlibs.datastructure.*
import korlibs.korge.ldtk.view.*
import korlibs.math.geom.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import npc.Movement
import utils.Inventory

class ActionModel(private val ldtk: LDTKWorld, private val grid: IntIArray2, private val gridSize: Size) {
    private val npcInventories: MutableMap<String, Inventory> = mutableMapOf()
    private val playerInventory: Inventory = Inventory()

    private fun executeAction(action: String, actor: String, subject: String, location: String?, item: String?) {
        when (action) {
            "MOVE" -> moveActorToLocation(actor, location)
            "GIVE" -> giveItemToActor(actor, subject, item)
            "TAKE" -> takeItemFromActor(actor, subject, item)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun moveActorToLocation(actor: String, location: String?) {
        val movement = MovementRegistry.getMovementForNPC(actor)
        movement?.let { move ->
            location?.let { loc ->
                GlobalScope.launch {
                    move.moveToSector(ldtk, loc, grid)
                }
            }
        }
    }

    private fun giveItemToActor(giver: String, receiver: String, itemName: String?) {
        itemName?.let {
            if (giver == "Player") {
                val item = playerInventory.getItems().find { it == itemName }
                item?.let {
                    playerInventory.removeItem(it)
                    npcInventories.getOrPut(receiver) { Inventory() }.addItem(it)
                }
            } else {
                npcInventories[giver]?.let { giverInventory ->
                    val item = giverInventory.getItems().find { it == itemName }
                    item?.let {
                        giverInventory.removeItem(it)
                        if (receiver == "Player") {
                            playerInventory.addItem(it)
                        } else {
                            npcInventories.getOrPut(receiver) { Inventory() }.addItem(it)
                        }
                    }
                }
            }
        }
    }

    private fun takeItemFromActor(taker: String, giver: String, itemName: String?) {
        itemName?.let {
            if (giver == "Player") {
                val item = playerInventory.getItems().find { it == itemName }
                item?.let {
                    playerInventory.removeItem(it)
                    npcInventories.getOrPut(taker) { Inventory() }.addItem(it)
                }
            } else {
                npcInventories[giver]?.let { giverInventory ->
                    val item = giverInventory.getItems().find { it == itemName }
                    item?.let {
                        giverInventory.removeItem(it)
                        if (taker == "Player") {
                            playerInventory.addItem(it)
                        } else {
                            npcInventories.getOrPut(taker) { Inventory() }.addItem(it)
                        }
                    }
                }
            }
        }
    }

    fun processNPCReflection(npcReflection: String): List<String> {
        val actions = translateNextStepsToActionModel(npcReflection)
        actions.forEach { actionModel ->
            val (action, actor, subject, location, item) = actionModel.split(",")
            executeAction(action, actor, subject, location, item)
        }
        return actions
    }

    private fun translateNextStepsToActionModel(nextSteps: String): List<String> {
        val actionList = mutableListOf<String>()

        // Simple regex patterns to match action phrases
        val movePattern = "(?i)meet at the (\\w+)".toRegex()
        val givePattern = "(?i)give you (my |the )?(\\w+)".toRegex()

        nextSteps.lines().forEach { line ->
            when {
                movePattern.containsMatchIn(line) -> {
                    val match = movePattern.find(line)
                    val location = match?.groupValues?.get(1)?.uppercase()
                    actionList.add("MOVE,NPC,Player,$location,")
                }
                givePattern.containsMatchIn(line) -> {
                    val match = givePattern.find(line)
                    val item = match?.groupValues?.get(2)?.uppercase()
                    actionList.add("GIVE,NPC,Player,,${item}")
                }
            }
        }

        return actionList
    }
}

object MovementRegistry {
    private val movements: MutableMap<String, Movement> = mutableMapOf()

    fun addMovementForNPC(npc: String, movement: Movement) {
        movements[npc] = movement
    }

    fun getMovementForNPC(npc: String): Movement? {
        return movements[npc]
    }
}
