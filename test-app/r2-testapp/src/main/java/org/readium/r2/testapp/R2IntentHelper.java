package org.readium.r2.testapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by aferditamuriqi on 1/16/18.
 */

public class R2IntentHelper {

    public static String URI = "URI";

    public Intent newAActivityIntent(Context context, Uri uri) {
        Intent i = new Intent(context, CatalogActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(URI, uri.toString());
        return i;
    }

}