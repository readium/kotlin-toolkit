/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalReadiumApi::class)
class ReadiumCssTest {

    @Test
    fun `Inject with a simple HEAD`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </head>
                    <body dir="ltr"></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Inject with a HEAD and BODY with attributes`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head xmlns:xlink="http://www.w3.org/1999/xlink">
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </head>
                    <body dir="ltr" xmlns:xlink="http://www.w3.org/1999/xlink"></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head xmlns:xlink="http://www.w3.org/1999/xlink">
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body xmlns:xlink="http://www.w3.org/1999/xlink"></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Inject with a document on a single line`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?><html dir="ltr" xmlns="http://www.w3.org/1999/xhtml"><head xmlns:xlink="http://www.w3.org/1999/xlink">
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                <title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </head><body dir="ltr" xmlns:xlink="http://www.w3.org/1999/xlink"></body></html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head xmlns:xlink="http://www.w3.org/1999/xlink"><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/></head><body xmlns:xlink="http://www.w3.org/1999/xlink"></body></html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Inject with uppercase tags`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?><HTML dir="ltr" xmlns="http://www.w3.org/1999/xhtml"><HEAD xmlns:xlink="http://www.w3.org/1999/xlink">
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                <title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </HEAD><BODY dir="ltr" xmlns:xlink="http://www.w3.org/1999/xlink"></BODY></HTML>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?><HTML xmlns="http://www.w3.org/1999/xhtml"><HEAD xmlns:xlink="http://www.w3.org/1999/xlink"><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/></HEAD><BODY xmlns:xlink="http://www.w3.org/1999/xlink"></BODY></HTML>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Inject default styles with a resource without any styles`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-default.css"/>
                
                        <title>Publication</title>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </head>
                    <body dir="ltr"></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                        </head>
                        <body></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Do not inject default styles if the resource is already styled`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        // A <link> tag is considered styled.
        assertFalse(
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link href="style"/>
                        </head>
                        <body></body>
                    </html>
                """.trimIndent()
            ).contains("ReadiumCSS-default.css")
        )

        // An inline style="" attribute is considered styled.
        assertFalse(
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                        </head>
                        <body style="color: red;"></body>
                    </html>
                """.trimIndent()
            ).contains("ReadiumCSS-default.css")
        )

        // A <style> tag is considered styled.
        assertFalse(
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <style>color: red;</style>
                        </head>
                        <body></body>
                    </html>
                """.trimIndent()
            ).contains("ReadiumCSS-default.css")
        )
    }

    @Test
    fun `Inject RTL stylesheets`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.Rtl,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="rtl" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/rtl/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/rtl/ReadiumCSS-after.css"/>
                </head>
                    <body dir="rtl"></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Inject horizontal CJK stylesheets`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.CjkHorizontal,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/cjk-horizontal/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/cjk-horizontal/ReadiumCSS-after.css"/>
                </head>
                    <body dir="ltr"></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    // https://github.com/readium/readium-css/tree/master/css/dist#vertical
    @Test
    fun `Inject vertical CJK stylesheets`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.CjkVertical,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/cjk-vertical/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/cjk-vertical/ReadiumCSS-after.css"/>
                </head>
                    <body></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Inject language when missing`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = Language("fr-CA"),
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html xml:lang="fr-CA" dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </head>
                    <body xml:lang="fr-CA" dir="ltr"></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Copy BODY lang to HTML`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = Language("fr-CA"),
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html xml:lang="en-US" dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </head>
                    <body dir="ltr" xml:lang="en-US"></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body xml:lang="en-US"></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Copy BODY lang to HTML without namespace`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = Language("fr-CA"),
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html xml:lang="en-US" dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </head>
                    <body dir="ltr" lang="en-US"></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body lang="en-US"></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Copy BODY lang to HTML when empty`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = Language("fr-CA"),
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html xml:lang="fr-CA" dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </head>
                    <body dir="ltr" xml:lang=""></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body xml:lang=""></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Inject font declarations`() {
        val sut = ReadiumCss(
            fontFamilyDeclarations = listOf(
                buildFontFamilyDeclaration("Libre Franklin", alternates = emptyList()) {
                    addFontFace {
                        addSource("fonts/LibreFranklin.otf")
                    }
                },
                buildFontFamilyDeclaration("Open Dyslexic", alternates = emptyList()) {
                    addFontFace {
                        addSource("fonts/OpenDyslexic.otf")
                    }
                }
            ),
            googleFonts = listOf(
                FontFamily.OPEN_DYSLEXIC,
                FontFamily.SANS_SERIF,
                FontFamily.SERIF,
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                                    <style type="text/css">
                                    @import url('https://fonts.googleapis.com/css?family=OpenDyslexic%7Csans-serif%7Cserif');
                @font-face { font-family: "Libre Franklin"; src: url("/assets/fonts/LibreFranklin.otf"); }
                @font-face { font-family: "Open Dyslexic"; src: url("/assets/fonts/OpenDyslexic.otf"); }
                                    </style>
                </head>
                    <body dir="ltr"></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <body></body>
                    </html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Remove existing dir attributes in html and body`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            ),
            assetsBaseHref = "/assets/"
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                                <style>
                                    :root[style], :root { overflow: visible !important; }
                                    :root[style] > body, :root > body { overflow: visible !important; }
                                </style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium/readium-css/ReadiumCSS-after.css"/>
                </head>
                    <BODY dir="ltr">
                        <p dir="rtl"></p>
                    </body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml" dir="rtl" xmlns:epub="http://www.idpf.org/2007/ops">
                        <head>
                            <title>Publication</title>
                            <link rel="stylesheet" href="style.css" type="text/css"/>
                        </head>
                        <BODY DIR='rtl'>
                            <p dir="rtl"></p>
                        </body>
                    </html>
                """.trimIndent()
            )
        )
    }
}
