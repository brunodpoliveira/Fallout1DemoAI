package ai

object NPCBio {
    fun getBioForNPC(npcName: String): String {
        return Director.getNPCBio(npcName)
    }
}
