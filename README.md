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
- JVM/Android blocking wait strategies: sleeping, parking, yielding, and busy spinning
- Suspending receivers with coroutines on every target
- Channel operators: `map`, `mapNotNull`, `filter`

Supported Kotlin Multiplatform targets are JVM, Android, JavaScript (Node.js), macOS ARM64, and iOS
(ARM64, x64, and simulator ARM64).

The common API exposes non-blocking `offer()` and `poll()` operations. Synchronous `take()`, `forEach()`, and blocking
strategy member functions are available only from JVM/Android source sets, including ordinary Java calls such as
`receiver.withBusySpinBlockingStrategy().take()`. JavaScript and Apple targets should use the suspending APIs from
`channels-core` when they need to wait for values.

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
    implementation(platform("io.kriptal.channels:channels-bom:2.0.0"))

    // For the latest snapshot (requires the snapshot repository above)
    // implementation(platform("io.kriptal.channels:channels-bom:1.0.5-SNAPSHOT"))

    // Core includes non-blocking queues and common coroutine-based receivers
    implementation("io.kriptal.channels:channels-core")
}
```

### Queue-based Channels

```kotlin
val channel = QueueChannel.mpscUnbounded<Int>()
channel.offer(1)
channel.offer(2)
channel.offer(3)

// iterate over the channel, until the channel is closed. 

// JVM/Android only: blocks the current thread
channel.forEach { element ->
    println(element)
}

// common suspending API from channels-core
channel.forEachSuspend { element ->
    println(element)
}
```
