package utils

import kotlinx.coroutines.*
import kotlin.math.pow

class RetryPolicy(
    private val maxAttempts: Int,
    private val initialTimeout: Int,
    private val maxTimeout: Int
) {
    suspend fun <T> execute(block: suspend () -> T): T {
        var currentTimeout = initialTimeout
        repeat(maxAttempts - 1) { attempt ->
            try {
                return withTimeout(currentTimeout * 1000L) { block() }
            } catch (e: Exception) {
                println("Attempt ${attempt + 1} failed: ${e.message}")
                if (attempt == maxAttempts - 2) throw e
                delay(calculateBackoff(attempt))
                currentTimeout = (currentTimeout * 2).coerceAtMost(maxTimeout)
            }
        }
        return withTimeout(currentTimeout * 1000L) { block() } // last attempt
    }

    private fun calculateBackoff(attempt: Int): Long =
        (2.0.pow(attempt.toDouble()) * 1000).toLong().coerceAtMost(30000)
}
