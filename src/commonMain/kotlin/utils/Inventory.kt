package utils

class Inventory(val owner: String) {
    private val items = mutableListOf<String>()

    fun hasItem(item: String): Boolean = item in items

    fun getItems(): List<String> = items.toList()

    fun addItem(item: String) {
        items.add(item)
        Logger.debug("$owner added $item to their inventory")
    }

    fun consumePotion(potion: String, playerStats: EntityStats, updateHealthUI: (Int) -> Unit) {
        if (potion == "Red_potion") {
            playerStats.hp += 10
            Logger.debug("Consumed potion: increase hp by 10")
            updateHealthUI(playerStats.hp)
        }
    }

    fun removeItem(item: String): Boolean {
        val removed = items.remove(item)
        if (removed) {
            Logger.debug("$owner removed $item from their inventory")
        }
        return removed
    }

    fun useAmmo(playerStats: EntityStats, updateAmmoUI: (Int) -> Unit): Boolean {
        if (playerStats.ammo > 0) {
            playerStats.ammo -= 1
            updateAmmoUI(playerStats.ammo)
            return true
        }
        return false
    }

    fun addAmmo(amount: Int, playerStats: EntityStats, updateAmmoUI: (Int) -> Unit) {
        playerStats.ammo += amount
        updateAmmoUI(playerStats.ammo)
        Logger.debug("Added $amount ammo. Total ammo: ${playerStats.ammo}")
    }
}
