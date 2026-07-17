@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.MapCompanion

/**
 * Hint about the nature of the layout for the publication.
 * https://readium.org/webpub-manifest/contexts/default/#layout-and-reading-progression
 */
@Parcelize
public enum class Layout(public val value: String) : Parcelable {

    /**
     * Each resource is a “page” where both dimensions are usually contained in
     * the device’s viewport. Based on user preferences, the reading system may
     * also display two resources side by side in a spread.
     *
     * Formats: Divina, FXL EPUB or PDF
     */
    FIXED("fixed"),

    /**
     * Reading systems are free to adapt text and layout entirely based on user
     * preferences.
     *
     * Formats: Reflowable EPUB
     */
    REFLOWABLE("reflowable"),

    /**
     * Resources are displayed in a continuous scroll, usually by filling the
     * width of the viewport, without any visible gap between between spine items.
     *
     * Formats: Scrolled Divina
     */
    SCROLLED("scrolled"),
    ;

    public companion object : MapCompanion<String, Layout>(
        entries.toTypedArray(),
        Layout::value
    )
}
