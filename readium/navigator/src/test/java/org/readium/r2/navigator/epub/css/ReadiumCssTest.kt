package org.readium.r2.navigator.epub.css

import org.junit.Test
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalReadiumApi::class)
class HtmlInjectionTest {

    @Test
    fun `Inject with a simple HEAD`() {
        val sut = ReadiumCss(
            layout = Layout(
                language = null,
                stylesheets = Layout.Stylesheets.Default,
                readingProgression = ReadingProgression.LTR
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
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
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head xmlns:xlink="http://www.w3.org/1999/xlink">
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
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
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?><html dir="ltr" xmlns="http://www.w3.org/1999/xhtml"><head xmlns:xlink="http://www.w3.org/1999/xlink">
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                <title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
                </head><body dir="ltr" xmlns:xlink="http://www.w3.org/1999/xlink"></body></html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head xmlns:xlink="http://www.w3.org/1999/xlink"><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/></head><body xmlns:xlink="http://www.w3.org/1999/xlink"></body></html>
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?><HTML dir="ltr" xmlns="http://www.w3.org/1999/xhtml"><HEAD xmlns:xlink="http://www.w3.org/1999/xlink">
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                <title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
                </HEAD><BODY dir="ltr" xmlns:xlink="http://www.w3.org/1999/xlink"></BODY></HTML>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?><HTML xmlns="http://www.w3.org/1999/xhtml"><HEAD xmlns:xlink="http://www.w3.org/1999/xlink"><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/></HEAD><BODY xmlns:xlink="http://www.w3.org/1999/xlink"></BODY></HTML>
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-default.css"/>
                
                        <title>Publication</title>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
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
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
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
                """.trimIndent(),
                baseHref = "/assets/"
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
                """.trimIndent(),
                baseHref = "/assets/"
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
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="rtl" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/rtl/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/rtl/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
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
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/cjk-horizontal/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/cjk-horizontal/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
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
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/cjk-vertical/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/cjk-vertical/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
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
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html xml:lang="fr-CA" dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
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
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html xml:lang="en-US" dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
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
                """.trimIndent(),
                baseHref = "/assets/"
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
            )
        )
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <html xml:lang="en-US" dir="ltr" xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>audio[controls] { width: revert; height: revert; }</style>
                
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    
                <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <style type="text/css">@font-face { font-family: "OpenDyslexic"; src: url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype'); }</style>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
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
                """.trimIndent(),
                baseHref = "/assets/"
            )
        )
    }
}