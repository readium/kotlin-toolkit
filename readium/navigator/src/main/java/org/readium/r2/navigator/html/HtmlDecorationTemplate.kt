/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.html

import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlin.reflect.KClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.Decoration.Style
import org.readium.r2.shared.JSONable

/**
 * An [HtmlDecorationTemplate] renders a [Decoration] into a set of HTML elements and associated
 * stylesheet.
 *
 * @param layout Determines the number of created HTML elements and their position relative to the
 *        matching DOM range.
 * @param width Indicates how the width of each created HTML element expands in the viewport.
 * @param element Closure used to generate a new HTML element for the given [Decoration]. Several
 *        elements will be created for a single decoration when using the BOXES layout.
 *        The Navigator will automatically position the created elements according to the
 *        decoration's Locator. The template is only responsible for the look and feel of the
 *        generated elements.
 *        Every child elements with a `data-activable="1"` HTML attribute will handle tap events.
 *        If no element has this attribute, the root element will handle taps.
 * @param stylesheet A CSS stylesheet which will be injected in the resource, which can be
 *        referenced by the created elements. Make sure to use unique identifiers for your classes
 *        and IDs to avoid conflicts with the HTML resource itself. Best practice is to prefix with
 *        your app name. r2- and readium- are reserved by the Readium toolkit.
 */
public data class HtmlDecorationTemplate(
    val layout: Layout,
    val width: Width = Width.WRAP,
    val element: (Decoration) -> String = { "<div/>" },
    val stylesheet: String? = null,
) : JSONable {

    /**
     * Determines the number of created HTML elements and their position relative to the matching
     * DOM range.
     */
    @Parcelize
    public enum class Layout(public val value: String) : Parcelable {
        /** A single HTML element covering the smallest region containing all CSS border boxes. */
        BOUNDS("bounds"),

        /** One HTML element for each CSS border box (e.g. line of text). */
        BOXES("boxes"),
    }

    /**
     * Indicates how the width of each created HTML element expands in the viewport.
     */
    @Parcelize
    public enum class Width(public val value: String) : Parcelable {
        /** Smallest width fitting the CSS border box. */
        WRAP("wrap"),

        /** Fills the bounds layout. */
        BOUNDS("bounds"),

        /** Fills the anchor page, useful for dual page. */
        VIEWPORT("viewport"),

        /** Fills the whole viewport. */
        PAGE("page"),
    }

    private data class Padding(
        val left: Int = 0,
        val top: Int = 0,
        val right: Int = 0,
        val bottom: Int = 0,
    )

    override fun toJSON(): JSONObject = JSONObject().apply {
        put("layout", layout.value)
        put("width", width.value)
        putOpt("stylesheet", stylesheet)
    }

    public companion object {

        /** Creates a new decoration template for the highlight style. */
        public fun highlight(
            @ColorInt defaultTint: Int,
            lineWeight: Int,
            cornerRadius: Int,
            alpha: Double,
            experimentalPositioning: Boolean = false,
        ): HtmlDecorationTemplate =
            createTemplate(
                asHighlight = true,
                defaultTint = defaultTint,
                lineWeight = lineWeight,
                cornerRadius = cornerRadius,
                alpha = alpha,
                experimentalPositioning = experimentalPositioning,
            )

        /** Creates a new decoration template for the underline style. */
        public fun underline(
            @ColorInt defaultTint: Int,
            lineWeight: Int,
            cornerRadius: Int,
            alpha: Double,
            experimentalPositioning: Boolean = false,
        ): HtmlDecorationTemplate =
            createTemplate(
                asHighlight = false,
                defaultTint = defaultTint,
                lineWeight = lineWeight,
                cornerRadius = cornerRadius,
                alpha = alpha,
                experimentalPositioning = experimentalPositioning,
            )

        /**
         * @param asHighlight When true, the non active style is of an highlight. Otherwise, it is
         *        an underline.
         */
        private fun createTemplate(
            asHighlight: Boolean,
            @ColorInt defaultTint: Int,
            lineWeight: Int,
            cornerRadius: Int,
            alpha: Double,
            experimentalPositioning: Boolean = false,
        ): HtmlDecorationTemplate {
            val className = createUniqueClassName(if (asHighlight) "highlight" else "underline")
            val padding = Padding(left = 1, right = 1)

            return HtmlDecorationTemplate(
                layout = Layout.BOXES,
                element = { decoration ->
                    val tint = (decoration.style as? Style.Tinted)?.tint ?: defaultTint
                    val isActive = (decoration.style as? Style.Activable)?.isActive ?: false
                    val css = buildString {
                        if (asHighlight || isActive) {
                            append("background-color: ${tint.toCss(alpha = alpha)} !important;")
                        }
                        if (experimentalPositioning) {
                            append("--decoration-z-index: -1;")
                        }
                        if (!asHighlight || isActive) {
                            append("--underline-color: ${tint.toCss()};")
                        }
                    }
                    """<div class="$className" style="$css"/>"""
                },
                stylesheet = """
            .$className {
                margin: ${-padding.top}px ${-padding.left}px 0 0;
                padding: 0 ${padding.left + padding.right}px ${padding.top + padding.bottom}px 0;
                border-radius: ${cornerRadius}px;
                box-sizing: border-box;
                border: 0 solid var(--underline-color);
                z-index: var(--decoration-z-index);
            }
            
            /* Horizontal (default) */
            [data-writing-mode="horizontal-tb"].$className {
                border-bottom-width: ${lineWeight}px;
            }
            
            /* Vertical right-to-left */
            [data-writing-mode="vertical-rl"].$className,
            [data-writing-mode="sideways-rl"].$className {
                border-left-width: ${lineWeight}px;
            }
            
            /* Vertical left-to-right */
            [data-writing-mode="vertical-lr"].$className,
            [data-writing-mode="sideways-lr"].$className {
                border-right-width: ${lineWeight}px;
            }            
            """
            )
        }

        private var classNamesId = 0
        private fun createUniqueClassName(key: String): String =
            "r2-$key-${++classNamesId}"
    }
}

