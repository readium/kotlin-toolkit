package org.readium.r2.navigator.epub.css

import org.junit.Test
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Either
import kotlin.test.assertEquals
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalReadiumApi::class)
class PropertiesTest {

    @Test
    fun `Convert empty RS properties to a dictionary of CSS properties`() {
        assertEquals(
            mapOf<String, String?>(
                "--RS__colWidth" to null,
                "--RS__colCount" to null,
                "--RS__colGap" to null,
                "--RS__pageGutter" to null,
                "--RS__flowSpacing" to null,
                "--RS__paraSpacing" to null,
                "--RS__paraIndent" to null,
                "--RS__maxLineLength" to null,
                "--RS__maxMediaWidth" to null,
                "--RS__maxMediaHeight" to null,
                "--RS__boxSizingMedia" to null,
                "--RS__boxSizingTable" to null,
                "--RS__textColor" to null,
                "--RS__backgroundColor" to null,
                "--RS__selectionTextColor" to null,
                "--RS__selectionBackgroundColor" to null,
                "--RS__linkColor" to null,
                "--RS__visitedColor" to null,
                "--RS__primaryColor" to null,
                "--RS__secondaryColor" to null,
                "--RS__typeScale" to null,
                "--RS__baseFontFamily" to null,
                "--RS__baseLineHeight" to null,
                "--RS__oldStyleTf" to null,
                "--RS__modernTf" to null,
                "--RS__sansTf" to null,
                "--RS__humanistTf" to null,
                "--RS__monospaceTf" to null,
                "--RS__serif-ja" to null,
                "--RS__sans-serif-ja" to null,
                "--RS__serif-ja-v" to null,
                "--RS__sans-serif-ja-v" to null,
                "--RS__compFontFamily" to null,
                "--RS__codeFontFamily" to null,
            ),
            RsProperties().toCssProperties()
        )
    }

    @Test
    fun `Override RS properties`() {
        val props = RsProperties(
            colCount = ColCount.ONE,
            overrides = mapOf(
                "--RS__colCount" to "2",
                "--RS__custom" to "value"
            )
        ).toCssProperties()

        assertEquals("2", props["--RS__colCount"])
        assertEquals("value", props["--RS__custom"])
    }

