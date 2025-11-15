package io.channels.core.blocking

/**
 * JVM implementation of platform-specific wait strategies.
 */
internal actual object PlatformWaitStrategy {
    /**
     * Hint to the JVM that the current thread is in a spin-wait loop.
     * Uses Thread.onSpinWait() if available (Java 9+), otherwise does nothing.
     */
    actual fun onSpinWait() {
        ThreadHints.onSpinWait()
    }

    /**
     * Yield the current thread to allow other threads to execute.
     */
    actual fun yieldThread() {
        Thread.yield()
    }

    /**
     * Sleep for the specified duration in nanoseconds.
     */
    actual fun sleepNanos(nanos: Long) {
        try {
            Thread.sleep(nanos / 1_000_000, (nanos % 1_000_000).toInt())
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
