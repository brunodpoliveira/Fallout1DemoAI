package ui

import korlibs.korge.view.*
import korlibs.image.color.Colors
import korlibs.image.font.*
import korlibs.korge.view.align.*

class PlayerStatsUI(
    override val stage: Stage,
    defaultFont: Font
) : Container() {

    private val hpText = Text("", font = defaultFont, textSize = 32.0, color = Colors.RED).apply {
        stage?.let { alignRightToRightOf(it, 10.0) }
        stage?.let { alignTopToTopOf(it, 10.0) }
    }

    private val ammoText = Text("", font = defaultFont, textSize = 32.0, color = Colors.WHITE).apply {
        stage?.let { alignRightToRightOf(it, 10.0) }
        alignTopToBottomOf(hpText, 10.0)
    }

    init {
        addChild(hpText)
        addChild(ammoText)
    }

    fun update(playerHp: Int, playerAmmo: Int) {
        hpText.text = "HP: $playerHp"
        hpText.alignRightToRightOf(stage, 10.0)
        hpText.alignTopToTopOf(stage, 10.0)

        ammoText.text = "Ammo: $playerAmmo"
        ammoText.alignRightToRightOf(stage, 10.0)
        ammoText.alignTopToBottomOf(hpText, 10.0)
    }
}
