package io.channels.core

/**
 * A channel that can be subscribed to.
 * */
interface SubscriptionChannel<T : Any> : AutoCloseable {
    /**
     * Subscribe to this channel, returning a [ChannelReceiver]. When the [SubscriptionChannel] is closed, the
     * receiver will be closed as well. If the [ChannelReceiver] is closed, it closes only itself.
     * */
    fun subscribe(): ChannelReceiver<T>

    /**
     * Map each element from this channel using [mapper], from type [T] to [R].
     * */
    fun <R : Any> map(mapper: ChannelFunction<in T, R>): SubscriptionChannel<R> {
        return SubscriptionChannelOperator(this) { it.map(mapper) }
    }

    /**
     * Map each element from this channel using [mapper], from type [T] to [R]. If [mapper] returns null, the element
     * is skipped.
     * */
    fun <R : Any> mapNotNull(mapper: ChannelFunction<in T, R?>): SubscriptionChannel<R> {
        return SubscriptionChannelOperator(this) { it.mapNotNull(mapper) }
    }

    /**
     * Filter elements from this channel using [predicate].
     * */
    fun filter(predicate: ChannelPredicate<in T>): SubscriptionChannel<T> {
        return SubscriptionChannelOperator(this) { it.filter(predicate) }
    }
}

/**
 * A [SubscriptionChannel] that re-maps each call to [subscribe] using [mapper].
 * */
private class SubscriptionChannelOperator<T : Any, R : Any>(
    private val parent: SubscriptionChannel<T>,
    private val mapper: ChannelFunction<in ChannelReceiver<T>, ChannelReceiver<R>>,
) : SubscriptionChannel<R> {
    override fun subscribe(): ChannelReceiver<R> {
        return mapper.apply(parent.subscribe())
    }

    override fun close() {
        parent.close()
    }
}
