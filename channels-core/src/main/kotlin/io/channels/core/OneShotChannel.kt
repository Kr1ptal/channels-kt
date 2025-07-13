package io.channels.core

import io.channels.core.blocking.NotificationHandle
import java.util.function.Consumer
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A [Channel] that can send / receive a single element. After the element is sent and received, the channel is closed
 * and cannot be used again.
 * */
@OptIn(ExperimentalAtomicApi::class)
class OneShotChannel<T : Any> @JvmOverloads constructor(value: T? = null) : Channel<T> {
    private val state = AtomicReference<Any?>(value)

    override val notificationHandle = NotificationHandle(this)

    override fun offer(element: T): Boolean {
        if (state.compareAndSet(null, element)) {
            notificationHandle.signalStateChange()
            return true
        }
        return false
    }

    override fun close() {
        if (state.exchange(CONSUMED) !== CONSUMED) {
            notificationHandle.signalStateChange()
        }
    }

    override fun take(): T? {
        while (true) {
            val ret = poll()
            if (ret != null) {
                return ret
            }

            // check after polling, so we still drain the queue even if unsubscribed
            if (isClosed) {
                break
            }

            // if no next element, wait until next event is available
            notificationHandle.waitWithParking()
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun poll(): T? {
        val ret = state.getAndUpdate { if (it == null) null else CONSUMED }
        return if (ret == null || ret === CONSUMED) null else ret as T
    }

    /**
     * Atomically updates the current value with the results of applying the given [update] function,
     * returning the previous value.
     */
    private fun <T> AtomicReference<T?>.getAndUpdate(update: (T?) -> T?): T? {
        var prev: T? = this.load()
        var next: T? = null
        var haveNext = false
        while (true) {
            if (!haveNext) {
                next = update(prev)
            }

            if (this.compareAndSet(prev, next)) {
                return prev
            }

            val stored = this.load()
            haveNext = prev == stored
            prev = stored
        }
    }

    override val isClosed: Boolean
        get() = state.load() === CONSUMED

    override val size: Int
        get() {
            val ret = state.load()
            return if (ret == null || ret === CONSUMED) 0 else 1
        }

    override fun forEach(consumer: Consumer<in T>) {
        consumer.accept(take() ?: return)
    }

    companion object {
        private val CONSUMED = Any()
    }
}
