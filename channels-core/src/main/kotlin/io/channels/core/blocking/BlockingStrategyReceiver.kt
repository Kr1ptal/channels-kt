package io.channels.core.blocking

import io.channels.core.ChannelReceiver
import java.util.function.Consumer
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Wrapper that applies a specific blocking strategy to a [ChannelReceiver].
 * Each wrapper is immutable and creates new instances when strategy changes.
 */
class BlockingStrategyReceiver<T : Any>(
    private val delegate: ChannelReceiver<T>,
    private val waitFunction: (NotificationHandle) -> Unit,
) : ChannelReceiver<T> {
    // Delegate all non-blocking operations
    override val notificationHandle: NotificationHandle get() = delegate.notificationHandle
    override val isClosed: Boolean get() = delegate.isClosed
    override val size: Int get() = delegate.size
    override fun poll(): T? = delegate.poll()
    override fun close() = delegate.close()

    override fun forEach(consumer: Consumer<in T>) {
        while (true) {
            consumer.accept(take() ?: break)
        }
    }

    // Blocking operation uses this wrapper's wait strategy
    override fun take(): T? {
        while (true) {
            val value = delegate.poll()
            if (value != null || delegate.isClosed) return value
            waitFunction.invoke(notificationHandle)
        }
    }

    // Strategy methods - create new wrappers with different wait functions
    override fun withBusySpinBlockingStrategy(): ChannelReceiver<T> {
        return BlockingStrategyReceiver(delegate, NotificationHandle::waitWithBusySpin)
    }

    override fun withParkingBlockingStrategy(): ChannelReceiver<T> {
        return BlockingStrategyReceiver(delegate, NotificationHandle::waitWithParking)
    }

    override fun withSleepBlockingStrategy(): ChannelReceiver<T> {
        return withSleepBlockingStrategy(100, DurationUnit.NANOSECONDS)
    }

    override fun withSleepBlockingStrategy(duration: Long, unit: DurationUnit): ChannelReceiver<T> {
        val sleepNanos = duration.toDuration(unit).inWholeNanoseconds
        return BlockingStrategyReceiver(delegate) { it.waitWithSleep(sleepNanos) }
    }

    override fun withYieldingBlockingStrategy(): ChannelReceiver<T> {
        return BlockingStrategyReceiver(delegate, NotificationHandle::waitWithYield)
    }
}
