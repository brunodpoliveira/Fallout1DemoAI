import korlibs.korge.ldtk.view.LDTKEntityView

class EntityStats(var hp: Int)

fun readEntityStats(entityView: LDTKEntityView): EntityStats {
    val identifier = entityView.entity.identifier
    if (identifier !in listOf("Player", "Enemy")) {
        return EntityStats(0)
    }

    val hpEntity = entityView.fieldsByName.values.firstOrNull { it.identifier == "HP" }

    val hpField: Int? = when (val hpValue = hpEntity?.value) {
        is Int -> hpValue
        is Double -> hpValue.toInt()
        is Float -> hpValue.toInt()
        is String -> hpValue.toIntOrNull()
        else -> null
    }

    return EntityStats(hpField ?: 0)
}
