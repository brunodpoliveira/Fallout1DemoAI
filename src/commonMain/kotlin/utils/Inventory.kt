package utils

class Inventory {
    private val items = mutableListOf<String>()

    fun addItem(item: String) {
        items.add(item)
    }

    fun getItems(): List<String> {
        return items
    }

    fun consumePotion(potion: String, playerStats: EntityStats, updateHealthUI: (Int) -> Unit) {
        if (potion == "Red_potion") {
            playerStats.hp += 10
            println("Consumed potion: increase hp by 10")
            updateHealthUI(playerStats.hp)
        }
    }

    fun removeItem(item: String) {
        items.remove(item)
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
        println("Added $amount ammo. Total ammo: ${playerStats.ammo}")
    }
}
