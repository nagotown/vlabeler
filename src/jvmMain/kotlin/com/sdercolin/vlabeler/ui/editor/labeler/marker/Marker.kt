@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.editor.Tool
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.EndPointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.NonePointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.StartPointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Mouse
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.FloatRange
import com.sdercolin.vlabeler.util.clear
import com.sdercolin.vlabeler.util.contains
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull
import com.sdercolin.vlabeler.util.getScreenRange
import com.sdercolin.vlabeler.util.length
import com.sdercolin.vlabeler.util.parseColor
import com.sdercolin.vlabeler.util.requireValue
import com.sdercolin.vlabeler.util.update
import com.sdercolin.vlabeler.util.updateNonNull
import kotlin.math.abs
import kotlin.math.min

private const val RegionAlpha = 0.3f
private val EditableOutsideRegionColor = White
private const val UneditableRegionAlpha = 0.9f
private val UneditableRegionColor = Black
private const val IdleLineAlpha = 0.7f
private const val StrokeWidth = 2f
private val LabelSize = DpSize(40.dp, 25.dp)
private val LabelShiftUp = 8.dp

@Composable
fun MarkerCanvas(
    canvasParams: CanvasParams,
    horizontalScrollState: ScrollState,
    editorState: EditorState,
    appState: AppState,
    state: MarkerState
) {
    FieldBorderCanvas(editorState, appState, state, horizontalScrollState)
    LaunchAdjustScrollPosition(
        state.entriesInPixel,
        editorState.project.currentIndex,
        canvasParams.lengthInPixel,
        horizontalScrollState,
        appState.scrollFitViewModel
    )
    LaunchedEffect(editorState.tool) {
        if (editorState.tool == Tool.Scissors) {
            state.scissorsState.update { MarkerScissorsState() }
            state.cursorState.update { MarkerCursorState() }
        } else {
            state.scissorsState.clear()
        }
    }
}

@Composable
fun MarkerLabels(
    appState: AppState,
    state: MarkerState
) {
    val requestRename: (Int) -> Unit = remember(appState) {
        { appState.openEditEntryNameDialog(it, InputEntryNameDialogPurpose.Rename) }
    }

    // Content are put in the layout no matter it's width
    // So just give a safe width
    val width = state.canvasParams.lengthInPixel.coerceAtMost(CanvasParams.MaxCanvasLengthInPixel)
    val widthDp = with(LocalDensity.current) {
        width.toDp()
    }

    FieldLabels(state, widthDp)
    if (state.labelerConf.continuous) {
        NameLabels(state, requestRename, widthDp)
    }
}

