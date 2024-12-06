package controls

import korlibs.korge.view.*
import kotlinx.coroutines.*
import interactions.PlayerInteractionHandler

class InputManager(
    private val controllerManager: VirtualControllerManager,
    private val playerInteractionHandler: PlayerInteractionHandler,
    private val coroutineScope: CoroutineScope
) {
    fun setupInput(container: Container) {
        controllerManager.apply {
            container.setupVirtualController()
            setupButtonActions(
                onAnyButton = { coroutineScope.launch {
                    playerInteractionHandler.handleAnyButton()
                }},
                onWestButton = {
                    if (controllerManager.combatManager.isPlayerTurn()) {
                        coroutineScope.launch { playerInteractionHandler.handleWestButton() }
                    }
                },
                onSouthButton = {
                    if (controllerManager.combatManager.isPlayerTurn()) {
                        coroutineScope.launch { playerInteractionHandler.handleSouthButton() }
                    }
                },
                onNorthButton = {
                    if (controllerManager.combatManager.isPlayerTurn()) {
                        coroutineScope.launch { playerInteractionHandler.handleNorthButton() }
                    }
                }
            )
        }
    }

    fun getControllerInput(): Pair<Double, Double> {
        return controllerManager.getControllerInput()
    }
}
