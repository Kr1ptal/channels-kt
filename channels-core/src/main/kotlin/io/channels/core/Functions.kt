package io.channels.core

/**
 * Represents an operation that accepts a single input argument and returns no result
 * */
fun interface ChannelConsumer<T : Any> {
    fun accept(value: T)
}

/**
 * Represents a function that accepts one argument and produces a result.
 * */
fun interface ChannelFunction<I : Any, O> {
    fun apply(value: I): O
}

/**
 * Represents a predicate (boolean-valued function) of one argument.
 * */
fun interface ChannelPredicate<T : Any> {
    fun test(value: T): Boolean
}
