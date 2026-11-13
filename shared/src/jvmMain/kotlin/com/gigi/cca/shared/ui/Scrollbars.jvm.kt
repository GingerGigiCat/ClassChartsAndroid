package com.gigi.cca.shared.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The default scrollbar is translucent black, which disappears on the dark theme. */
@Composable
private fun themedStyle(): ScrollbarStyle = LocalScrollbarStyle.current.copy(
    thickness = 8.dp,
    shape = RoundedCornerShape(4.dp),
    unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
    hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
)

@Composable
actual fun ListScrollbar(state: LazyListState, modifier: Modifier) =
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier.padding(end = 3.dp, top = 4.dp, bottom = 4.dp), style = themedStyle())

@Composable
actual fun ColumnScrollbar(state: ScrollState, modifier: Modifier) =
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier.padding(end = 3.dp, top = 4.dp, bottom = 4.dp), style = themedStyle())

/** Finger drags arrive as mouse drags here, so scroll with them (see DragScroll.kt). */
@Composable
actual fun Modifier.dragToScroll(state: ScrollableState): Modifier = mouseDragScroll(state)

@Composable
actual fun Modifier.dragToSwipe(state: PagerState): Modifier = mouseDragSwipe(state)
