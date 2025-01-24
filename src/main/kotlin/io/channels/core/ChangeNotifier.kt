package io.channels.core

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Append-only collection of listeners that are invoked when the [notifyChange] method is called.
 * */
class ChangeNotifier {
    private val listeners = CopyOnWriteArrayList<Runnable>()

    /**
     * Register a listener that will be notified when the state changes.
     * */
    fun register(listener: Runnable) {
        listeners.add(listener)
    }

    /**
     * Notify all listeners that the state has changed.
     * */
    fun notifyChange() {
        for (i in listeners.indices) {
            listeners[i].run()
        }
    }
}