public class HtmlDecorationTemplates private constructor(
    internal val styles: MutableMap<KClass<*>, HtmlDecorationTemplate> = mutableMapOf(),
) : JSONable {

    public operator fun <S : Style> get(style: KClass<S>): HtmlDecorationTemplate? =
        styles[style]

    public operator fun <S : Style> set(style: KClass<S>, template: HtmlDecorationTemplate) {
        styles[style] = template
    }

    override fun toJSON(): JSONObject = JSONObject(
        styles.entries.associate {
            it.key.qualifiedName!! to it.value.toJSON()
        }
    )

    public fun copy(): HtmlDecorationTemplates = HtmlDecorationTemplates(styles.toMutableMap())

    public companion object {
        public operator fun invoke(build: HtmlDecorationTemplates.() -> Unit): HtmlDecorationTemplates =
            HtmlDecorationTemplates().apply(build)

        /**
         * Creates the default list of decoration styles with associated HTML templates.
         */
        public fun defaultTemplates(
            @ColorInt defaultTint: Int = Color.YELLOW,
            lineWeight: Int = 2,
            cornerRadius: Int = 3,
            alpha: Double = 0.3,
            experimentalPositioning: Boolean = false,
        ): HtmlDecorationTemplates = HtmlDecorationTemplates {
            set(
                Style.Highlight::class,
                HtmlDecorationTemplate.highlight(
                    defaultTint = defaultTint,
                    lineWeight = lineWeight,
                    cornerRadius = cornerRadius,
                    alpha = alpha,
                    experimentalPositioning = experimentalPositioning,
                )
            )
            set(
                Style.Underline::class,
                HtmlDecorationTemplate.underline(
                    defaultTint = defaultTint,
                    lineWeight = lineWeight,
                    cornerRadius = cornerRadius,
                    alpha = alpha,
                    experimentalPositioning = experimentalPositioning,
                )
            )
        }
    }
}

/**
 * Converts the receiver color int to a CSS expression.
 *
 * @param alpha When set, overrides the actual color alpha.
 */
public fun @receiver:ColorInt Int.toCss(alpha: Double? = null): String {
    val r = Color.red(this)
    val g = Color.green(this)
    val b = Color.blue(this)
    val a = alpha ?: (Color.alpha(this).toDouble() / 255)
    return "rgba($r, $g, $b, $a)"
}
