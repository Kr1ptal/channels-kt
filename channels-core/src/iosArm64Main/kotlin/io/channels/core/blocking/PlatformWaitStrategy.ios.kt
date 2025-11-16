package io.channels.core.blocking

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.nanosleep
import platform.posix.sched_yield
import platform.posix.timespec

/**
 * iOS implementation of platform-specific wait strategies using Darwin/POSIX APIs.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual object PlatformWaitStrategy {
    /**
     * Hint to the platform that the current thread is in a spin-wait loop.
     * On iOS, we use sched_yield() as a hint to the scheduler.
     * Note: iOS doesn't have a direct equivalent to Java's Thread.onSpinWait(),
     * but sched_yield() serves a similar purpose.
     */
    actual fun onSpinWait() {
        sched_yield()
    }

    /**
     * Yield the current thread to allow other threads to execute.
     * Uses POSIX sched_yield() to yield the CPU to other threads.
     */
    actual fun yieldThread() {
        sched_yield()
    }

    /**
     * Sleep for the specified duration in nanoseconds.
     * Uses POSIX nanosleep() for precise nanosecond-level sleeping.
     *
     * @param nanos Duration to sleep in nanoseconds
     */
    actual fun sleepNanos(nanos: Long) {
        memScoped {
            val timespec = alloc<timespec>()
            timespec.tv_sec = nanos / 1_000_000_000
            timespec.tv_nsec = nanos % 1_000_000_000
            nanosleep(timespec.ptr, null)
        }
    }
}
