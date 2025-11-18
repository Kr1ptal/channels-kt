package io.channels.core.blocking

/**
 * Platform-specific lock with integrated condition variable support.
 *
 * On JVM, this wraps `java.util.concurrent.locks.ReentrantLock` + `Condition`.
 * On Native (iOS), this uses `NSCondition`.
 */
internal expect class PlatformLock() {
    /**
     * Acquire the lock.
     * Blocks until the lock is available.
     */
    fun lock()

    /**
     * Release the lock.
     * Must be called by the thread that currently holds the lock.
     */
    fun unlock()

    /**
     * Wait on the condition variable.
     *
     * MUST be called with the lock held.
     * Atomically releases the lock and blocks until [signalAll] is called.
     * Upon return, the lock is re-acquired.
     */
    fun await()

    /**
     * Wake up all threads waiting on this lock's condition.
     *
     * Should typically be called with the lock held to ensure proper
     * memory visibility, though not strictly required on all platforms.
     */
    fun signalAll()
}

/**
 * Execute [block] with the lock held.
 * Ensures the lock is properly released even if [block] throws an exception.
 */
internal inline fun <T> PlatformLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
