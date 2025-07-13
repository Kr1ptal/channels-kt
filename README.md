# <h1 align="center"> channels-kt </h1>

<p style="text-align: center;"> <b>channels-kt</b> is a high-performance abstraction over queues. It provides different flavors of
queues, such as <b>MPSC</b> (multiple-producer, single-consumer) and <b>SPSC</b> (single-producer, single-consumer).
It also contains specialized implementations of channels, such as <b>BroadcastChannel</b> and <b>OneShotChannel</b>.
</p>

## Features

- MPSC (multiple-producer, single-consumer) queues
- SPSC (single-producer, single-consumer) queues
- Broadcast channels
- One-shot channels
- Different blocking wait strategies: sleeping, parking, yielding, busy spinning, suspending (coroutines)
- Channel operators: `map`, `mapNotNull`, `filter`

## Usage

## 🚀 Quickstart

All releases are published to Maven Central. Changelog of each release can be found
under [Releases](https://github.com/Kr1ptal/channels-kt/releases).

It's recommended to define BOM platform dependency to ensure that all artifacts are compatible with each other.

```kotlin
// Define a maven repository where the library is published
repositories {
    mavenCentral()

    // for snapshot versions, use the following repository
    //maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
}

dependencies {
    // Define a BOM and its version
    implementation(platform("io.kriptal.channels:channels-bom:1.0.0"))

    // Define any required artifacts without version
    implementation("io.kriptal.channels:channels-core")
    implementation("io.kriptal.channels:channels-coroutines")
}
```

### Queue-based Channels

```kotlin
val channel = QueueChannel.mpscUnbounded<Int>()
channel.offer(1)
channel.offer(2)
channel.offer(3)

// iterate over the channel, until the channel is closed. 

// blocking the current thread
channel.forEach { element ->
    println(element)
}

// needs "channels-coroutines" dependency
channel.forEachSuspend { element ->
    println(element)
}
```
