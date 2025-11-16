package io.channels.core.operator

import io.channels.core.QueueChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapNotNullChannelTest : FunSpec({
    test("filters out null mapped values") {
        val source = QueueChannel.spscUnbounded<Int>()
        val mapped = source.mapNotNull { if (it % 2 == 0) it * 2 else null }

        source.offer(1)
        source.offer(2)
        source.offer(3)
        source.offer(4)

        mapped.take() shouldBe 4
        mapped.take() shouldBe 8
        source.close()
        mapped.take() shouldBe null
    }

    test("poll skips null values") {
        val source = QueueChannel.spscUnbounded<Int>()
        val mapped = source.mapNotNull { if (it > 2) it * 2 else null }

        source.offer(1)
        source.offer(2)
        source.offer(3)

        mapped.poll() shouldBe 6
        mapped.poll() shouldBe null
    }

    test("forEach only processes non-null mapped values") {
        val source = QueueChannel.spscUnbounded<Int>()
        val mapped = source.mapNotNull { if (it % 2 == 0) it * 2 else null }
        val results = mutableListOf<Int>()

        launch(Dispatchers.Default) {
            source.offer(1)
            source.offer(2)
            source.offer(3)
            source.offer(4)
            delay(100)
            source.close()
        }

        mapped.forEach { results.add(it) }
        results shouldBe listOf(4, 8)
    }

    test("propagates close state") {
        val source = QueueChannel.mpscBounded<Int>(2)
        val mapped = source.mapNotNull { if (it % 2 == 0) it * 2 else null }

        mapped.close()
        mapped.isClosed shouldBe true
        mapped.take() shouldBe null
    }
})
