package com.gigi.cca.shared

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Tiny persistent key/value store used for the offline cache. */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

/**
 * How requests reach ClassCharts. Android and desktop talk to it directly. Browsers can't
 * (no CORS, and the login cookies are HttpOnly), so the web build goes through web-proxy/,
 * which passes cookies and redirect statuses back in X-CC-* headers.
 */
class NetConfig(val root: String, val viaProxy: Boolean) {
    val cookieHeader: String get() = if (viaProxy) "X-CC-Cookie" else "Cookie"
    val setCookieHeader: String get() = if (viaProxy) "X-CC-Set-Cookie" else "Set-Cookie"

    companion object {
        val Direct = NetConfig("https://www.classcharts.com", viaProxy = false)
    }
}

expect fun createStore(context: Any?): KeyValueStore

expect fun netConfig(): NetConfig

/** Platform colour scheme (Material You on Android 12+), or null to use the app's own. */
@Composable
expect fun platformColorScheme(dark: Boolean): ColorScheme?

/**
 * True when the main input is a finger (Android, phones in a browser). Pull-to-refresh is only
 * used then, because a mouse wheel overscroll triggers it without ever "letting go".
 */
expect fun usesTouchInput(): Boolean

/**
 * True on the desktop app, where touchscreens arrive as mouse drags. Text selection is turned off
 * there so dragging over long homework descriptions scrolls instead of selecting.
 */
expect val isDesktopApp: Boolean
