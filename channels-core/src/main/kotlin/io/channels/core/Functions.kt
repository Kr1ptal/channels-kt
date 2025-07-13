package io.channels.core

fun interface ChannelConsumer<T : Any> {
    fun accept(value: T)
}
