package io.channels.core.blocking

/**
 * Platform-specific wait strategies for blocking operations.
 * Each platform provides optimal implementations for these primitives.
 */
expect object PlatformWaitStrategy {
    /**
     * Hint to the platform that the current thread is in a spin-wait loop.
     * On JVM, this may use Thread.onSpinWait() for better CPU utilization.
     */
    fun onSpinWait()

    /**
     * Yield the current thread to allow other threads to execute.
     * On JVM, this calls Thread.yield().
     */
    fun yieldThread()

    /**
     * Sleep for the specified duration in nanoseconds.
     *
     * @param nanos Duration to sleep in nanoseconds
     */
    fun sleepNanos(nanos: Long)
}
