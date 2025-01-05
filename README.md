# <h1 align="center"> channels-kt </h1>

<p style="text-align: center;"> <b>channels-kt</b> is a high-performance abstraction over queues. It provides different flavors of
queues, such as <b>MPSC</b> (multiple-producer, single-consumer) and <b>SPSC</b> (single-producer, single-consumer).
It also contains specialized implementations of channels, such as <b>BroadcastChannel</b> and <b>OneShotChannel</b>.
</p>

## Features

- Multiple-producer, single-consumer queues
- Single-producer, single-consumer queues
- Broadcast channels
- One-shot channels
- Different blocking wait strategies: sleeping, parking, yielding, busy spinning

## Usage

### Queues

```kotlin
import io.channels.core.QueueChannel

val channel = QueueChannel.mpscUnbounded<Int>()
channel.offer(1)
channel.offer(2)
channel.offer(3)

// iterate over the channel, until the channel is closed. 

// blocking the current thread
for (element in channel) {
    println(element)
}

// does NOT block the current thread
channel.forEachAsync { element ->
    println(element)
}


