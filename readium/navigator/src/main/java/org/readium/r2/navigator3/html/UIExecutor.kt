package org.readium.r2.navigator3.html

import android.os.Handler
import android.os.Looper

internal class UIExecutor {

    private val handler: Handler =
        Handler(Looper.getMainLooper())

    private val callbacks: MutableList<Runnable> =
        mutableListOf()

    private var isDisposed: Boolean =
        false

    private val lock: Any =
        Any()

    fun execute(command: Runnable) {
        synchronized(this.lock) {
            if (!this.isDisposed) {
                this.callbacks.add(command)
                this.handler.post(command)
            }
        }
    }

    fun executeAfter(command: Runnable, ms: Long) {
        synchronized(this.lock) {
            if (!this.isDisposed) {
                this.callbacks.add(command)
                this.handler.postDelayed(command, ms)
            }
        }
    }

    fun dispose() {
        synchronized(this.lock) {
            this.isDisposed = true
            while (this.callbacks.isNotEmpty()) {
                val callback = this.callbacks.removeFirst()
                this.handler.removeCallbacks(callback)
            }
        }
    }
}
