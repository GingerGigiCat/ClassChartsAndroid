package com.gigi.cca.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import be.digitalia.compose.htmlconverter.HtmlStyle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** WCAG AA contrast for normal text. */
private const val MIN_CONTRAST = 4.5f

/**
 * A teacher's HTML description as styled text, keeping their colours and highlights but fixing
 * any that would be hard to read on the current theme (like black text in dark mode).
 */
@Composable
fun rememberHomeworkBody(html: String): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    val link = colors.primary
    val background = colors.surface
    val text = colors.onSurface
    return remember(html, link, background, text) {
        htmlToAnnotatedString(
            normalizeTeacherHtml(html),
            style = HtmlStyle(
                textLinkStyles = TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline)),
                isTextColorEnabled = true,
            ),
        ).withReadableColors(background, text)
    }
}

private val FONT_OPEN = Regex("<font\\b([^>]*)>", RegexOption.IGNORE_CASE)
private val FONT_CLOSE = Regex("</font\\s*>", RegexOption.IGNORE_CASE)
private val FONT_COLOR = Regex("color\\s*=\\s*[\"']?([^\"'\\s>]+)", RegexOption.IGNORE_CASE)

private val STYLE_ATTRIBUTE = Regex("style\\s*=\\s*(\"[^\"]*\"|'[^']*')", RegexOption.IGNORE_CASE)
private val RGB = Regex(
    "rgba?\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*(?:,\\s*([\\d.]+)(%?)\\s*)?\\)",
    RegexOption.IGNORE_CASE,
)

/**
 * The converter only understands CSS colours written as #hex or names. Teachers' editors also
 * produce `rgb(…)` colours and old `<font color=…>` tags, so rewrite those into a form it reads.
 */
internal fun normalizeTeacherHtml(html: String): String {
    var result = html
    if (result.contains("<font", ignoreCase = true)) {
        result = result
            .replace(FONT_OPEN) { match ->
                val color = FONT_COLOR.find(match.groupValues[1])?.groupValues?.get(1)
                if (color != null) "<span style=\"color: $color\">" else "<span>"
            }
            .replace(FONT_CLOSE, "</span>")
    }
    if (result.contains("rgb", ignoreCase = true)) {
        result = result.replace(STYLE_ATTRIBUTE) { style -> style.value.replace(RGB) { rgbToHex(it) } }
    }
    return result
}

private fun rgbToHex(match: MatchResult): String {
    fun hex(value: Int) = value.coerceIn(0, 255).toString(16).padStart(2, '0')
    val (r, g, b) = match.destructured.toList().take(3).map { it.toInt() }
    val alphaText = match.groupValues[4]
    if (alphaText.isEmpty()) return "#${hex(r)}${hex(g)}${hex(b)}"
    val alpha = alphaText.toFloatOrNull()?.let { if (match.groupValues[5] == "%") it / 100f else it } ?: 1f
    return "#${hex(r)}${hex(g)}${hex(b)}${hex((alpha.coerceIn(0f, 1f) * 255).roundToInt())}"
}

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

/** Black, white and greys, where the teacher most likely just meant "normal text". */
private fun Color.isNeutral(): Boolean = max(red, max(green, blue)) - min(red, min(green, blue)) < 0.12f

/**
 * A colour readable on [back]: the teacher's colour if it already is, otherwise the same hue
 * moved just far enough towards white or black. Neutral colours become [defaultText].
 */
internal fun readableColor(teacher: Color?, defaultText: Color, back: Color): Color? {
    val wanted = teacher ?: defaultText
    if (contrast(wanted, back) >= MIN_CONTRAST) return teacher
    if (teacher == null || teacher.isNeutral()) {
        if (contrast(defaultText, back) >= MIN_CONTRAST) return defaultText
        return if (contrast(Color.Black, back) >= contrast(Color.White, back)) Color.Black else Color.White
    }
    val towards = if (back.luminance() < 0.5f) Color.White else Color.Black
    var fraction = 0.1f
    while (fraction < 1f) {
        val candidate = lerp(teacher, towards, fraction)
        if (contrast(candidate, back) >= MIN_CONTRAST) return candidate
        fraction += 0.1f
    }
    return towards
}

/**
 * Splits the text wherever a style starts or ends, works out the colour and highlight each
 * piece ends up with, and adds a corrected colour on top where it would be hard to read.
 * Everything else the teacher set (bold, lists, highlights, links) is left as it was.
 */
internal fun AnnotatedString.withReadableColors(surface: Color, defaultText: Color): AnnotatedString {
    val styled = spanStyles.filter { it.item.color.isSpecified || it.item.background.isSpecified }
    if (styled.isEmpty()) return this
    val cuts = (styled.flatMap { listOf(it.start, it.end) } + 0 + text.length).distinct().sorted()
    val fixes = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    for ((start, end) in cuts.zipWithNext()) {
        if (start >= end) continue
        var teacher: Color? = null
        var highlight: Color? = null
        // Inner tags come later in the list, so the last match is what's actually shown.
        for (range in styled) {
            if (range.start <= start && range.end >= end) {
                if (range.item.color.isSpecified) teacher = range.item.color
                if (range.item.background.isSpecified) highlight = range.item.background
            }
        }
        val back = highlight?.compositeOver(surface) ?: surface
        val shown = teacher?.compositeOver(back)
        val fixed = readableColor(shown, defaultText, back) ?: continue
        if (fixed != shown) fixes += AnnotatedString.Range(SpanStyle(color = fixed), start, end)
    }
    if (fixes.isEmpty()) return this
    return AnnotatedString.Builder(this).apply {
        // Added last, so these win over the teacher's colour for the same text.
        fixes.forEach { addStyle(it.item, it.start, it.end) }
    }.toAnnotatedString()
}
