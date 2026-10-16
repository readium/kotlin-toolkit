/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.resources

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import android.util.Xml
import androidx.annotation.FontRes
import androidx.core.R as AndroidxCoreR
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.epub.css.FontWeight
import org.readium.r2.shared.ExperimentalReadiumApi
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber

/**
 * A non italic font style
 *
 * Android before API 29 doesn't provide public constants for styles
 *
 * See [android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT]
 */
private const val FONT_STYLE_NORMAL = 0

/**
 * Iterates over all font entries declared by an Android font resource.
 *
 * The resource can be either a single font file (eg `res/font/my_font.ttf`) or a
 * `font-family` XML declaration (eg `res/font/my_family.xml`).
 *
 * For each resolved font face, [builder] receives the concrete font resource id together with
 * its parsed style and weight so the caller can declare one `@font-face` per variant.
 */
@ExperimentalReadiumApi
internal fun forEachFontInResource(
    context: Context,
    @FontRes fontResId: Int,
    builder: (fontResId: Int, fontWeight: FontWeight, fontStyle: FontStyle) -> Unit,
) {
    // path to the resource
    val path = TypedValue()
        .also { context.resources.getValue(fontResId, it, true) }
        .coerceToString()
        ?.toString()
        .orEmpty()

    if (path.endsWith(".xml", ignoreCase = true)) {
        parseXmlFontFamily(
            resources = context.resources,
            fontResId = fontResId,
            builder = builder,
        )
    } else {
        parseSingleFont(
            context = context,
            fontResId = fontResId,
            builder = builder
        )
    }
}

/**
 * Parses an XML font-family resource in `res/font/` and emits one callback per `<font>` entry.
 *
 * Invalid XML roots or unsupported entries are ignored to keep this helper resilient to
 * vendor-specific variations.
 */
@ExperimentalReadiumApi
private fun parseXmlFontFamily(
    resources: Resources,
    @FontRes fontResId: Int,
    builder: (resId: Int, fontWeight: FontWeight, fontStyle: FontStyle) -> Unit,
) {
    @Suppress("ResourceType")
    resources.getXml(fontResId)
        .use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    break
                }
                parser.next()
            }

            if (parser.eventType != XmlPullParser.START_TAG || parser.name != "font-family") {
                return@use
            }

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "font") {
                    parseSingleXmlFont(
                        resources = resources,
                        parser = parser,
                        builder = builder,
                    )
                }

                parser.next()
            }
        }
}

/**
 * Resolves a non-XML font resource (`.ttf`/`.otf`) and infers its style metadata from
 * [android.graphics.Typeface].
 */
@ExperimentalReadiumApi
private fun parseSingleFont(
    context: Context,
    @FontRes fontResId: Int,
    builder: (resId: Int, fontWeight: FontWeight, fontStyle: FontStyle) -> Unit,
) {
    // Let's try to get a single font Typeface
    val typeface = ResourcesCompat.getFont(context, fontResId) ?: run {
        Timber.d("The provided font family resource cannot be parsed")

        return
    }

    val weight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        FontWeight.entries.firstOrNull { it.value == typeface.weight } ?: FontWeight.NORMAL
    } else {
        if (typeface.isBold) {
            FontWeight.BOLD
        } else {
            FontWeight.NORMAL
        }
    }

    val style = if (typeface.isItalic) {
        FontStyle.ITALIC
    } else {
        FontStyle.NORMAL
    }

    builder(fontResId, weight, style)
}

/**
 * Parses a single `<font>` node from a font-family XML resource.
 *
 * The Android and support-library attribute variants are both supported so this works across
 * API levels and packaging modes.
 */
@ExperimentalReadiumApi
private fun parseSingleXmlFont(
    resources: Resources,
    parser: XmlPullParser,
    builder: (resId: Int, fontWeight: FontWeight, fontStyle: FontStyle) -> Unit,
) {
    resources.obtainAttributes(
        Xml.asAttributeSet(parser),
        AndroidxCoreR.styleable.FontFamilyFont
    ).use { typedArray ->
        val fontAttr = when {
            typedArray.hasValue(AndroidxCoreR.styleable.FontFamilyFont_font) ->
                AndroidxCoreR.styleable.FontFamilyFont_font

            typedArray.hasValue(AndroidxCoreR.styleable.FontFamilyFont_android_font) ->
                AndroidxCoreR.styleable.FontFamilyFont_android_font

            else -> {
                return@use
            }
        }

        val resId = typedArray.getResourceId(fontAttr, 0)
            .takeIf { it != 0 }
            ?: return@use

        val weight = typedArray.getInt(
            if (typedArray.hasValue(AndroidxCoreR.styleable.FontFamilyFont_fontWeight)) {
                AndroidxCoreR.styleable.FontFamilyFont_fontWeight
            } else {
                AndroidxCoreR.styleable.FontFamilyFont_android_fontWeight
            },
            FontWeight.NORMAL.value
        ).let { weight ->
            FontWeight.entries.firstOrNull { it.value == weight } ?: FontWeight.NORMAL
        }

        // A normal, non italic font style
        val normalStyle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT
        } else {
            FONT_STYLE_NORMAL
        }

        val fontStyleValue = typedArray.getInt(
            if (typedArray.hasValue(AndroidxCoreR.styleable.FontFamilyFont_fontStyle)) {
                AndroidxCoreR.styleable.FontFamilyFont_fontStyle
            } else {
                AndroidxCoreR.styleable.FontFamilyFont_android_fontStyle
            },
            normalStyle
        )

        val style = if (fontStyleValue == normalStyle) {
            FontStyle.NORMAL
        } else {
            FontStyle.ITALIC
        }

        builder(resId, weight, style)
    }
}
