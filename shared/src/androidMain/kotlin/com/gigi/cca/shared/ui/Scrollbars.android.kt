package com.gigi.cca.shared.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun ListScrollbar(state: LazyListState, modifier: Modifier) = Unit

@Composable
actual fun ColumnScrollbar(state: ScrollState, modifier: Modifier) = Unit

// Touch already reaches this platform as touch, which lists scroll with natively.
@Composable
actual fun Modifier.dragToScroll(state: ScrollableState): Modifier = this

@Composable
actual fun Modifier.dragToSwipe(state: PagerState): Modifier = this
