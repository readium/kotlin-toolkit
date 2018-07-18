/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by aferditamuriqi on 1/16/18.
 */

public class R2IntentHelper {

    public static final String URI = "URI";
    public static final String LCP = "LCP";

    public Intent catalogActivityIntent(Context context, Uri uri) {
        return catalogActivityIntent(context,uri, false);
    }

    public Intent catalogActivityIntent(Context context, Uri uri, boolean lcp) {
        Intent i = new Intent(context, CatalogActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(LCP, lcp);
        i.putExtra(URI, uri.toString());
        return i;
    }
}