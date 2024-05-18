package ui

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*

class DialogWindow : Container() {
    init {
        val dialogBackground = solidRect(400, 200) {
            color = Colors.DARKGREY
        }

        val closeButton = uiButton {
            text = "Close"
            position(350, 150)
        }

        closeButton.onClick {
            this.removeFromParent()
        }
    }

    fun show(container: Container) {
        this.addTo(container)
        this.centerOnStage()
    }
}
