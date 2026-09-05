package io.channels.core.blocking

/**
 * JVM and Android implementation of platform-specific wait strategies.
 */
internal object PlatformWaitStrategy {
    /**
     * Hint to the JVM that the current thread is in a spin-wait loop.
     * Uses Thread.onSpinWait() if available (Java 9+), otherwise does nothing.
     */
    fun onSpinWait() {
        ThreadHints.onSpinWait()
    }

    /**
     * Yield the current thread to allow other threads to execute.
     */
    fun yieldThread() {
        Thread.yield()
    }

    /**
     * Sleep for the specified duration in nanoseconds.
     */
    fun sleepNanos(nanos: Long) {
        try {
            Thread.sleep(nanos / 1_000_000, (nanos % 1_000_000).toInt())
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
