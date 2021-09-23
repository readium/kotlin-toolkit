package org.readium.r2.streamer.fetcher

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.publication.*
import kotlin.test.assertEquals

class HtmlInjectorTest {

    @Test
    fun `Inject a reflowable with a simple HEAD`() {
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                    <head><meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=0" /><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>

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
            transform("""
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    </head>
                    <body></body>
                </html>
            """.trimIndent())
        )
    }

    @Test
    fun `Inject a reflowable with a HEAD with attributes`() {
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                    <head xmlns:xlink="http://www.w3.org/1999/xlink"><meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=0" /><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>

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
            transform("""
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                    <head xmlns:xlink="http://www.w3.org/1999/xlink">
                        <title>Publication</title>
                        <link rel="stylesheet" href="style.css" type="text/css"/>
                    </head>
                    <body></body>
                </html>
            """.trimIndent())
        )
    }

    @Test
    fun `Inject a reflowable with HEAD with attributes on a single line`() {
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><head xmlns:xlink="http://www.w3.org/1999/xlink"><meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=0" /><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <script type="text/javascript" src="/assets/scripts/readium-reflowable.js"></script>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
                <style type="text/css"> @font-face{font-family: "OpenDyslexic"; src:url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype');}</style>
                </head><body></body></html>
            """.trimIndent(),
            transform("""
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><head xmlns:xlink="http://www.w3.org/1999/xlink"><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/></head><body></body></html>
            """.trimIndent())
        )
    }

    @Test
    fun `Inject a reflowable with uppercase HEAD with attributes on several lines`() {
        assertEquals(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><HEAD
                 xmlns:xlink="http://www.w3.org/1999/xlink"
                 ><meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=0" /><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-before.css"/>
                <title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/><link rel="stylesheet" type="text/css" href="/assets/readium-css/ReadiumCSS-after.css"/>
                <script type="text/javascript" src="/assets/scripts/readium-reflowable.js"></script>
                <style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>
                <style type="text/css"> @font-face{font-family: "OpenDyslexic"; src:url("/assets/fonts/OpenDyslexic-Regular.otf") format('truetype');}</style>
                </HEAD><body></body></html>
            """.trimIndent(),
            transform("""
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><HEAD
                 xmlns:xlink="http://www.w3.org/1999/xlink"
                 ><title>Publication</title><link rel="stylesheet" href="style.css" type="text/css"/></HEAD><body></body></html>
            """.trimIndent())
        )
    }

    private fun transform(content: String): String = runBlocking {
        val sut = HtmlInjector(
            publication = Publication(manifest = Manifest(metadata = Metadata(localizedTitle = LocalizedString("")))),
            userPropertiesPath = null
        )

        val link = Link(href = "", type = "application/xhtml+xml")

        sut
            .transform(StringResource(link, content))
            .readAsString()
            .getOrThrow()
    }

}
