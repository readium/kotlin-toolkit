package org.readium.r2.navigator.input

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Utility view to intercept keyboard events and forward them to a [VisualNavigator.Listener].
 */
@SuppressLint("ViewConstructor")
@OptIn(ExperimentalReadiumApi::class)
internal class KeyInterceptorView(
    view: View,
    private val listener: InputListener?
) : FrameLayout(view.context) {

    init {
        addView(view)

        isFocusable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false
        }
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        event?.let { listener?.onKey(KeyEvent(KeyEvent.Type.Up, it)) }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        event?.let { listener?.onKey(KeyEvent(KeyEvent.Type.Down, it)) }
        return super.onKeyDown(keyCode, event)
    }
}