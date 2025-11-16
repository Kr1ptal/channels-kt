package io.channels.core.blocking

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCondition

/**
 * iOS implementation of [PlatformLock] using Foundation's NSCondition.
 *
 * NSCondition is a higher-level wrapper around pthread_mutex_t and pthread_cond_t
 * that provides better error handling and Kotlin/Native integration compared to
 * raw pthread primitives.
 *
 * ## Why NSCondition instead of pthread?
 *
 * Direct pthread usage had issues with thread ownership enforcement:
 * - pthread_mutex requires the **same thread** that locks to unlock
 * - Kotlin/Native's memory model and coroutines can cause subtle thread switches
 * - NSCondition handles these edge cases more gracefully
 *
 * ## Resource Management
 *
 * NSCondition is a Foundation object managed by ARC (Automatic Reference Counting).
 * The [close] method is a no-op since ARC handles cleanup when this object is freed.
 *
 * ## Thread Safety
 *
 * This class is thread-safe and designed for concurrent access from
 * multiple threads. NSCondition ensures proper synchronization.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual class PlatformLock {
    private val condition = NSCondition()

    /**
     * Acquire the lock.
     * Blocks until the lock is available.
     */
    actual fun lock() {
        condition.lock()
    }

    /**
     * Release the lock.
     * Must be called by the thread that currently holds the lock.
     */
    actual fun unlock() {
        condition.unlock()
    }

    /**
     * Wait on the condition variable.
     *
     * MUST be called with the lock held (after calling [lock]).
     *
     * This method atomically:
     * 1. Releases the lock
     * 2. Blocks the thread
     * 3. Re-acquires the lock when signaled
     *
     * The lock will be held upon return.
     *
     * Note: Like pthread_cond_wait, this can experience spurious wakeups.
     * Callers should re-check their wait condition in a loop.
     */
    actual fun await() {
        condition.wait()
    }

    /**
     * Wake up all threads waiting on this lock's condition.
     *
     * It's recommended to hold the lock when calling this method
     * to ensure proper memory visibility, though not strictly required.
     */
    actual fun signalAll() {
        condition.broadcast()
    }
}
