package io.channels.coroutines

import io.channels.core.OneShotChannel
import io.channels.core.QueueChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ReceiverExtensionsTest : FunSpec({
    context("forEachSuspend") {
        test("processes elements from QueueChannel") {
            val channel = QueueChannel.mpscUnbounded<String>()
            val results = mutableListOf<String>()

            val job = launch {
                channel.forEachSuspend {
                    results.add(it)
                }
            }

            channel.offer("hello")
            channel.offer("world")
            channel.close()
            job.join()

            results shouldBe listOf("hello", "world")
        }

        test("processes single element from OneShotChannel") {
            val channel = OneShotChannel<String>()
            val results = mutableListOf<String>()

            val job = launch {
                channel.forEachSuspend {
                    results.add(it)
                }
            }

            channel.offer("hello")
            job.join()

            results shouldBe listOf("hello")
            channel.isClosed shouldBe true
        }

        test("suspends until element is available") {
            val channel = QueueChannel.mpscUnbounded<String>()
            val results = mutableListOf<String>()

            val job = launch {
                channel.forEachSuspend {
                    results.add(it)
                }
            }

            channel.offer("hello")
            channel.offer("world")
            channel.close()
            job.join()

            results shouldBe listOf("hello", "world")
        }

        test("handles coroutine cancellation") {
            val channel = QueueChannel.mpscUnbounded<String>()
            val results = mutableListOf<String>()

            val job = launch {
                channel.forEachSuspend {
                    results.add(it)
                }
            }
            channel.offer("hello")
            delay(50)

            job.cancel()
            job.join()

            job.isActive shouldBe false
            job.isCancelled shouldBe true
            channel.isClosed shouldBe true
            results shouldBe listOf("hello")
        }
    }

    context("asCoroutineReceiver") {
        test("converts QueueChannel to ReceiveChannel") {
            val channel = QueueChannel.mpscUnbounded<String>()
            val results = mutableListOf<String>()

            val receiver = channel.asCoroutineReceiver()
            val job = launch {
                for (item in receiver) {
                    results.add(item)
                }
            }

            channel.offer("hello")
            channel.offer("world")
            channel.close()
            job.join()

            results shouldBe listOf("hello", "world")
        }

        test("converts OneShotChannel to ReceiveChannel") {
            val channel = OneShotChannel<String>()
            val results = mutableListOf<String>()

            val receiver = channel.asCoroutineReceiver()
            val job = launch {
                for (item in receiver) {
                    results.add(item)
                }
            }

            channel.offer("hello")
            job.join()

            results shouldBe listOf("hello")
            channel.isClosed shouldBe true
        }

        test("handles backpressure with rendezvous channel") {
            val channel = QueueChannel.mpscUnbounded<Int>()

            val receiver = channel.asCoroutineReceiver()
            val producerJob = launch {
                repeat(500) { channel.offer(it) }
                channel.close()
            }

            var sum = 0
            for (item in receiver) {
                sum += item
                delay(1.milliseconds) // Simulate slow consumer
            }

            producerJob.join()
            sum shouldBe (0..499).sum()
        }

        test("closes both channels on cancellation") {
            val channel = QueueChannel.mpscUnbounded<String>()
            val receiver = channel.asCoroutineReceiver()

            channel.offer("hello")
            receiver.cancel()

            delay(100) // Give time for cleanup
            channel.isClosed shouldBe true
            receiver.isClosedForReceive shouldBe true
        }

        test("properly closes on normal completion") {
            val channel = QueueChannel.mpscUnbounded<String>()

            val receiver = channel.asCoroutineReceiver()
            val job = launch {
                for (item in receiver) {
                    item.length
                }
            }

            channel.offer("hello")
            channel.close()
            job.join()

            channel.isClosed shouldBe true
            receiver.isClosedForReceive shouldBe true
        }
    }
})
