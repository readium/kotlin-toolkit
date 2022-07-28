package org.readium.r2.navigator.epub.css

import org.junit.Assert
import org.junit.Test
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

@OptIn(ExperimentalReadiumApi::class)
class HtmlInjectionTest {

    @Test
    fun `Inject a reflowable with a simple HEAD`() {
        val sut = ReadiumCss(
            layout = Layout(stylesheets = Layout.Stylesheets.Default, readingProgression = ReadingProgression.LTR)
        )
        Assert.assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                    <head><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>
                audio[controls] {
                    width: revert;
                    height: revert;
                }
                </style>
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <script type="text/javascript" src="/assets/scripts/readium-reflowable.js"></script>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
                <style type="text/css"> @font-face{font-family: "OpenDyslexic"; src:url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype');}</style>
                </head>
                    <body></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
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
    fun `Inject a reflowable with a HEAD with attributes`() {
        val sut = ReadiumCss(
            layout = Layout(stylesheets = Layout.Stylesheets.Default, readingProgression = ReadingProgression.LTR)
        )
        Assert.assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                    <head xmlns:xlink="http://www.w3.org/1999/xlink"><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>
                audio[controls] {
                    width: revert;
                    height: revert;
                }
                </style>
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    <link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <script type="text/javascript" src="/assets/scripts/readium-reflowable.js"></script>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
                <style type="text/css"> @font-face{font-family: "OpenDyslexic"; src:url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype');}</style>
                </head>
                    <body></body>
                </html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                    <html xmlns="http://www.w3.org/1999/xhtml">
                        <head xmlns:xlink="http://www.w3.org/1999/xlink">
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
    fun `Inject a reflowable with HEAD with attributes on a single line`() {
        val sut = ReadiumCss(
            layout = Layout(stylesheets = Layout.Stylesheets.Default, readingProgression = ReadingProgression.LTR)
        )
        Assert.assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><head xmlns:xlink="http://www.w3.org/1999/xlink"><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>
                audio[controls] {
                    width: revert;
                    height: revert;
                }
                </style><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <script type="text/javascript" src="/assets/scripts/readium-reflowable.js"></script>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
                <style type="text/css"> @font-face{font-family: "OpenDyslexic"; src:url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype');}</style>
                </head><body></body></html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><head xmlns:xlink="http://www.w3.org/1999/xlink"><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/></head><body></body></html>
                """.trimIndent()
            )
        )
    }

    @Test
    fun `Inject a reflowable with uppercase HEAD with attributes on several lines`() {
        val sut = ReadiumCss(
            layout = Layout(stylesheets = Layout.Stylesheets.Default, readingProgression = ReadingProgression.LTR)
        )

        Assert.assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><HEAD
                 xmlns:xlink="http://www.w3.org/1999/xlink"
                 ><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <style>
                audio[controls] {
                    width: revert;
                    height: revert;
                }
                </style><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <script type="text/javascript" src="/assets/scripts/readium-reflowable.js"></script>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
                <style type="text/css"> @font-face{font-family: "OpenDyslexic"; src:url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype');}</style>
                </HEAD><body></body></html>
            """.trimIndent(),
            sut.injectHtml(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><HEAD
                     xmlns:xlink="http://www.w3.org/1999/xlink"
                     ><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/></HEAD><body></body></html>
                """.trimIndent()
            )
        )
    }
}