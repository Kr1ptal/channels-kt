package io.channels.core

import io.channels.core.waiting.WaitStrategy
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/**
 * A [Channel] that can send / receive a single element. After the element is sent and received, the channel is closed
 * and cannot be used again.
 * */
class OneShotChannel<T : Any> : Channel<T> {
    private val state = AtomicReference<Any>(null)
    private val notifier = ChangeNotifier()

    override fun onStateChange(listener: Runnable) {
        notifier.register(listener)
    }

    override fun offer(element: T): Boolean {
        if (state.compareAndSet(null, element)) {
            notifier.notifyChange()
            return true
        }
        return false
    }

    override fun close() {
        if (state.getAndSet(CLOSED) !== CLOSED) {
            notifier.notifyChange()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun tryPoll(): T? {
        val ret = state.getAndUpdate { if (it == null) null else CLOSED }
        return if (ret == null || ret === CLOSED) null else ret as T
    }

    override val isClosed: Boolean
        get() = state.get() === CLOSED

    override val size: Int
        get() {
            val ret = state.get()
            return if (ret == null || ret === CLOSED) 0 else 1
        }

    override fun forEach(waitStrategy: WaitStrategy, consumer: Consumer<in T>) {
        onStateChange { waitStrategy.signalStateChange() }

        while (true) {
            val next = tryPoll()
            if (next != null) {
                consumer.accept(next)
                break
            }

            // check after polling, so we still drain the queue even if unsubscribed
            if (isClosed) {
                break
            }

            // if no next element, wait until next event is available
            waitStrategy.waitForNextElement(this)
        }
    }

    companion object {
        private val CLOSED = Any()
    }
}
