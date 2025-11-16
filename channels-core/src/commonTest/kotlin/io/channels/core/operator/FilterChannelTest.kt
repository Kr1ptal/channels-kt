package io.channels.core.operator

import io.channels.core.QueueChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FilterChannelTest : FunSpec({
    test("filters elements based on predicate") {
        val source = QueueChannel.spscUnbounded<Int>()
        val filtered = source.filter { it % 2 == 0 }

        source.offer(1)
        source.offer(2)
        source.offer(3)
        source.offer(4)

        filtered.take() shouldBe 2
        filtered.take() shouldBe 4
        source.close()
        filtered.take() shouldBe null
    }

    test("poll returns only matching elements") {
        val source = QueueChannel.spscUnbounded<Int>()
        val filtered = source.filter { it > 2 }

        source.offer(1)
        source.offer(2)
        source.offer(3)
        source.offer(31)

        filtered.poll() shouldBe 3
        filtered.poll() shouldBe 31
        filtered.poll() shouldBe null
    }

    test("forEach only processes matching elements") {
        val source = QueueChannel.spscUnbounded<Int>()
        val filtered = source.filter { it % 2 == 0 }
        val results = mutableListOf<Int>()

        launch(Dispatchers.Default) {
            source.offer(1)
            source.offer(2)
            source.offer(3)
            source.offer(4)
            delay(100)
            source.close()
        }

        filtered.forEach { results.add(it) }
        results shouldBe listOf(2, 4)
    }

    test("propagates close state") {
        val source = QueueChannel.mpscBounded<Int>(2)
        val filtered = source.filter { it > 2 }

        filtered.close()
        filtered.isClosed shouldBe true
        filtered.take() shouldBe null
    }
})
