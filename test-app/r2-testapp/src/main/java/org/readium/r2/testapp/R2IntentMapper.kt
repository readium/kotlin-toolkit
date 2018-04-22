package org.readium.r2.testapp

import android.content.Context
import android.content.Intent

/**
 * Created by aferditamuriqi on 1/16/18.
 */

class R2IntentMapper(private val mContext: Context, private val mIntents: R2IntentHelper) {

    private val TAG = this::class.java.simpleName

    fun dispatchIntent(intent: Intent) {
        val uri = intent.data ?: throw IllegalArgumentException("Uri cannot be null")
        if (uri.toString().contains(".")) {
            val extension = uri.toString().substring(uri.toString().lastIndexOf("."))
            if (extension.equals(".lcpl")) {
                mContext.startActivity(mIntents.catalogActivityIntent(mContext, uri, true))
            }
            else {
                val dispatchIntent = mIntents.catalogActivityIntent(mContext, uri)
                mContext.startActivity(dispatchIntent)
            }
        }
    }
}
