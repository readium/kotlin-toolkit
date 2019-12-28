package org.readium.r2.testapp.utils

import android.view.View


/**
 * Prevents from double clicks on a view, which could otherwise lead to unpredictable states. Useful
 * while transitioning to another activity for instance.
 */
class SingleClickListener(private val click: (v: View) -> Unit) : View.OnClickListener {

    companion object {
        private const val DOUBLE_CLICK_TIMEOUT = 2500
    }

    private var lastClick: Long = 0

    override fun onClick(v: View) {
        if (getLastClickTimeout() > DOUBLE_CLICK_TIMEOUT) {
            lastClick = System.currentTimeMillis()
            click(v)
        }
    }

    private fun getLastClickTimeout(): Long {
        return System.currentTimeMillis() - lastClick
    }
}

fun View.singleClick(l: (View) -> Unit) {
    setOnClickListener(SingleClickListener(l))
}
