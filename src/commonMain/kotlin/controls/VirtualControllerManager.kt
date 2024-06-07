package controls

import korlibs.event.*
import korlibs.korge.view.*
import korlibs.korge.virtualcontroller.*
import korlibs.math.geom.*

class VirtualControllerManager {
    lateinit var virtualController: VirtualController

    fun Container.setupVirtualController() {
        virtualController = virtualController(
            sticks = listOf(
                VirtualStickConfig(
                    left = Key.LEFT,
                    right = Key.RIGHT,
                    up = Key.UP,
                    down = Key.DOWN,
                    lx = GameButton.LX,
                    ly = GameButton.LY,
                    anchor = Anchor.BOTTOM_LEFT,
                )
            ),
            buttons = listOf(
                VirtualButtonConfig(
                    key = Key.SPACE,
                    button = GameButton.BUTTON_SOUTH,
                    anchor = Anchor.BOTTOM_RIGHT,
                ),
                VirtualButtonConfig(
                    key = Key.RETURN,
                    button = GameButton.BUTTON_NORTH,
                    anchor = Anchor.BOTTOM_RIGHT,
                    offset = Point(0f, -100f)
                ),
                VirtualButtonConfig(
                    key = Key.Z,
                    button = GameButton.BUTTON_WEST,
                    anchor = Anchor.BOTTOM_RIGHT,
                    offset = Point(0f, -200f)
                )
            )
        )
    }

    fun setupButtonActions(onAnyButton: () -> Unit, onWestButton: () -> Unit, onSouthButton: () -> Unit, onNorthButton: () -> Unit) {
        virtualController.apply {
            down(GameButton.BUTTON_WEST) { onAnyButton(); onWestButton() }
            down(GameButton.BUTTON_SOUTH) { onAnyButton(); onSouthButton() }
            down(GameButton.BUTTON_NORTH) { onAnyButton(); onNorthButton() }
        }
    }

    fun getControllerInput(): Pair<Double, Double> {
        return Pair(virtualController.lx.toDouble(), virtualController.ly.toDouble())
    }
}
