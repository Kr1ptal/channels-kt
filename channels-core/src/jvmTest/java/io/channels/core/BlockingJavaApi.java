package io.channels.core;

import io.channels.core.blocking.NotificationHandle;
import kotlin.Unit;
import kotlin.time.DurationUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Compiled by javac: catches accidental replacement of members with Kotlin extensions. */
public final class BlockingJavaApi {
    private BlockingJavaApi() {}

    public static void verify() {
        List<Function<ChannelReceiver<String>, ChannelReceiver<String>>> strategies = List.of(
            ChannelReceiver::withBusySpinBlockingStrategy,
            ChannelReceiver::withParkingBlockingStrategy,
            ChannelReceiver::withYieldingBlockingStrategy,
            ChannelReceiver::withSleepBlockingStrategy,
            receiver -> receiver.withSleepBlockingStrategy(100, DurationUnit.NANOSECONDS),
            receiver -> receiver.withBlockingStrategy(handle -> {
                handle.waitWithParking();
                return Unit.INSTANCE;
            })
        );
        for (Function<ChannelReceiver<String>, ChannelReceiver<String>> strategy : strategies) {
            ChannelReceiver<String> source = new OneShotChannel<>("hello");
            ChannelReceiver<String> receiver = strategy.apply(source);
            if (receiver.getNotificationHandle() != source.getNotificationHandle()) {
                throw new AssertionError("Strategies must share the source notification handle");
            }
            if (!"hello".equals(receiver.take()) || receiver.take() != null) {
                throw new AssertionError("Blocking members must consume and observe close");
            }
            // All waits must immediately return on a closed, empty channel.
            NotificationHandle handle = receiver.getNotificationHandle();
            handle.waitWithBusySpin();
            handle.waitWithYield();
            handle.waitWithParking();
            handle.waitWithSleep(100);
        }
        if (!"direct".equals(new OneShotChannel<>("direct").take())) {
            throw new AssertionError("Concrete channels must inherit blocking members");
        }
        ChannelReceiver<String> mapped = new OneShotChannel<>("hello").map(String::toUpperCase);
        List<String> values = new ArrayList<>();
        mapped.forEach(values::add);
        if (!values.equals(List.of("HELLO"))) {
            throw new AssertionError("Operator receivers must inherit blocking members");
        }
    }

    public static void verifyAsync() throws InterruptedException {
        for (int overload = 0; overload < 3; overload++) {
            OneShotChannel<String> source = new OneShotChannel<>();
            ChannelReceiver<String> receiver = source.withBlockingStrategy(handle -> {
                source.offer("hello");
                return Unit.INSTANCE;
            });
            CountDownLatch consumed = new CountDownLatch(1);
            AtomicReference<String> value = new AtomicReference<>();
            AtomicReference<Thread> worker = new AtomicReference<>();
            AtomicReference<Thread> factoryThread = new AtomicReference<>();
            ChannelConsumer<String> consumer = element -> {
                value.set(element);
                worker.set(Thread.currentThread());
                consumed.countDown();
            };

            try {
                ChannelReceiver<String> returned;
                if (overload == 0) {
                    returned = receiver.forEachAsync(consumer);
                } else if (overload == 1) {
                    returned = receiver.forEachAsync("named-receiver", consumer);
                } else {
                    returned = receiver.forEachAsync(runnable -> {
                        Thread thread = new Thread(runnable, "factory-receiver");
                        thread.setDaemon(true);
                        factoryThread.set(thread);
                        return thread;
                    }, consumer);
                }
                if (returned != receiver) {
                    throw new AssertionError("forEachAsync must return the original receiver");
                }
                if (!consumed.await(5, TimeUnit.SECONDS) || !"hello".equals(value.get())) {
                    throw new AssertionError("forEachAsync must consume using the selected blocking strategy");
                }
                Thread thread = worker.get();
                if (thread == Thread.currentThread() || !thread.isDaemon()) {
                    throw new AssertionError("Expected a background daemon thread");
                }
                if (overload == 1 && !"named-receiver".equals(thread.getName())) {
                    throw new AssertionError("The requested thread name must be used");
                }
                if (overload == 2 && thread != factoryThread.get()) {
                    throw new AssertionError("The supplied thread factory must be used");
                }
            } finally {
                source.close();
                Thread thread = worker.get();
                if (thread != null) thread.join(5_000);
            }
        }
    }

}
