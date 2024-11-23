package ai

object NPCBio {
    fun getBioForNPC(npcName: String): String {
        return Director.getNPCBio(npcName)
    }
    fun getAllNPCNames(): List<String> {
        return Director.getAllNPCNames()
    }
}
