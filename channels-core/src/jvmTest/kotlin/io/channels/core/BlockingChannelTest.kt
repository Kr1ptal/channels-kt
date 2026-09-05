package io.channels.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.DurationUnit

class BlockingChannelTest : FunSpec({
    test("blocking strategies and handle waits are Java-callable members") {
        BlockingJavaApi.verify()
    }

    test("forEachAsync overloads are Java-callable members and preserve receiver and thread behavior") {
        BlockingJavaApi.verifyAsync()
    }

    test("a strategy is retained when the wrapper is typed as ChannelReceiver") {
        val channel = OneShotChannel<String>()
        var waits = 0
        val receiver: ChannelReceiver<String> = channel.withBlockingStrategy {
            waits++
            channel.offer("hello")
        }

        val elements = mutableListOf<String>()
        receiver.forEach { elements.add(it) }

        elements shouldBe listOf("hello")
        waits shouldBe 1
    }

    val operators = listOf<Pair<String, (ChannelReceiver<String>) -> ChannelReceiver<String>>>(
        "map" to { it.map { value -> value.uppercase() } },
        "filter" to { it.filter { value -> value != "skip" } },
        "mapNotNull" to { it.mapNotNull { value -> value.takeUnless { it == "skip" } } },
        "chain" to { it.map { value -> value.uppercase() }.filter { value -> value != "SKIP" }.mapNotNull { it } },
    )
    operators.forEach { (name, wrap) ->
        listOf("take", "forEach", "forEachAsync").forEach { terminal ->
            test("$name preserves the parent blocking strategy for $terminal") {
                val channel = QueueChannel.spscUnbounded<String>()
                var waits = 0
                val receiver = wrap(
                    channel.withBlockingStrategy {
                        when (++waits) {
                            1 -> channel.offer("skip")
                            2 -> channel.offer("keep")
                            else -> channel.close()
                        }
                    },
                )
                val elements = mutableListOf<String>()
                val finished = CompletableFuture<Unit>()
                val factory = ThreadFactory { task ->
                    thread(start = false, isDaemon = true) {
                        try {
                            task.run()
                            finished.complete(Unit)
                        } catch (failure: Throwable) {
                            finished.completeExceptionally(failure)
                        }
                    }
                }
                try {
                    if (terminal == "forEachAsync") {
                        receiver.forEachAsync(factory) { elements.add(it) } shouldBe receiver
                    } else {
                        factory.newThread {
                            if (terminal == "take") {
                                while (true) elements.add(receiver.take() ?: break)
                            } else {
                                receiver.forEach { elements.add(it) }
                            }
                        }.start()
                    }
                    finished.get(5, TimeUnit.SECONDS)
                    elements shouldBe when (name) {
                        "map" -> listOf("SKIP", "KEEP")
                        "chain" -> listOf("KEEP")
                        else -> listOf("keep")
                    }
                    waits shouldBe 3
                } finally {
                    channel.close()
                }
            }
        }
    }

    test("take blocks until an element is available") {
        val channel = QueueChannel.mpscBounded<String>(2)
        val producer = thread {
            Thread.sleep(50)
            channel.offer("hello")
        }

        channel.take() shouldBe "hello"
        producer.join()
    }

    test("take returns null when an empty channel is closed") {
        val channel = QueueChannel.mpscBounded<String>(2)
        val producer = thread {
            Thread.sleep(50)
            channel.close()
        }

        channel.take() shouldBe null
        producer.join()
    }

    test("take drains a channel before returning null after close") {
        val channel = QueueChannel.mpscBounded<String>(2)
        channel.offer("hello")
        channel.close()

        channel.take() shouldBe "hello"
        channel.take() shouldBe null
    }

    test("forEach processes elements until close") {
        val channel = QueueChannel.mpscBounded<String>(2)
        val elements = mutableListOf<String>()
        val producer = thread {
            channel.offer("hello")
            channel.offer("world")
            channel.close()
        }

        channel.forEach { elements.add(it) }

        producer.join()
        elements shouldBe listOf("hello", "world")
    }

    test("forEach terminates immediately for a closed empty channel") {
        val channel = QueueChannel.mpscBounded<String>(2)
        channel.close()

        var count = 0
        channel.forEach { count++ }

        count shouldBe 0
    }

    test("forEachAsync processes elements on a background thread") {
        val channel = QueueChannel.mpscBounded<String>(2)
        val elements = mutableListOf<String>()
        val consumed = CountDownLatch(2)

        channel.forEachAsync {
            elements.add(it)
            consumed.countDown()
        }
        channel.offer("hello")
        channel.offer("world")

        consumed.await(5, TimeUnit.SECONDS) shouldBe true
        channel.close()
        elements shouldBe listOf("hello", "world")
    }

    test("OneShotChannel take blocks until its element is available") {
        val channel = OneShotChannel<String>()
        val producer = thread {
            Thread.sleep(50)
            channel.offer("hello")
        }

        channel.take() shouldBe "hello"
        producer.join()
        channel.isClosed shouldBe true
    }

    test("BroadcastChannel receivers support blocking iteration") {
        val channel = BroadcastChannel.mpscBounded<String>(2)
        val receiver = channel.subscribe()
        val elements = mutableListOf<String>()
        val producer = thread {
            channel.offer("hello")
            channel.offer("world")
            channel.close()
        }

        receiver.forEach { elements.add(it) }

        producer.join()
        elements shouldBe listOf("hello", "world")
    }

    val strategies =
        listOf<Pair<String, (ChannelReceiver<String>) -> ChannelReceiver<String>>>(
            "busy spin" to { it.withBusySpinBlockingStrategy() },
            "parking" to { it.withParkingBlockingStrategy() },
            "sleeping" to { it.withSleepBlockingStrategy(100, DurationUnit.NANOSECONDS) },
            "yielding" to { it.withYieldingBlockingStrategy() },
            "custom" to { it.withBlockingStrategy { Thread.yield() } },
        )

    strategies.forEach { (name, wrap) ->
        test("$name blocking strategy waits for an element") {
            val channel = QueueChannel.spscUnbounded<String>()
            val receiver = wrap(channel)
            val producer = thread {
                Thread.sleep(20)
                channel.offer("hello")
            }

            receiver.take() shouldBe "hello"
            producer.join()
        }
    }
})
