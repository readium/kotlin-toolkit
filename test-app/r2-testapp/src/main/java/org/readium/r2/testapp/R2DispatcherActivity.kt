/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.app.Activity
import android.os.Bundle

import timber.log.Timber

/**
 * Created by aferditamuriqi on 1/16/18.
 */

class R2DispatcherActivity : Activity() {
    private val mMapper = R2IntentMapper(this, R2IntentHelper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            mMapper.dispatchIntent(intent)
        } catch (iae: IllegalArgumentException) {
            if (BuildConfig.DEBUG) {
                Timber.e(iae, "Deep links  - Invalid URI")
            }
        } finally {
            finish()
        }
    }
}