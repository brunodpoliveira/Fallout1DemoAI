package agent.system

import korlibs.math.geom.*

sealed class NPCTask {
    data class Patrol(
        val points: List<Point>,
        val startTime: Long = System.currentTimeMillis()
    ) : NPCTask() {
        fun isComplete(): Boolean = false // TODO: Implement actual patrol completion check
    }

    data class Dialog(
        val partnerId: String,
        val startTime: Long = System.currentTimeMillis()
    ) : NPCTask() {
        fun isTimedOut(): Boolean = System.currentTimeMillis() - startTime > DIALOG_TIMEOUT
    }

    companion object {
        const val DIALOG_TIMEOUT = 30000L // 30 seconds
        const val AUTONOMOUS_ACTION_DELAY = 5L //5 seconds
    }
}
