package com.gigi.cca.shared.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private fun Offset.along(orientation: Orientation) = if (orientation == Orientation.Vertical) y else x
private fun Offset.across(orientation: Orientation) = if (orientation == Orientation.Vertical) x else y
private fun Velocity.along(orientation: Orientation) = if (orientation == Orientation.Vertical) y else x

/**
 * Turns mouse drags (which is how touchscreens reach the desktop app) into scrolling.
 *
 * It watches the pointer before the list items do (the Initial pass), so the list moves straight
 * away and, once the movement is a drag, the item under the finger never gets a click. Pressing
 * on a list that's still flinging stops it without clicking, like on a phone. Nested lists in
 * different directions (timetable days inside the day pager) each only claim drags in their own
 * direction.
 */
private fun Modifier.mouseDrag(
    key: Any,
    orientation: Orientation,
    isMoving: () -> Boolean,
    onStart: () -> Unit,
    onDelta: (Float) -> Unit,
    onRelease: (velocity: Float) -> Unit,
): Modifier = pointerInput(key) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.type != PointerType.Mouse || !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
        if (isMoving()) {
            down.consume()
            onStart()
        }
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var dragging = false
        var travelled = Offset.Zero
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
            if (!change.pressed) {
                if (dragging) {
                    change.consume()
                    onRelease(tracker.calculateVelocity().along(orientation))
                }
                return@awaitEachGesture
            }
            // An outer list in the other direction already took this drag.
            if (!dragging && change.isConsumed) return@awaitEachGesture
            tracker.addPosition(change.uptimeMillis, change.position)
            val delta = change.position - change.previousPosition
            if (dragging) {
                onDelta(delta.along(orientation))
                change.consume()
                continue
            }
            travelled += delta
            val main = travelled.along(orientation)
            if (abs(main) > slop && abs(main) > abs(travelled.across(orientation))) {
                dragging = true
                onStart()
                onDelta(main - slop * sign(main))
                change.consume()
            } else {
                // Give inner lists the first go; if one claimed the drag, stop watching.
                val final = awaitPointerEvent(PointerEventPass.Final)
                if (final.changes.any { it.id == down.id && it.isConsumed }) return@awaitEachGesture
            }
        }
    }
}

/** Mouse/touchscreen drag scrolling for a list or scrolling column, with a fling on release. */
@Composable
internal fun Modifier.mouseDragScroll(state: ScrollableState): Modifier {
    val fling = ScrollableDefaults.flingBehavior()
    val scope = rememberCoroutineScope()
    return mouseDrag(
        key = state,
        orientation = Orientation.Vertical,
        isMoving = { state.isScrollInProgress },
        onStart = { scope.launch { state.stopScroll() } },
        onDelta = { state.dispatchRawDelta(-it) },
        onRelease = { velocity -> scope.launch { state.scroll { with(fling) { performFling(-velocity) } } } },
    )
}

/** Mouse/touchscreen swiping between pages: a flick moves one page, a slow drag settles on the nearest. */
@Composable
internal fun Modifier.mouseDragSwipe(state: PagerState): Modifier {
    val scope = rememberCoroutineScope()
    val startPage = remember { intArrayOf(0) }
    return mouseDrag(
        key = state,
        orientation = Orientation.Horizontal,
        isMoving = { state.isScrollInProgress },
        onStart = {
            startPage[0] = state.currentPage
            scope.launch { state.stopScroll() }
        },
        onDelta = { state.dispatchRawDelta(-it) },
        onRelease = { velocity ->
            val target = when {
                velocity < -FLICK_VELOCITY -> startPage[0] + 1
                velocity > FLICK_VELOCITY -> startPage[0] - 1
                else -> state.currentPage
            }
            scope.launch { state.animateScrollToPage(target.coerceIn(0, state.pageCount - 1)) }
        },
    )
}

private const val FLICK_VELOCITY = 400f
