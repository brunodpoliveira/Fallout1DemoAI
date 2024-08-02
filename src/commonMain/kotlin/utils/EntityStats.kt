package utils

import korlibs.korge.ldtk.view.LDTKEntityView

class EntityStats(
    var hp: Int,
    var ammo: Int)

fun readEntityStats(entityView: LDTKEntityView): EntityStats {
    val identifier = entityView.entity.identifier
    if (identifier !in listOf("Player", "Enemy")) {
        return EntityStats(0,0)
    }

    val hpEntity = entityView.fieldsByName.values.firstOrNull { it.identifier == "HP" }
    val ammoEntity = entityView.fieldsByName.values.firstOrNull { it.identifier == "Ammo" }

    val hpField: Int? = when (val hpValue = hpEntity?.value) {
        is Int -> hpValue
        is Double -> hpValue.toInt()
        is Float -> hpValue.toInt()
        is String -> hpValue.toIntOrNull()
        else -> null
    }
    val ammoField: Int? = when (val ammoValue = ammoEntity?.value) {
        is Int -> ammoValue
        is Double -> ammoValue.toInt()
        is Float -> ammoValue.toInt()
        is String -> ammoValue.toIntOrNull()
        else -> null
    }

    return EntityStats(hpField ?: 0, ammoField ?: 0)
}
