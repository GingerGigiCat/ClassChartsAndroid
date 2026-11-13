package com.gigi.cca.shared

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

private fun storageGet(key: String): String? =
    js("{ try { return window.localStorage.getItem(key); } catch (e) { return null; } }")

private fun storageSet(key: String, value: String): Unit =
    js("{ try { window.localStorage.setItem(key, value); } catch (e) {} }")

private fun storageRemove(key: String): Unit =
    js("{ try { window.localStorage.removeItem(key); } catch (e) {} }")

private fun pageOrigin(): String = js("window.location.origin")

private fun proxyParameter(): String? = js("new URLSearchParams(window.location.search).get('proxy')")

private class LocalStorageStore : KeyValueStore {
    override fun get(key: String): String? = storageGet("cca.$key")
    override fun put(key: String, value: String) = storageSet("cca.$key", value)
    override fun remove(key: String) = storageRemove("cca.$key")
}

actual fun createStore(context: Any?): KeyValueStore = LocalStorageStore()

/** Same origin as the page by default (web-proxy/ serves both), or ?proxy=http://host:port. */
actual fun usesTouchInput(): Boolean = coarsePointer()

private fun coarsePointer(): Boolean = js("window.matchMedia('(pointer: coarse)').matches")

actual val isDesktopApp: Boolean = false

actual fun netConfig(): NetConfig =
    NetConfig("${proxyParameter()?.trimEnd('/') ?: pageOrigin()}/cc", viaProxy = true)

@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme? = null
