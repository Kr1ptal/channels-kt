package io.channels.core.operator

import io.channels.core.QueueChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FilterChannelTest : FunSpec({
    test("filters elements based on predicate") {
        val source = QueueChannel.spscUnbounded<Int>()
        val filtered = source.filter { it % 2 == 0 }

        source.offer(1)
        source.offer(2)
        source.offer(3)
        source.offer(4)

        filtered.poll() shouldBe 2
        filtered.poll() shouldBe 4
        source.close()
        filtered.poll() shouldBe null
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

    test("forEachSuspend only processes matching elements") {
        val source = QueueChannel.spscUnbounded<Int>()
        val filtered = source.filter { it % 2 == 0 }
        val results = mutableListOf<Int>()

        source.offer(1)
        source.offer(2)
        source.offer(3)
        source.offer(4)
        source.close()

        filtered.forEachSuspend { results.add(it) }
        results shouldBe listOf(2, 4)
    }

    test("propagates close state") {
        val source = QueueChannel.mpscBounded<Int>(2)
        val filtered = source.filter { it > 2 }

        filtered.close()
        filtered.isClosed shouldBe true
        filtered.poll() shouldBe null
    }
})
