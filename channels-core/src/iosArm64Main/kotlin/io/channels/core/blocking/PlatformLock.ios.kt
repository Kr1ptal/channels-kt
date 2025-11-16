package io.channels.core.blocking

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_cond_broadcast
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock

/**
 * iOS implementation of [PlatformLock] using pthread primitives.
 *
 * This implementation properly pairs `pthread_mutex_t` and `pthread_cond_t`,
 * which is required for `pthread_cond_wait()` to function correctly.
 * The condition wait atomically unlocks the mutex, blocks the thread,
 * and re-locks the mutex upon waking.
 *
 * ## Resource Management
 *
 * The mutex and condition variable are allocated on the native heap
 * and live for the lifetime of this object. Since Kotlin/Native lacks
 * finalizers, cleanup is not automatic but rather done in [close] function.
 *
 * For typical usage in [NotificationHandle], the lifetime matches the
 * Channel's lifetime (long-lived), so the small resource leak is acceptable.
 *
 * ## Thread Safety
 *
 * This class is thread-safe and designed for concurrent access from
 * multiple threads. The pthread mutex ensures proper synchronization.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual class PlatformLock : AutoCloseable {
    // Allocate mutex and condition on native heap for long-lived usage
    private val mutex = nativeHeap.alloc<pthread_mutex_t>()
    private val cond = nativeHeap.alloc<pthread_cond_t>()

    init {
        // Initialize mutex with default attributes (non-recursive)
        val mutexResult = pthread_mutex_init(mutex.ptr, null)
        if (mutexResult != 0) {
            // Cleanup partial allocation and fail fast
            nativeHeap.free(mutex.rawPtr)
            nativeHeap.free(cond.rawPtr)
            throw RuntimeException("Failed to initialize pthread_mutex: error code $mutexResult")
        }

        // Initialize a condition variable with default attributes
        val condResult = pthread_cond_init(cond.ptr, null)
        if (condResult != 0) {
            // Cleanup mutex and fail fast
            pthread_mutex_destroy(mutex.ptr)
            nativeHeap.free(mutex.rawPtr)
            nativeHeap.free(cond.rawPtr)
            throw RuntimeException("Failed to initialize pthread_cond: error code $condResult")
        }
    }

    /**
     * Acquire the lock.
     * Blocks until the lock is available.
     */
    actual fun lock() {
        check(pthread_mutex_lock(mutex.ptr), "pthread_mutex_lock")
    }

    /**
     * Release the lock.
     * Must be called by the thread that currently holds the lock.
     */
    actual fun unlock() {
        check(pthread_mutex_unlock(mutex.ptr), "pthread_mutex_unlock")
    }

    /**
     * Wait on the condition variable.
     *
     * MUST be called with the lock held (after calling [lock]).
     *
     * This method atomically:
     * 1. Releases the mutex
     * 2. Blocks the thread
     * 3. Re-acquires the mutex when signaled
     *
     * The mutex will be held upon return.
     *
     * Note: pthread_cond_wait can experience spurious wakeups.
     * Callers should re-check their wait condition in a loop.
     */
    actual fun await() {
        check(pthread_cond_wait(cond.ptr, mutex.ptr), "pthread_cond_wait")
    }

    /**
     * Wake up all threads waiting on this lock's condition.
     *
     * It's recommended to hold the lock when calling this method
     * to ensure proper memory visibility, though not strictly required.
     */
    actual fun signalAll() {
        check(pthread_cond_broadcast(cond.ptr), "pthread_cond_broadcast")
    }

    /**
     * Destroy the mutex and condition variable and free native memory.
     *
     * IMPORTANT: Do NOT call this if any threads might still be waiting
     * on the condition or holding the lock. Undefined behavior will result.
     */
    actual override fun close() {
        pthread_cond_destroy(cond.ptr)
        pthread_mutex_destroy(mutex.ptr)
        nativeHeap.free(cond.rawPtr)
        nativeHeap.free(mutex.rawPtr)
    }

    private fun check(code: Int, what: String) {
        if (code != 0) {
            throw IllegalStateException("$what failed: errno=$code")
        }
    }
}
