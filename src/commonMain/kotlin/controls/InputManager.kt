package controls

import interactions.*
import korlibs.korge.view.*
import kotlinx.coroutines.*

class InputManager(
    private val controllerManager: VirtualControllerManager,
    private val interactionManager: InteractionManager,
    private val coroutineScope: CoroutineScope
) {
    fun setupInput(container: Container) {
        controllerManager.apply {
            container.setupVirtualController()
            setupButtonActions(
                onAnyButton = { coroutineScope.launch { interactionManager.handleAnyButton() } },
                onWestButton = { coroutineScope.launch { interactionManager.handleWestButton() } },
                onSouthButton = { coroutineScope.launch { interactionManager.handleSouthButton() } },
                onNorthButton = { coroutineScope.launch { interactionManager.handleNorthButton() } }
            )
        }
    }
    fun getControllerInput(): Pair<Double, Double> {
        return controllerManager.getControllerInput()
    }
}
