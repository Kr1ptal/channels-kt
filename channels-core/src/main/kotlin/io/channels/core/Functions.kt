package io.channels.core

fun interface ChannelConsumer<T : Any> {
    fun accept(value: T)
}

fun interface ChannelFunction<I : Any, O> {
    fun apply(value: I): O
}