@Composable
private fun FieldBorderCanvas(
    editorState: EditorState,
    appState: AppState,
    state: MarkerState,
    horizontalScrollState: ScrollState
) {
    val screenRange = horizontalScrollState.getScreenRange(state.canvasParams.lengthInPixel)
    val keyboardState by appState.keyboardViewModel.keyboardStateFlow.collectAsState()
    val tool = editorState.tool

    LaunchedEffect(keyboardState.isCtrlPressed, tool) {
        if (tool == Tool.Scissors) {
            state.scissorsState.updateNonNull { copy(disabled = keyboardState.isCtrlPressed) }
        }
    }

    Canvas(Modifier.fillMaxSize()
        .onPointerEvent(PointerEventType.Move) { event ->
            if (editorState.tool == Tool.Cursor &&
                state.cursorState.value.mouse == Mouse.Dragging &&
                event.buttons.areAnyPressed.not()
            ) {
                state.handleMouseRelease(
                    tool,
                    event,
                    editorState::submitEntries,
                    appState.player::playSection,
                    editorState::cutEntry,
                    keyboardState,
                    screenRange
                )
                return@onPointerEvent
            }
            state.handleMouseMove(tool, event, editorState::updateEntries, screenRange)
        }
        .onPointerEvent(PointerEventType.Press) {
            state.cursorState.handleMousePress(tool, keyboardState, state.labelerConf)
        }
        .onPointerEvent(PointerEventType.Release) { event ->
            state.handleMouseRelease(
                tool,
                event,
                editorState::submitEntries,
                appState.player::playSection,
                editorState::cutEntry,
                keyboardState,
                screenRange
            )
        }
    ) {
        screenRange ?: return@Canvas
        try {
            val entriesInPixel = state.entriesInPixel
            val start = entriesInPixel.first().start
            val end = entriesInPixel.last().end
            val canvasActualWidth = state.canvasParams.lengthInPixel.toFloat()
            val canvasHeight = size.height
            val leftBorder = state.leftBorder
            val rightBorder = state.rightBorder
            val cursorState = state.cursorState
            val labelerConf = state.labelerConf
            state.canvasHeightState.value = canvasHeight

            // Draw left border
            if (leftBorder >= 0 && (0f..leftBorder in screenRange)) {
                val leftBorderColor = UneditableRegionColor
                val relativeLeftBorder = leftBorder - screenRange.start
                drawRect(
                    color = leftBorderColor,
                    alpha = UneditableRegionAlpha,
                    topLeft = Offset.Zero,
                    size = Size(width = relativeLeftBorder, height = canvasHeight)
                )
                drawLine(
                    color = leftBorderColor.copy(alpha = IdleLineAlpha),
                    start = Offset(relativeLeftBorder, 0f),
                    end = Offset(relativeLeftBorder, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw start
            if (leftBorder..start in screenRange) {
                val startColor = EditableOutsideRegionColor
                val relativeLeftBorder = leftBorder - screenRange.start
                val relativeStart = start - screenRange.start
                val coercedLeftBorder = relativeLeftBorder.coerceAtLeast(0f)
                val coercedStart = relativeStart.coerceAtMost(screenRange.length)
                drawRect(
                    color = startColor,
                    alpha = RegionAlpha,
                    topLeft = Offset(coercedLeftBorder, 0f),
                    size = Size(width = coercedStart - coercedLeftBorder, height = canvasHeight)
                )
                val startLineAlpha = if (cursorState.value.usingStartPoint) 1f else IdleLineAlpha
                drawLine(
                    color = startColor.copy(alpha = startLineAlpha),
                    start = Offset(coercedStart, 0f),
                    end = Offset(coercedStart, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw custom fields and borders
            for (entryIndex in state.entriesInPixel.indices) {
                val entryInPixel = state.entriesInPixel[entryIndex]
                if (entryIndex != 0) {
                    val border = state.entryBorders[entryIndex - 1]
                    val borderColor = EditableOutsideRegionColor
                    val pointIndex = cursorState.value.pointIndex
                    val borderLineAlpha =
                        if (state.isBorderIndex(pointIndex) &&
                            state.getEntryIndexesByBorderIndex(pointIndex).second == entryIndex
                        ) 1f else IdleLineAlpha
                    if (border in screenRange) {
                        val relativeBorder = border - screenRange.start
                        drawLine(
                            color = borderColor.copy(alpha = borderLineAlpha),
                            start = Offset(relativeBorder, 0f),
                            end = Offset(relativeBorder, canvasHeight),
                            strokeWidth = StrokeWidth
                        )
                    }
                }
                for (fieldIndex in labelerConf.fields.indices) {
                    val field = labelerConf.fields[fieldIndex]
                    val x = entryInPixel.points[fieldIndex]
                    val waveformsHeight = canvasHeight * state.waveformsHeightRatio
                    val height = waveformsHeight * field.height
                    val top = waveformsHeight - height
                    val color = parseColor(field.color)
                    val fillTargetIndex = when (field.filling) {
                        "start" -> StartPointIndex
                        "end" -> EndPointIndex
                        null -> null
                        else -> labelerConf.fields.withIndex().find { it.value.name == field.filling }?.index
                    }
                    if (fillTargetIndex != null) {
                        val targetX = entryInPixel.getPoint(fillTargetIndex)
                        val left = min(targetX, x)
                        val width = abs(targetX - x)
                        val right = left + width
                        if (left..right in screenRange) {
                            val coercedRelativeLeft = (left - screenRange.start).coerceAtLeast(0f)
                            val coercedRelativeRight = (right - screenRange.start).coerceAtMost(screenRange.length)
                            drawRect(
                                color = color,
                                alpha = RegionAlpha,
                                topLeft = Offset(coercedRelativeLeft, top),
                                size = Size(width = coercedRelativeRight - coercedRelativeLeft, height = height)
                            )
                        }
                    }

                    val pointIndex = fieldIndex + entryIndex * (state.labelerConf.fields.size + 1)
                    val lineAlpha = if (cursorState.value.pointIndex != pointIndex) IdleLineAlpha else 1f
                    if (x in screenRange) {
                        val relativeX = x - screenRange.start
                        drawLine(
                            color = color.copy(alpha = lineAlpha),
                            start = Offset(relativeX, top),
                            end = Offset(relativeX, canvasHeight),
                            strokeWidth = StrokeWidth
                        )
                    }
                }
            }

            // Draw end
            if (end..rightBorder in screenRange) {
                val endColor = EditableOutsideRegionColor
                val relativeEnd = end - screenRange.start
                val relativeRightBorder = rightBorder - screenRange.start
                val coercedEnd = relativeEnd.coerceAtLeast(0f)
                val coercedRightBorder = relativeRightBorder.coerceAtMost(screenRange.length)
                drawRect(
                    color = endColor,
                    alpha = RegionAlpha,
                    topLeft = Offset(coercedEnd, 0f),
                    size = Size(width = coercedRightBorder - coercedEnd, height = canvasHeight)
                )
                val endLineAlpha = if (cursorState.value.usingEndPoint) 1f else IdleLineAlpha
                drawLine(
                    color = endColor.copy(alpha = endLineAlpha),
                    start = Offset(coercedEnd, 0f),
                    end = Offset(coercedEnd, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw right border
            if (rightBorder < canvasActualWidth && (rightBorder..canvasActualWidth in screenRange)) {
                val rightBorderColor = UneditableRegionColor
                val relativeRightBorder = rightBorder - screenRange.start
                drawRect(
                    color = rightBorderColor,
                    alpha = UneditableRegionAlpha,
                    topLeft = Offset(relativeRightBorder, 0f),
                    size = Size(width = screenRange.endInclusive - relativeRightBorder, height = canvasHeight)
                )
                drawLine(
                    color = rightBorderColor.copy(alpha = IdleLineAlpha),
                    start = Offset(relativeRightBorder, 0f),
                    end = Offset(relativeRightBorder, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw scissors
            state.scissorsState.value?.let { scissors ->
                val position = scissors.position
                if (scissors.disabled.not() && position != null && state.isValidCutPosition(position)) {
                    if (position in screenRange) {
                        val relativePosition = position - screenRange.start
                        drawLine(
                            color = parseColor(appState.appConf.editor.scissorsColor),
                            start = Offset(relativePosition, 0f),
                            end = Offset(relativePosition, canvasHeight),
                            strokeWidth = StrokeWidth * 2
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            if (isDebug) throw t
            Log.debug(t)
        }
    }
}

@Composable
private fun FieldLabels(state: MarkerState, widthDp: Dp) {
    val labelIndexes = state.entriesInPixel.indices.flatMap { entryIndex ->
        state.labelerConf.fields.indices.map { fieldIndex ->
            entryIndex to fieldIndex
        }
    }

    FieldLabelsContent(
        modifier = Modifier.fillMaxHeight().width(widthDp),
        waveformsHeightRatio = state.waveformsHeightRatio,
        fields = state.labelerConf.fields,
        entries = state.entriesInPixel,
        labelIndexes = labelIndexes,
        state = state
    )
}

@Composable
private fun FieldLabelsContent(
    modifier: Modifier,
    waveformsHeightRatio: Float,
    fields: List<LabelerConf.Field>,
    entries: List<EntryInPixel>,
    labelIndexes: List<Pair<Int, Int>>,
    state: MarkerState,
) {
    val labelShiftUp = with(LocalDensity.current) { LabelShiftUp.toPx() }
    Layout(
        modifier = modifier,
        content = {
            labelIndexes.forEach { (entryIndex, fieldIndex) ->
                val field = state.labelerConf.fields[fieldIndex]
                val pointIndex = fieldIndex + entryIndex * (state.labelerConf.fields.size + 1)
                val alpha = if (state.cursorState.value.pointIndex != pointIndex) IdleLineAlpha else 1f
                Box(
                    modifier = Modifier.requiredSize(LabelSize),
                    contentAlignment = Alignment.Center
                ) {
                    FieldLabelText(entries[entryIndex].index, field, alpha)
                }
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (entryIndex, fieldIndex) = labelIndexes[index]
                val entry = entries[entryIndex]
                val field = fields[fieldIndex]
                val x = entry.points[fieldIndex] - (constraints.maxWidth) / 2
                val canvasHeight = constraints.maxHeight.toFloat()
                val waveformsHeight = canvasHeight * waveformsHeightRatio
                val restCanvasHeight = canvasHeight - waveformsHeight
                val height = waveformsHeight * field.height + restCanvasHeight
                val y = canvasHeight - height - labelShiftUp - canvasHeight / 2
                placeable.place(x.toInt(), y.toInt())
            }
        }
    }
}

@Composable
private fun FieldLabelText(entryIndex: Int, field: LabelerConf.Field, alpha: Float) {
    Log.info("FieldLabel(${field.name}) of entry $entryIndex composed")
    Text(
        text = field.label,
        textAlign = TextAlign.Center,
        color = parseColor(field.color).copy(alpha = alpha),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.caption.copy(fontSize = 14.sp),
        overflow = TextOverflow.Visible
    )
}

@Composable
private fun NameLabels(state: MarkerState, requestRename: (Int) -> Unit, widthDp: Dp) {
    val leftEntry = remember(state.entriesInSample, state.entries.first().index) {
        val entry = state.entriesInSample.getPreviousOrNull { it.index == state.entries.first().index }
        entry?.let { state.entryConverter.convertToPixel(it, state.sampleLengthMillis) }
    }
    val rightEntry = remember(state.entriesInSample, state.entries.last().index) {
        val entry = state.entriesInSample.getNextOrNull { it.index == state.entries.last().index }
        entry?.let { state.entryConverter.convertToPixel(it, state.sampleLengthMillis) }
    }
    NameLabelsContent(
        modifier = Modifier.fillMaxHeight().width(widthDp),
        entries = state.entriesInPixel,
        leftEntry = leftEntry,
        rightEntry = rightEntry,
        requestRename = requestRename
    )
}

@Composable
private fun NameLabel(index: Int, name: String, color: Color, requestRename: (Int) -> Unit) {
    Log.info("NameLabel of entry $index composed")
    Text(
        modifier = Modifier.widthIn(max = 100.dp)
            .wrapContentSize()
            .clickable { requestRename(index) }
            .padding(vertical = 2.dp, horizontal = 5.dp),
        maxLines = 1,
        text = name,
        color = color,
        style = MaterialTheme.typography.caption
    )
}

@Composable
private fun NameLabelsContent(
    modifier: Modifier,
    entries: List<EntryInPixel>,
    leftEntry: EntryInPixel?,
    rightEntry: EntryInPixel?,
    requestRename: (Int) -> Unit
) {
    val items = remember(leftEntry, entries, rightEntry) {
        listOf(
            listOfNotNull(leftEntry),
            entries,
            listOfNotNull(rightEntry)
        ).flatten()
    }
    val colors = remember(leftEntry, entries, rightEntry) {
        listOf(
            listOfNotNull(leftEntry).map { Black },
            entries.map { DarkYellow },
            listOfNotNull(rightEntry).map { Black }
        ).flatten()
    }

    Layout(
        modifier = modifier,
        content = {
            items.indices.forEach { itemIndex ->
                val item = items[itemIndex]
                val color = colors[itemIndex]
                NameLabel(item.index, item.name, color, requestRename)
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val x = items[index].start
                placeable.place(x.toInt(), 0)
            }
        }
    }
}

private fun MarkerState.handleMouseMove(
    tool: Tool,
    event: PointerEvent,
    editEntries: (List<IndexedEntry>) -> Unit,
    screenRange: FloatRange?
) {
    screenRange ?: return
    when (tool) {
        Tool.Cursor -> handleCursorMove(event, editEntries, screenRange)
        Tool.Scissors -> handleScissorsMove(event, screenRange)
    }
}

private fun MarkerState.handleCursorMove(
    event: PointerEvent,
    editEntries: (List<IndexedEntry>) -> Unit,
    screenRange: FloatRange
) {
    val eventChange = event.changes.first()
    val x = eventChange.position.x.coerceIn(0f, canvasParams.lengthInPixel.toFloat())
    val actualX = x + screenRange.start
    val y = eventChange.position.y.coerceIn(0f, canvasHeightState.value.coerceAtLeast(0f))
    if (cursorState.value.mouse == Mouse.Dragging) {
        val updated = if (cursorState.value.lockedDrag) {
            getLockedDraggedEntries(cursorState.value.pointIndex, actualX, leftBorder, rightBorder)
        } else {
            getDraggedEntries(cursorState.value.pointIndex, actualX, leftBorder, rightBorder, labelerConf)
        }
        if (updated != entriesInPixel) {
            val updatedInMillis = updated.map { entryConverter.convertToMillis(it) }
            editEntries(updatedInMillis)
        }
    } else {
        val newPointIndex = getPointIndexForHovering(
            x = actualX,
            y = y,
            conf = labelerConf,
            canvasHeight = canvasHeightState.value,
            waveformsHeightRatio = waveformsHeightRatio,
            density = canvasParams.density,
            labelSize = LabelSize,
            labelShiftUp = LabelShiftUp
        )
        if (newPointIndex == NonePointIndex) {
            cursorState.update { moveToNothing() }
        } else {
            cursorState.update { moveToHover(newPointIndex) }
        }
    }
}

private fun MarkerState.handleScissorsMove(
    event: PointerEvent,
    screenRange: FloatRange
) {
    val scissorsState = scissorsState
    val x = event.changes.first().position.x + screenRange.start
    val position = x.takeIf { isValidCutPosition(it) }
    scissorsState.updateNonNull { copy(position = position) }
}

private fun MutableState<MarkerCursorState>.handleMousePress(
    tool: Tool,
    keyboardState: KeyboardState,
    labelerConf: LabelerConf
) {
    when (tool) {
        Tool.Cursor -> handleCursorPress(keyboardState, labelerConf)
        Tool.Scissors -> Unit
    }
}

private fun MutableState<MarkerCursorState>.handleCursorPress(
    keyboardState: KeyboardState,
    labelerConf: LabelerConf
) {
    if (!keyboardState.isCtrlPressed) {
        if (value.mouse == Mouse.Hovering) {
            val lockedDragByBaseField =
                labelerConf.lockedDrag.useDragBase &&
                    labelerConf.fields.getOrNull(value.pointIndex)?.dragBase == true
            val lockedDragByStart =
                labelerConf.lockedDrag.useStart && value.usingStartPoint
            val lockedDrag = (lockedDragByBaseField || lockedDragByStart) xor keyboardState.isShiftPressed
            update { startDragging(lockedDrag) }
        }
    }
}

private fun MarkerState.handleMouseRelease(
    tool: Tool,
    event: PointerEvent,
    submitEntry: () -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    cutEntry: (Int, Float) -> Unit,
    keyboardState: KeyboardState,
    screenRange: FloatRange?
) {
    screenRange ?: return
    if (keyboardState.isCtrlPressed) {
        val x = event.changes.first().position.x
        val actualX = x + screenRange.start
        val clickedRange = getClickedAudioRange(actualX, leftBorder, rightBorder)
        if (clickedRange != null) {
            val start = clickedRange.first?.let { entryConverter.convertToFrame(it) } ?: 0f
            val end = clickedRange.second?.let { entryConverter.convertToFrame(it) }
                ?: canvasParams.dataLength.toFloat()
            playSampleSection(start, end)
        }
    } else {
        when (tool) {
            Tool.Cursor -> handleCursorRelease(submitEntry)
            Tool.Scissors -> handleScissorsRelease(cutEntry)
        }
    }
}

private fun MarkerState.handleCursorRelease(submitEntry: () -> Unit) {
    submitEntry()
    cursorState.update { finishDragging() }
}

private fun MarkerState.handleScissorsRelease(
    cutEntry: (Int, Float) -> Unit
) {
    val scissorsState = scissorsState

    val position = scissorsState.requireValue().position
    if (position != null) {
        val timePosition = entryConverter.convertToMillis(position)
        val entryIndex = getEntryIndexByCutPosition(position)
        cutEntry(entryIndex, timePosition)
    }
}

@Composable
private fun LaunchAdjustScrollPosition(
    entriesInPixel: List<EntryInPixel>,
    currentIndex: Int,
    canvasLength: Int,
    horizontalScrollState: ScrollState,
    scrollFitViewModel: ScrollFitViewModel
) {
    LaunchedEffect(currentIndex, canvasLength, horizontalScrollState.maxValue) {
        scrollFitViewModel.update(horizontalScrollState, canvasLength, entriesInPixel, currentIndex)
    }
}