    @Test
    fun `Convert full RS properties to inline CSS`() {
        assertEquals(
            mapOf<String, String?>(
                "--RS__colWidth" to "1.2cm",
                "--RS__colCount" to "2",
                "--RS__colGap" to "2.3pt",
                "--RS__pageGutter" to "3.4pc",
                "--RS__flowSpacing" to "4.5mm",
                "--RS__paraSpacing" to "5.6px",
                "--RS__paraIndent" to "6.7em",
                "--RS__maxLineLength" to "7.8rem",
                "--RS__maxMediaWidth" to "50%",
                "--RS__maxMediaHeight" to "9.1vw",
                "--RS__boxSizingMedia" to "border-box",
                "--RS__boxSizingTable" to "content-box",
                "--RS__textColor" to "#432FCA",
                "--RS__backgroundColor" to "#FF0000",
                "--RS__selectionTextColor" to "rgb(100, 150, 200)",
                "--RS__selectionBackgroundColor" to "rgb(120, 230, 30)",
                "--RS__linkColor" to "#00FF00",
                "--RS__visitedColor" to "#0000FF",
                "--RS__primaryColor" to "#FA4358",
                "--RS__secondaryColor" to "#CBC322",
                "--RS__typeScale" to "10.11",
                "--RS__baseFontFamily" to """"Palatino", "Comic Sans MS"""",
                "--RS__baseLineHeight" to "11.12vh",
                "--RS__oldStyleTf" to """"Old", "Style"""",
                "--RS__modernTf" to """"Modern", "Tf"""",
                "--RS__sansTf" to """"Sans"""",
                "--RS__humanistTf" to """"Humanist"""",
                "--RS__monospaceTf" to """"Monospace"""",
                "--RS__serif-ja" to """"Serif", "Ja"""",
                "--RS__sans-serif-ja" to """"Sans serif", "Ja"""",
                "--RS__serif-ja-v" to """"Serif", "JaV"""",
                "--RS__sans-serif-ja-v" to """"Sans serif", "JaV"""",
                "--RS__compFontFamily" to """"Arial"""",
                "--RS__codeFontFamily" to """"Monaco", "Console Sans"""",
            ),
            RsProperties(
                colWidth = Length.Cm(1.2),
                colCount = ColCount.TWO,
                colGap = Length.Pt(2.3),
                pageGutter = Length.Pc(3.4),
                flowSpacing = Length.Mm(4.5),
                paraSpacing = Length.Px(5.6),
                paraIndent = Length.Em(6.7),
                maxLineLength = Length.Rem(7.8),
                maxMediaWidth = Length.Percent(0.5),
                maxMediaHeight = Length.Vw(9.10),
                boxSizingMedia = BoxSizing.BORDER_BOX,
                boxSizingTable = BoxSizing.CONTENT_BOX,
                textColor = Color.Hex("#432FCA"),
                backgroundColor = Color.Int(AndroidColor.RED),
                selectionTextColor = Color.Rgb(100, 150, 200),
                selectionBackgroundColor = Color.Rgb(120, 230, 30),
                linkColor = Color.Int(AndroidColor.GREEN),
                visitedColor = Color.Int(AndroidColor.BLUE),
                primaryColor = Color.Hex("#FA4358"),
                secondaryColor = Color.Hex("#CBC322"),
                typeScale = 10.11,
                baseFontFamily = listOf("Palatino", "Comic Sans MS"),
                baseLineHeight = Either(Length.Vh(11.12)),
                oldStyleTf = listOf("Old", "Style"),
                modernTf = listOf("Modern", "Tf"),
                sansTf = listOf("Sans"),
                humanistTf = listOf("Humanist"),
                monospaceTf = listOf("Monospace"),
                serifJa = listOf("Serif", "Ja"),
                sansSerifJa = listOf("Sans serif", "Ja"),
                serifJaV = listOf("Serif", "JaV"),
                sansSerifJaV = listOf("Sans serif", "JaV"),
                compFontFamily = listOf("Arial"),
                codeFontFamily = listOf("Monaco", "Console Sans")
            ).toCssProperties()
        )
    }

    @Test
    fun `Convert empty user properties to a dictionary of CSS properties`() {
        assertEquals(
            mapOf<String, String?>(
                "--USER__view" to null,
                "--USER__colCount" to null,
                "--USER__pageMargins" to null,
                "--USER__appearance" to null,
                "--USER__darkenImages" to null,
                "--USER__invertImages" to null,
                "--USER__textColor" to null,
                "--USER__backgroundColor" to null,
                "--USER__fontOverride" to null,
                "--USER__fontFamily" to null,
                "--USER__fontSize" to null,
                "--USER__advancedSettings" to null,
                "--USER__typeScale" to null,
                "--USER__textAlign" to null,
                "--USER__lineHeight" to null,
                "--USER__paraSpacing" to null,
                "--USER__paraIndent" to null,
                "--USER__wordSpacing" to null,
                "--USER__letterSpacing" to null,
                "--USER__bodyHyphens" to null,
                "--USER__ligatures" to null,
                "--USER__a11yNormalize" to null,
            ),
            UserProperties().toCssProperties()
        )
    }

