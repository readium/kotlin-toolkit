package org.readium.r2.navigator.input

import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public interface InputListener {
    /**
     * Called when the user tapped the content, but nothing handled the event internally (eg.
     * by following an internal link).
     */
    public fun onTap(navigator: VisualNavigator, event: TapEvent): Boolean = false

    /**
     * Called when the user dragged the content, but nothing handled the event internally.
     */
    public fun onDrag(navigator: VisualNavigator, event: DragEvent): Boolean = false

    /**
     * Called when the user pressed or released a key, but nothing handled the event internally.
     */
    public fun onKey(navigator: VisualNavigator, event: KeyEvent): Boolean = false
}

@OptIn(ExperimentalReadiumApi::class)
internal class CompositeInputListener : InputListener {
    private val listeners = mutableListOf<InputListener>()

    fun add(listener: InputListener) {
        listeners.add(listener)
    }

    fun remove(listener: InputListener) {
        listeners.remove(listener)
    }

    override fun onTap(navigator: VisualNavigator, event: TapEvent): Boolean =
        listeners.any { it.onTap(navigator, event) }

    override fun onDrag(navigator: VisualNavigator, event: DragEvent): Boolean =
        listeners.any { it.onDrag(navigator, event) }

    override fun onKey(navigator: VisualNavigator, event: KeyEvent): Boolean =
        listeners.any { it.onKey(navigator, event) }
}
