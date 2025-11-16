package io.channels.core.operator

import io.channels.core.QueueChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapChannelTest : FunSpec({
    test("maps elements synchronously") {
        val source = QueueChannel.spscUnbounded<Int>()
        val mapped = source.map { it * 2 }

        source.offer(1)
        source.offer(2)
        source.offer(3)

        mapped.take() shouldBe 2
        mapped.take() shouldBe 4
        mapped.take() shouldBe 6
    }

    test("forEach processes all mapped elements") {
        val source = QueueChannel.spscUnbounded<Int>()
        val mapped = source.map { it * 2 }
        val results = mutableListOf<Int>()

        launch(Dispatchers.Default) {
            source.offer(1)
            source.offer(2)
            source.offer(3)
            delay(100)
            source.close()
        }

        mapped.forEach { results.add(it) }
        results shouldBe listOf(2, 4, 6)
    }

    test("propagates close state") {
        val source = QueueChannel.spscUnbounded<Int>()
        val mapped = source.map { it * 2 }

        mapped.close()
        mapped.isClosed shouldBe true
        mapped.take() shouldBe null
    }
})
