package io.channels.core.blocking

import io.channels.core.ChannelState
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Coordinates a suspended receiver, state-change callbacks, and JVM blocking notifications. */
class NotificationHandle(channelState: ChannelState) : PlatformNotification(channelState) {
    private val suspendedReceiver = atomic<CancellableContinuation<Unit>?>(null)

    // Callback support for custom strategies - we use atomic reference with an immutable list because
    // there will be many more reads than writes in a normal scenario.
    private val callbacks: AtomicRef<List<() -> Unit>> = atomic(emptyList())

    /**
     * Signal that channel state has changed - called by sender. Optimized for minimal overhead
     * in the common case.
     */
    fun signalStateChange() {
        signalPlatformStateChange()

        // Avoid an atomic read-modify-write on the producer fast path when no coroutine is waiting.
        if (suspendedReceiver.value != null) {
            suspendedReceiver.getAndSet(null)?.resume(Unit)
        }

        // Notify any explicitly registered callbacks.
        for (callback in callbacks.value) {
            callback.invoke()
        }
    }

    /**
     * Wait directly on the single receiver's continuation. Register before rechecking state so an offer or close
     * between the caller's empty poll and registration cannot be lost. Only the receiver polls for elements;
     * resuming with Unit keeps values in the channel if cancellation wins before the receiver is dispatched.
     */
    suspend fun awaitStateChange() {
        var waiter: CancellableContinuation<Unit>? = null
        try {
            suspendCancellableCoroutine { continuation ->
                waiter = continuation
                check(suspendedReceiver.compareAndSet(null, continuation)) {
                    "Only one suspended receiver is supported per notification handle"
                }

                if (!channelState.isEmpty || channelState.isClosed) {
                    // Race with signalStateChange(): only the thread that removes this waiter may resume it.
                    if (suspendedReceiver.compareAndSet(continuation, null)) {
                        continuation.resume(Unit)
                    }
                }
            }
        } finally {
            // Runs on cancellation too, without allocating an invokeOnCancellation callback. Identity matters:
            // an already-signalled continuation must never clear another receiver's later registration.
            waiter?.let { suspendedReceiver.compareAndSet(it, null) }
        }
    }

    /**
     * Register a callback to be invoked when channel state changes. Returns a handle that can be
     * used to unregister the callback.
     */
    fun onStateChangeCallback(callback: () -> Unit): CallbackHandle {
        callbacks.update { it + callback }
        return CallbackHandle { callbacks.update { list -> list - callback } }
    }

    /**
     * Handle for managing callback registration/deregistration.
     */
    fun interface CallbackHandle {
        /**
         * Unregister the callback.
         */
        fun unregister()
    }
}
