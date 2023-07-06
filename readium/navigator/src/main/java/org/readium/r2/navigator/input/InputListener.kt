package org.readium.r2.navigator.input

import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
interface InputListener {
    /**
     * Called when the user tapped the content, but nothing handled the event internally (eg.
     * by following an internal link).
     */
    fun onTap(event: TapEvent): Boolean = false

    /**
     * Called when the user dragged the content, but nothing handled the event internally.
     */
    fun onDrag(event: DragEvent): Boolean = false

    /**
     * Called when the user pressed or released a key, but nothing handled the event internally.
     */
    fun onKey(event: KeyboardEvent): Boolean = false
}

@OptIn(ExperimentalReadiumApi::class)
internal class CompositeInputListener: InputListener {
    private val listeners = mutableListOf<InputListener>()

    fun add(listener: InputListener) {
        listeners.add(listener)
    }

    fun remove(listener: InputListener) {
        listeners.remove(listener)
    }

    override fun onTap(event: TapEvent): Boolean =
        listeners.any { it.onTap(event) }

    override fun onDrag(event: DragEvent): Boolean =
        listeners.any { it.onDrag(event) }

    override fun onKey(event: KeyboardEvent): Boolean =
        listeners.any { it.onKey(event) }
}