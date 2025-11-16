package io.channels.core.blocking

/**
 * Platform-specific lock with integrated condition variable support.
 *
 * This abstraction combines mutex and condition functionality in a single class,
 * which is necessary for proper pthread usage on native platforms where
 * `pthread_cond_wait()` requires the mutex to be passed explicitly.
 *
 * On JVM, this wraps `java.util.concurrent.locks.ReentrantLock` + `Condition`.
 * On Native (iOS), this uses `pthread_mutex_t` + `pthread_cond_t`.
 */
internal expect class PlatformLock() : AutoCloseable {
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

    override fun close()
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
