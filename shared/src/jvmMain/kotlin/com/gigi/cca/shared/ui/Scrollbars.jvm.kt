package com.gigi.cca.shared.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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

/** Finger drags arrive as mouse drags here, so follow them and fling on release. */
@Composable
actual fun Modifier.dragToScroll(state: ScrollableState): Modifier {
    val fling = ScrollableDefaults.flingBehavior()
    val dragState = rememberDraggableState { delta -> state.dispatchRawDelta(-delta) }
    return draggable(
        state = dragState,
        orientation = Orientation.Vertical,
        onDragStarted = { state.stopScroll() },
        onDragStopped = { velocity -> state.scroll { with(fling) { performFling(-velocity) } } },
    )
}

/** Follows the finger, then settles on the next or previous page if it was a flick, else the nearest. */
@Composable
actual fun Modifier.dragToSwipe(state: PagerState): Modifier {
    val startPage = remember { mutableIntStateOf(0) }
    val dragState = rememberDraggableState { delta -> state.dispatchRawDelta(-delta) }
    return draggable(
        state = dragState,
        orientation = Orientation.Horizontal,
        onDragStarted = {
            state.stopScroll()
            startPage.intValue = state.currentPage
        },
        onDragStopped = { velocity ->
            val target = when {
                velocity < -FLICK_VELOCITY -> startPage.intValue + 1
                velocity > FLICK_VELOCITY -> startPage.intValue - 1
                else -> state.currentPage
            }
            state.animateScrollToPage(target.coerceIn(0, state.pageCount - 1))
        },
    )
}

private const val FLICK_VELOCITY = 400f
