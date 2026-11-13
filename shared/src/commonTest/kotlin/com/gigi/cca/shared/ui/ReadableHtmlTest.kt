package com.gigi.cca.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReadableHtmlTest {
    private val darkSurface = Color(0xFF141218)
    private val darkText = Color(0xFFE6E0E9)
    private val lightSurface = Color(0xFFFEF7FF)
    private val lightText = Color(0xFF1D1B20)

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    @Test
    fun blackTextInDarkModeBecomesNormalText() {
        assertEquals(darkText, readableColor(Color.Black, darkText, darkSurface))
    }

    @Test
    fun blackTextInLightModeIsLeftAlone() {
        assertNull(readableColor(null, lightText, lightSurface))
        assertEquals(Color.Black, readableColor(Color.Black, lightText, lightSurface))
    }

    @Test
    fun darkRedInDarkModeStaysRedButReadable() {
        val red = Color(0xFF8B0000)
        val fixed = readableColor(red, darkText, darkSurface)!!
        assertTrue(contrast(fixed, darkSurface) >= 4.5f)
        assertTrue(fixed.red > fixed.green + 0.2f && fixed.red > fixed.blue + 0.2f, "should still look red: $fixed")
    }

    @Test
    fun lightTextOnPaleHighlightTurnsDark() {
        val yellow = Color(0xFFFBEEB8)
        val fixed = readableColor(null, darkText, yellow)!!
        assertTrue(contrast(fixed, yellow) >= 4.5f)
    }

    @Test
    fun readableTextIsReturnedUnchanged() {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFF00695C))) { append("dark teal is fine on white") }
        }
        assertSame(text, text.withReadableColors(Color.White, lightText))
    }

    @Test
    fun blackSpanInDarkModeGetsFixedColour() {
        val text = buildAnnotatedString {
            append("normal ")
            withStyle(SpanStyle(color = Color.Black)) { append("black") }
        }
        val fixed = text.withReadableColors(darkSurface, darkText)
        assertTrue(fixed.spanStyles.any { it.item.color == darkText && it.start == 7 && it.end == 12 })
    }

    @Test
    fun fontTagsBecomeColouredSpans() {
        assertEquals(
            "<span style=\"color: #003366\">hi</span> <span>plain</span>",
            normalizeTeacherHtml("<FONT color=\"#003366\">hi</font> <font face=\"Arial\">plain</font>"),
        )
    }

    @Test
    fun rgbColoursBecomeHex() {
        assertEquals(
            "<span style=\"color: #e03e2d; background-color: #fbeeb880;\">x</span> rgb(1, 2, 3)",
            normalizeTeacherHtml("<span style=\"color: rgb(224, 62, 45); background-color: rgba(251,238,184,0.5);\">x</span> rgb(1, 2, 3)"),
        )
    }
}
