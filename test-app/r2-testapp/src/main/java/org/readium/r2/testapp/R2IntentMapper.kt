package org.readium.r2.testapp

import android.content.Context
import android.content.Intent

/**
 * Created by aferditamuriqi on 1/16/18.
 */

class R2IntentMapper(private val mContext: Context, private val mIntents: R2IntentHelper) {

    private val TAG = this::class.java.simpleName

    fun dispatchIntent(intent: Intent) {
        val uri = intent.data

        if (uri == null) throw IllegalArgumentException("Uri cannot be null")

        val dispatchIntent = mIntents.newAActivityIntent(mContext, uri)

        mContext.startActivity(dispatchIntent)
    }

}