    @Test
    fun `Convert full user properties to a dictionary of CSS properties`() {
        assertEquals(
            mapOf<String, String?>(
                "--USER__view" to "readium-scroll-on",
                "--USER__colCount" to "auto",
                "--USER__pageMargins" to "1.2",
                "--USER__appearance" to "readium-night-on",
                "--USER__darkenImages" to "readium-darken-on",
                "--USER__invertImages" to "readium-invert-on",
                "--USER__textColor" to "#FF0000",
                "--USER__backgroundColor" to "#00FF00",
                "--USER__fontOverride" to "readium-font-on",
                "--USER__fontFamily" to """"Times New"""",
                "--USER__fontSize" to "2.3vmax",
                "--USER__advancedSettings" to "readium-advanced-on",
                "--USER__typeScale" to "3.4",
                "--USER__textAlign" to "justify",
                "--USER__lineHeight" to "4.5pt",
                "--USER__paraSpacing" to "5.6pt",
                "--USER__paraIndent" to "6.7rem",
                "--USER__wordSpacing" to "7.8rem",
                "--USER__letterSpacing" to "8.9rem",
                "--USER__bodyHyphens" to "auto",
                "--USER__ligatures" to "common-ligatures",
                "--USER__a11yNormalize" to "readium-a11y-on",
            ),
            UserProperties(
                view = View.SCROLL,
                colCount = ColCount.AUTO,
                pageMargins = 1.2,
                appearance = Appearance.NIGHT,
                darkenImages = true,
                invertImages = true,
                textColor = Color.Int(AndroidColor.RED),
                backgroundColor = Color.Int(AndroidColor.GREEN),
                fontOverride = true,
                fontFamily = listOf("Times New"),
                fontSize = Length.VMax(2.3),
                advancedSettings = true,
                typeScale = 3.4,
                textAlign = TextAlign.JUSTIFY,
                lineHeight = Either(Length.Pt(4.5)),
                paraSpacing = Length.Pt(5.6),
                paraIndent = Length.Rem(6.7),
                wordSpacing = Length.Rem(7.8),
                letterSpacing = Length.Rem(8.9),
                bodyHyphens = Hyphens.AUTO,
                ligatures = Ligatures.COMMON,
                a11yNormalize = true,
            ).toCssProperties()
        )
    }

    @Test
    fun `Override user properties`() {
        val props = UserProperties(
            colCount = ColCount.ONE,
            overrides = mapOf(
                "--USER__colCount" to "2",
                "--USER__custom" to "value"
            )
        ).toCssProperties()

        assertEquals("2", props["--USER__colCount"])
        assertEquals("value", props["--USER__custom"])
    }

    @Test
    fun `Generate empty inline CSS properties`() {
        assertEquals(
            "",
            UserProperties().toCss()
        )
    }

    @Test
    fun `Generate minimal inline CSS properties`() {
        assertEquals(
            """
                --USER__view: readium-scroll-on;
                --USER__colCount: auto;
                
            """.trimIndent(),
            UserProperties(
                view = View.SCROLL,
                colCount = ColCount.AUTO,
            ).toCss()
        )
    }

    @Test
    fun `Generate full inline CSS properties`() {
        assertEquals(
            """
                --USER__view: readium-scroll-on;
                --USER__colCount: auto;
                --USER__pageMargins: 1.2;
                --USER__appearance: readium-night-on;
                --USER__darkenImages: readium-darken-on;
                --USER__invertImages: readium-invert-on;
                --USER__textColor: #FF0000;
                --USER__backgroundColor: #00FF00;
                --USER__fontOverride: readium-font-on;
                --USER__fontFamily: "Times New", "Comic Sans";
                --USER__fontSize: 2.3vmax;
                --USER__advancedSettings: readium-advanced-on;
                --USER__typeScale: 3.4;
                --USER__textAlign: justify;
                --USER__lineHeight: 4.5pt;
                --USER__paraSpacing: 5.6pt;
                --USER__paraIndent: 6.7rem;
                --USER__wordSpacing: 7.8rem;
                --USER__letterSpacing: 8.9rem;
                --USER__bodyHyphens: auto;
                --USER__ligatures: common-ligatures;
                --USER__a11yNormalize: readium-a11y-on;
                
            """.trimIndent(),
            UserProperties(
                view = View.SCROLL,
                colCount = ColCount.AUTO,
                pageMargins = 1.2,
                appearance = Appearance.NIGHT,
                darkenImages = true,
                invertImages = true,
                textColor = Color.Int(AndroidColor.RED),
                backgroundColor = Color.Int(AndroidColor.GREEN),
                fontOverride = true,
                fontFamily = listOf("Times New", "Comic Sans"),
                fontSize = Length.VMax(2.3),
                advancedSettings = true,
                typeScale = 3.4,
                textAlign = TextAlign.JUSTIFY,
                lineHeight = Either(Length.Pt(4.5)),
                paraSpacing = Length.Pt(5.6),
                paraIndent = Length.Rem(6.7),
                wordSpacing = Length.Rem(7.8),
                letterSpacing = Length.Rem(8.9),
                bodyHyphens = Hyphens.AUTO,
                ligatures = Ligatures.COMMON,
                a11yNormalize = true,
            ).toCss()
        )
    }
}