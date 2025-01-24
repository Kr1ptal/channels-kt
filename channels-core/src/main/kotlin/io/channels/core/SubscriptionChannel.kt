package io.channels.core

import java.io.Closeable
import java.util.function.Function
import java.util.function.Predicate

/**
 * A channel that can be subscribed to.
 * */
interface SubscriptionChannel<T : Any> : Closeable {
    /**
     * Subscribe to this channel, returning a [ChannelReceiver]. When the [SubscriptionChannel] is closed, the
     * receiver will be closed as well. If the [ChannelReceiver] is closed, it closes only itself.
     * */
    fun subscribe(): ChannelReceiver<T>

    /**
     * Map each element from this channel using [mapper], from type [T] to [R].
     * */
    fun <R : Any> map(mapper: Function<in T, R>): SubscriptionChannel<R> {
        return SubscriptionChannelOperator(this) { it.map(mapper) }
    }

    /**
     * Map each element from this channel using [mapper], from type [T] to [R]. If [mapper] returns null, the element
     * is skipped.
     * */
    fun <R : Any> mapNotNull(mapper: Function<in T, R?>): SubscriptionChannel<R> {
        return SubscriptionChannelOperator(this) { it.mapNotNull(mapper) }
    }

    /**
     * Filter elements from this channel using [predicate].
     * */
    fun filter(predicate: Predicate<in T>): SubscriptionChannel<T> {
        return SubscriptionChannelOperator(this) { it.filter(predicate) }
    }
}

/**
 * A [SubscriptionChannel] that re-maps each call to [subscribe] using [mapper].
 * */
private class SubscriptionChannelOperator<T : Any, R : Any>(
    private val parent: SubscriptionChannel<T>,
    private val mapper: Function<in ChannelReceiver<T>, ChannelReceiver<R>>,
) : SubscriptionChannel<R> {
    override fun subscribe(): ChannelReceiver<R> {
        return mapper.apply(parent.subscribe())
    }

    override fun close() {
        parent.close()
    }
}