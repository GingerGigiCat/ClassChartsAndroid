package com.gigi.cca.shared.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Draggable scrollbar on desktop and web. Android has no scrollbars, so it draws nothing. */
@Composable
expect fun ListScrollbar(state: LazyListState, modifier: Modifier = Modifier)

@Composable
expect fun ColumnScrollbar(state: ScrollState, modifier: Modifier = Modifier)

/**
 * Lets dragging scroll a list on desktop. Touchscreens on Windows and Linux reach the desktop
 * app as mouse drags, and Compose only scrolls with the wheel for mice, so without this finger
 * scrolling does nothing. Elsewhere touch already works, so it does nothing.
 */
@Composable
expect fun Modifier.dragToScroll(state: ScrollableState): Modifier

/** The same for swiping between pages (timetable days). */
@Composable
expect fun Modifier.dragToSwipe(state: PagerState): Modifier
