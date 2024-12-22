package ai

enum class ActionVerb {
    MOVE,
    SHOOT,
    GIVE,
    TAKE,
    INTERACT;

    companion object {
        fun fromString(value: String): ActionVerb? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
