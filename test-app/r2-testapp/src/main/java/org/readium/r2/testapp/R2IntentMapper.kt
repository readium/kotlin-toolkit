package org.readium.r2.testapp

import android.content.Context
import android.content.Intent
import android.net.Uri


/**
 * Created by aferditamuriqi on 1/16/18.
 */

class R2IntentMapper(private val mContext: Context, private val mIntents: R2IntentHelper) {

    private val TAG = this::class.java.simpleName

    fun dispatchIntent(intent: Intent) {

        // Get intent, action and MIME type
        val action = intent.action
        val type = intent.type
        val uri: Uri
        if (Intent.ACTION_SEND == action && type != null) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } else {
            // Handle other intents, such as being started from the home screen
            uri = intent.data ?: throw IllegalArgumentException("Uri cannot be null")
        }

        if (uri.toString().contains(".")) {
            val extension = uri.toString().substring(uri.toString().lastIndexOf("."))
            if (extension.equals(".lcpl")) {
                mContext.startActivity(mIntents.catalogActivityIntent(mContext, uri, true))
            } else {
                val dispatchIntent = mIntents.catalogActivityIntent(mContext, uri)
                mContext.startActivity(dispatchIntent)
            }
        }
    }
}
