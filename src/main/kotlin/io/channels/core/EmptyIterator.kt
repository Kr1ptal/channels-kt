package io.channels.core

/**
 * An empty iterator that can never return a value.
 * */
internal object EmptyIterator : Iterator<Nothing> {
    override fun hasNext(): Boolean = false

    override fun next(): Nothing = throw NoSuchElementException()
}
