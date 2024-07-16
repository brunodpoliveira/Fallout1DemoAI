
class Inventory {
    private val items = mutableListOf<String>()

    fun addItem(item: String) {
        items.add(item)
    }

    fun getItems(): List<String> {
        return items
    }

    fun consumePotion(potion: String, playerStats: EntityStats, updateHealthUI: (Int) -> Unit) {
        if (potion == "red_potion") {
            playerStats.hp += 10
            println("Consumed potion: increase hp by 10")
            updateHealthUI(playerStats.hp)
        }
    }

    fun removeItem(item: String) {
        items.remove(item)
    }
}
