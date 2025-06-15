package io.channels.core.blocking

import io.channels.core.ChannelReceiver
import java.time.Duration
import java.util.concurrent.ThreadFactory
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * Wrapper that applies a specific blocking strategy to a [ChannelReceiver].
 * Each wrapper is immutable and creates new instances when strategy changes.
 */
class BlockingStrategyReceiver<T : Any>(
    private val delegate: ChannelReceiver<T>,
    private val waitFunction: (NotificationHandle) -> Unit,
    private val notificationHandle: NotificationHandle = NotificationHandle(delegate),
) : ChannelReceiver<T> {

    // Delegate all non-blocking operations
    override val isClosed: Boolean get() = delegate.isClosed
    override val size: Int get() = delegate.size
    override fun poll(): T? = delegate.poll()
    override fun close() = delegate.close()
    override fun forEach(consumer: Consumer<in T>) = delegate.forEach(consumer)
    override fun forEachAsync(threadName: String?, consumer: Consumer<in T>): ChannelReceiver<T> = delegate.forEachAsync(threadName, consumer)
    override fun forEachAsync(threadFactory: ThreadFactory, consumer: Consumer<in T>): ChannelReceiver<T> = delegate.forEachAsync(threadFactory, consumer)

    // Blocking operation uses this wrapper's strategy
    override fun take(): T? {
        while (true) {
            val value = delegate.poll()
            if (value != null || delegate.isClosed) return value
            waitFunction(notificationHandle)
        }
    }

    // Strategy methods - create new wrappers with different wait functions
    override fun withBusySpinBlockingStrategy(): ChannelReceiver<T> {
        return BlockingStrategyReceiver(delegate, NotificationHandle::waitWithBusySpin, notificationHandle)
    }

    override fun withParkingBlockingStrategy(): ChannelReceiver<T> {
        return BlockingStrategyReceiver(delegate, NotificationHandle::waitWithParking, notificationHandle)
    }

    override fun withSleepBlockingStrategy(): ChannelReceiver<T> {
        return withSleepBlockingStrategy(Duration.ofNanos(100))
    }

    override fun withSleepBlockingStrategy(duration: Duration): ChannelReceiver<T> {
        val sleepNanos = duration.toNanos()
        return BlockingStrategyReceiver(delegate, { it.waitWithSleep(sleepNanos) }, notificationHandle)
    }

    override fun withYieldingBlockingStrategy(): ChannelReceiver<T> {
        return BlockingStrategyReceiver(delegate, NotificationHandle::waitWithYield, notificationHandle)
    }

    // Operator methods delegate to wrapped instances
    override fun <R : Any> map(mapper: Function<in T, R>): ChannelReceiver<R> = delegate.map(mapper)
    override fun <R : Any> mapNotNull(mapper: Function<in T, R?>): ChannelReceiver<R> = delegate.mapNotNull(mapper)
    override fun filter(predicate: Predicate<in T>): ChannelReceiver<T> = delegate.filter(predicate)
}
