package com.example.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MindNodeEntity
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.DueDateStatus
import com.example.ui.util.NodeDueDateHelper
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Smooth organic bezier curves matching Mindly's fluid physics
private val MindlyZoomEasing = CubicBezierEasing(0.20f, 0.0f, 0.0f, 1.0f)
private val BloomEasing = CubicBezierEasing(0.0f, 0.0f, 0.20f, 1.0f)

private enum class TransitionDirection {
    NONE, DIVE_IN, ZOOM_OUT
}

enum class DragDropTarget {
    NONE, POCKET, EDIT, DELETE
}

@Composable
fun OrbitalMindCanvas(
    centralNode: MindNodeEntity,
    orbitingNodes: List<MindNodeEntity>,
    allNodesInMap: List<MindNodeEntity>,
    currentRotationAngle: Float,
    pocketNode: MindNodeEntity? = null,
    zoomOutTrigger: Int = 0,
    isZenMode: Boolean = false,
    onRotateBy: (Float) -> Unit,
    onDiveIntoNode: (String) -> Unit,
    onCenterNodeClick: (MindNodeEntity) -> Unit,
    onOrbitNodeLongClick: (MindNodeEntity) -> Unit,
    onReorderNodes: (List<String>) -> Unit,
    onPutInPocket: (MindNodeEntity) -> Unit,
    onDropNodeOntoTarget: (sourceNode: MindNodeEntity, targetNode: MindNodeEntity) -> Unit = { _, _ -> },
    onDropHeldNode: (MindNodeEntity) -> Unit = {},
    onClearPocket: () -> Unit = {},
    onOpenPocketDialog: () -> Unit = {},
    onEditNode: (MindNodeEntity) -> Unit,
    onDeleteNode: (MindNodeEntity) -> Unit,
    onAddChildClick: () -> Unit,
    onNavigateToOriginalNode: ((String) -> Unit)? = null,
    onImageNodeClick: ((MindNodeEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Fast O(1) memoized lookups for nodes, child counts, and sync groups
    val nodesById = remember(allNodesInMap) { allNodesInMap.associateBy { it.id } }
    val childCountByParentId = remember(allNodesInMap) {
        allNodesInMap.groupBy { it.parentId }.mapValues { it.value.size }
    }
    val syncMasterMap = remember(allNodesInMap) {
        allNodesInMap
            .filter { !it.syncMasterId.isNullOrBlank() }
            .groupBy { it.syncMasterId!! }
            .mapValues { (_, nodes) -> nodes.minByOrNull { it.createdAt } }
    }

    // Find parent node if exists (to render Mindly-style parent horizon/halo)
    val parentNode = remember(centralNode.parentId, nodesById) {
        centralNode.parentId?.let { nodesById[it] }
    }

    // Dive-in and Zoom-out transition state
    var transitionDirection by remember { mutableStateOf(TransitionDirection.NONE) }
    var clickedNodeId by remember { mutableStateOf<String?>(null) }
    var targetNodeWorldPos by remember { mutableStateOf<Offset?>(null) }

    val transitionProgress = remember { Animatable(0f) }

    // Radial bloom reveal for incoming orbit children
    val orbitBloomProgress = remember(centralNode.id) { Animatable(0f) }

    // -------------------------------------------------------------
    // LONG PRESS & DRAG STATE (Reordering & Drop Actions)
    // -------------------------------------------------------------
    var draggedNode by remember { mutableStateOf<MindNodeEntity?>(null) }
    var dragPointerPos by remember { mutableStateOf(Offset.Zero) }
    var activeDropTarget by remember { mutableStateOf(DragDropTarget.NONE) }
    var isHoveringCentralCore by remember { mutableStateOf(false) }
    var hoveredSatelliteNode by remember { mutableStateOf<MindNodeEntity?>(null) }

    // Screen bounds for hit-testing bottom containers
    var canvasRootBounds by remember { mutableStateOf(Rect.Zero) }
    var pocketTargetBounds by remember { mutableStateOf(Rect.Zero) }
    var editTargetBounds by remember { mutableStateOf(Rect.Zero) }
    var deleteTargetBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(centralNode.id) {
        transitionProgress.snapTo(0f)
        transitionDirection = TransitionDirection.NONE
        clickedNodeId = null
        targetNodeWorldPos = null
        draggedNode = null
        activeDropTarget = DragDropTarget.NONE
        isHoveringCentralCore = false
        hoveredSatelliteNode = null

        // Bloom newly revealed orbiting satellites smoothly outward
        orbitBloomProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, easing = BloomEasing)
        )
    }

    val isTransitioning = transitionDirection != TransitionDirection.NONE

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (isZenMode) Color.Black else SleekBackground)
            .onGloballyPositioned { coords ->
                canvasRootBounds = coords.boundsInRoot()
            }
            .testTag("orbital_canvas_container")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val baseCenterX = widthPx / 2f
        val baseCenterY = heightPx / 2f

        // Standard capacity for unzoomed orbit is 8 nodes maximum
        val maxStandardOrbitNodes = 8
        val isOrbitZoomed = orbitingNodes.size > maxStandardOrbitNodes
        var isZoomActive by remember(centralNode.id, orbitingNodes.size) {
            mutableStateOf(isOrbitZoomed)
        }

        // All nodes exist together on the single continuous orbit ring without pagination
        val totalOrbitSlots = (orbitingNodes.size + 1).coerceAtLeast(1)
        val angleStep = 360f / totalOrbitSlots

        // Responsive Orbit Radius:
        // When orbiting nodes exceed 8 and zoom is active, zoom in to the orbit so that nodes
        // maintain a generous, readable arc distance along the circumference
        val minDim = minOf(widthPx, heightPx)
        val standardRadiusPx = minDim * 0.36f
        val targetZoomFactor = if (isZoomActive && isOrbitZoomed) {
            (totalOrbitSlots.toFloat() / 8.2f).coerceIn(1.22f, 2.7f)
        } else {
            1.0f
        }
        val animatedZoomFactor by animateFloatAsState(
            targetValue = targetZoomFactor,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "orbit_zoom_factor"
        )
        val baseRadiusPx = standardRadiusPx * animatedZoomFactor

        val visibleOrbitingNodes = orbitingNodes

        // Smoothly rotate the orbit by 1 node angle step to flip between nodes in the close-up view
        fun flipToNextNode() {
            coroutineScope.launch {
                var lastVal = 0f
                Animatable(0f).animateTo(
                    targetValue = -angleStep,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                ) {
                    val stepDelta = value - lastVal
                    lastVal = value
                    onRotateBy(stepDelta)
                }
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        fun flipToPrevNode() {
            coroutineScope.launch {
                var lastVal = 0f
                Animatable(0f).animateTo(
                    targetValue = angleStep,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                ) {
                    val stepDelta = value - lastVal
                    lastVal = value
                    onRotateBy(stepDelta)
                }
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        // Focus index of the node closest to the top focal point (270 degrees)
        val focusedNodeIndex = remember(currentRotationAngle, angleStep, orbitingNodes.size) {
            if (orbitingNodes.isEmpty()) 0
            else {
                var bestIdx = 0
                var minDiff = 360f
                val focalAngle = 270f
                for (i in 0 until orbitingNodes.size) {
                    val nodeAngle = ((currentRotationAngle + i * angleStep) % 360f + 360f) % 360f
                    var diff = kotlin.math.abs(nodeAngle - focalAngle)
                    if (diff > 180f) diff = 360f - diff
                    if (diff < minDiff) {
                        minDiff = diff
                        bestIdx = i
                    }
                }
                bestIdx
            }
        }
        val focusedNode = orbitingNodes.getOrNull(focusedNodeIndex)

        val centerNodeColor = remember(centralNode.colorHex) {
            OrbitColors.parseColor(centralNode.colorHex)
        }

        val parentNodeColor = remember(parentNode?.colorHex) {
            parentNode?.let { OrbitColors.parseColor(it.colorHex) } ?: centerNodeColor
        }

        // Angular Drag Gesture Detector for smooth orbital spinning with single finger (when not dragging a node)
        var previousTouchAngle by remember { mutableFloatStateOf(0f) }

        val dragModifier = Modifier.pointerInput(centralNode.id, baseCenterX, baseCenterY, isTransitioning, draggedNode != null) {
            if (isTransitioning || draggedNode != null) return@pointerInput
            detectDragGestures(
                onDragStart = { offset ->
                    val dx = offset.x - baseCenterX
                    val dy = offset.y - baseCenterY
                    previousTouchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                },
                onDrag = { change, _ ->
                    val dx = change.position.x - baseCenterX
                    val dy = change.position.y - baseCenterY
                    val currentTouchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    var delta = currentTouchAngle - previousTouchAngle
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f

                    onRotateBy(delta)
                    previousTouchAngle = currentTouchAngle
                    change.consume()
                }
            )
        }

        val canvasDotColor = AppTheme.colors.canvasDot
        val canvasTrackColor = AppTheme.colors.canvasTrack
        val activePrimaryColor = SleekPrimary

        // -------------------------------------------------------------
        // GEOMETRIC CAMERA PROJECTION (MINDLY MATHEMATICAL ZOOM MODEL)
        // -------------------------------------------------------------
        // Calculate parent slot position for smooth geometric zoom out
        val parentChildren = remember(parentNode, allNodesInMap) {
            if (parentNode == null) emptyList()
            else allNodesInMap.filter { it.parentId == parentNode.id }.sortedBy { it.orderIndex }
        }
        val mySlotIndex = remember(parentChildren, centralNode.id) {
            parentChildren.indexOfFirst { it.id == centralNode.id }.let { if (it == -1) 0 else it }
        }
        val parentTotalSlots = (parentChildren.size + 1).coerceAtLeast(2)
        val parentAngleStep = 360f / parentTotalSlots
        val mySlotAngleDeg = (mySlotIndex * parentAngleStep) % 360f
        val mySlotAngleRad = Math.toRadians(mySlotAngleDeg.toDouble())
        val landingWorldX = baseCenterX + baseRadiusPx * cos(mySlotAngleRad).toFloat()
        val landingWorldY = baseCenterY + baseRadiusPx * sin(mySlotAngleRad).toFloat()

        val parentRelWorldX = baseCenterX - (landingWorldX - baseCenterX)
        val parentRelWorldY = baseCenterY - (landingWorldY - baseCenterY)

        val p = transitionProgress.value
        val bloom = orbitBloomProgress.value
        val targetPos = targetNodeWorldPos ?: Offset(baseCenterX, baseCenterY)

        // Camera Scale & Focus point calculated based on transition direction
        val cameraScale = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> 1f + p * 0.75f
            TransitionDirection.ZOOM_OUT -> 1f - p * 0.25f
            TransitionDirection.NONE -> 1f
        }

        val focusX = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> (1f - p) * baseCenterX + p * targetPos.x
            TransitionDirection.ZOOM_OUT -> (1f - p) * baseCenterX + p * parentRelWorldX
            TransitionDirection.NONE -> baseCenterX
        }

        val focusY = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> (1f - p) * baseCenterY + p * targetPos.y
            TransitionDirection.ZOOM_OUT -> (1f - p) * baseCenterY + p * parentRelWorldY
            TransitionDirection.NONE -> baseCenterY
        }

        // Project any point (worldX, worldY) to screen coordinates
        fun projectToScreen(worldX: Float, worldY: Float): Offset {
            val screenX = baseCenterX + (worldX - focusX) * cameraScale
            val screenY = baseCenterY + (worldY - focusY) * cameraScale
            return Offset(screenX, screenY)
        }

        // Screen center & radius of the orbital ring
        val ringCenterScreen = projectToScreen(baseCenterX, baseCenterY)
        val activeBloomRadius = when (transitionDirection) {
            TransitionDirection.ZOOM_OUT -> baseRadiusPx * (1f - p * 0.55f)
            TransitionDirection.DIVE_IN -> baseRadiusPx
            TransitionDirection.NONE -> baseRadiusPx * bloom
        }
        val ringRadiusScreen = activeBloomRadius * cameraScale

        // Center Core Node position and dimensions
        val centerScreenPos = ringCenterScreen
        val centerCoreSizeBaseDp = 124.dp
        val satelliteBaseSizeDp = 82.dp

        val centerCoreSizeDp = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> (centerCoreSizeBaseDp * (1f - p * 0.45f)).coerceIn(36.dp, 160.dp)
            TransitionDirection.ZOOM_OUT -> (centerCoreSizeBaseDp - (centerCoreSizeBaseDp - satelliteBaseSizeDp) * p).coerceIn(36.dp, 160.dp)
            TransitionDirection.NONE -> centerCoreSizeBaseDp
        }
        val centerCoreSizePx = with(density) { centerCoreSizeDp.toPx() }

        // Hit testing for bottom action containers, Satellite Nodes & Central Core
        fun updateDropTarget(pos: Offset) {
            val rootPos = Offset(pos.x + canvasRootBounds.left, pos.y + canvasRootBounds.top)
            val hitPadding = with(density) { 20.dp.toPx() }

            fun Rect.expanded() = Rect(left - hitPadding, top - hitPadding, right + hitPadding, bottom + hitPadding)

            val newTarget = when {
                pocketTargetBounds.expanded().contains(rootPos) -> DragDropTarget.POCKET
                editTargetBounds.expanded().contains(rootPos) -> DragDropTarget.EDIT
                deleteTargetBounds.expanded().contains(rootPos) -> DragDropTarget.DELETE
                else -> DragDropTarget.NONE
            }

            if (newTarget != activeDropTarget) {
                activeDropTarget = newTarget
                if (newTarget != DragDropTarget.NONE) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            if (newTarget == DragDropTarget.NONE && draggedNode != null) {
                // 1. Check Central Core Node
                val dxCenter = pos.x - centerScreenPos.x
                val dyCenter = pos.y - centerScreenPos.y
                val distToCenter = kotlin.math.sqrt((dxCenter * dxCenter + dyCenter * dyCenter).toDouble()).toFloat()
                val centerHitRadius = centerCoreSizePx / 2f + with(density) { 22.dp.toPx() }
                val hoveringCenter = distToCenter < centerHitRadius && draggedNode?.id != centralNode.id

                if (hoveringCenter != isHoveringCentralCore) {
                    isHoveringCentralCore = hoveringCenter
                    if (hoveringCenter) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                // 2. Check Orbiting Satellite Nodes
                var hoveredSat: MindNodeEntity? = null
                if (!hoveringCenter) {
                    val satHitRadius = with(density) { (satelliteBaseSizeDp * 0.95f).toPx() }
                    for ((index, satNode) in visibleOrbitingNodes.withIndex()) {
                        if (satNode.id == draggedNode?.id) continue
                        val slotAngleDeg = (currentRotationAngle + index * angleStep) % 360f
                        val slotAngleRad = Math.toRadians(slotAngleDeg.toDouble())
                        val nodeWorldX = baseCenterX + activeBloomRadius * cos(slotAngleRad).toFloat()
                        val nodeWorldY = baseCenterY + activeBloomRadius * sin(slotAngleRad).toFloat()
                        val nodeScreen = projectToScreen(nodeWorldX, nodeWorldY)

                        val dx = pos.x - nodeScreen.x
                        val dy = pos.y - nodeScreen.y
                        val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        if (dist < satHitRadius) {
                            hoveredSat = satNode
                            break
                        }
                    }
                }

                if (hoveredSat?.id != hoveredSatelliteNode?.id) {
                    hoveredSatelliteNode = hoveredSat
                    if (hoveredSat != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            } else {
                isHoveringCentralCore = false
                hoveredSatelliteNode = null
            }
        }

        // Trigger precise Mindly Dive In
        fun triggerMindlyDive(nodeId: String, worldX: Float, worldY: Float) {
            if (isTransitioning || draggedNode != null) return
            transitionDirection = TransitionDirection.DIVE_IN
            clickedNodeId = nodeId
            targetNodeWorldPos = Offset(worldX, worldY)

            coroutineScope.launch {
                transitionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = MindlyZoomEasing)
                )
                onDiveIntoNode(nodeId)
            }
        }

        // Trigger Mindly Zoom Out to Parent
        fun triggerMindlyZoomOut(parentId: String) {
            if (isTransitioning || draggedNode != null) return
            transitionDirection = TransitionDirection.ZOOM_OUT

            coroutineScope.launch {
                transitionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = MindlyZoomEasing)
                )
                onDiveIntoNode(parentId)
            }
        }

        // Listen for parent zoom out trigger from MindMapScreen (Back button / gesture)
        LaunchedEffect(zoomOutTrigger) {
            if (zoomOutTrigger > 0 && parentNode != null && !isTransitioning) {
                triggerMindlyZoomOut(parentNode.id)
            }
        }

        // 1. Draw Background Grid, Parent Horizon Arc & Orbital Tracks
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(dragModifier)
        ) {
            if (!isZenMode) {
                // Subtle dot grid shifted by camera
                val dotSpacing = 28.dp.toPx()
                val dotRadius = 1.2.dp.toPx()

                val gridOffsetX = (-(focusX - baseCenterX) * 0.35f) % dotSpacing
                val gridOffsetY = (-(focusY - baseCenterY) * 0.35f) % dotSpacing

                var gx = gridOffsetX
                while (gx < size.width) {
                    var gy = gridOffsetY
                    while (gy < size.height) {
                        drawCircle(
                            color = canvasDotColor,
                            radius = dotRadius,
                            center = Offset(gx, gy)
                        )
                        gy += dotSpacing
                    }
                    gx += dotSpacing
                }
            }

            // Parent Orbit Horizon Arc (Mindly-style contextual background horizon)
            if (parentNode != null && !isZenMode) {
                val parentHorizonRadius = baseRadiusPx * 2.1f * cameraScale
                val parentHorizonCenter = Offset(
                    ringCenterScreen.x - parentHorizonRadius * 0.65f,
                    ringCenterScreen.y - parentHorizonRadius * 0.65f
                )
                drawCircle(
                    color = parentNodeColor.copy(alpha = 0.04f),
                    radius = parentHorizonRadius,
                    center = parentHorizonCenter
                )
                drawCircle(
                    color = parentNodeColor.copy(alpha = 0.18f),
                    radius = parentHorizonRadius,
                    center = parentHorizonCenter,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
                    )
                )
            }

            val ringAlpha = if (isTransitioning) {
                (1f - p * 1.5f).coerceIn(0f, 1f)
            } else {
                bloom.coerceIn(0f, 1f)
            }

            if (ringAlpha > 0.01f) {
                // Glow around active orbit
                drawCircle(
                    color = centerNodeColor.copy(alpha = (if (isZenMode) 0.12f else 0.06f) * ringAlpha),
                    radius = ringRadiusScreen * 1.28f,
                    center = ringCenterScreen
                )

                // Orbital Ring Track
                drawCircle(
                    color = if (draggedNode != null) activePrimaryColor.copy(alpha = 0.55f) else if (isZenMode) Color.White.copy(alpha = 0.35f * ringAlpha) else canvasTrackColor.copy(alpha = ringAlpha),
                    radius = ringRadiusScreen,
                    center = ringCenterScreen,
                    style = Stroke(
                        width = if (draggedNode != null) 2.5.dp.toPx() else 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                )

                // Small Intermediate Drop/Reorder Anchor Dots on Orbit Ring during Drag
                if ((draggedNode != null || pocketNode != null) && totalOrbitSlots > 0) {
                    for (i in 0 until totalOrbitSlots) {
                        val midAngleDeg = (currentRotationAngle + i * angleStep + angleStep / 2f) % 360f
                        val midAngleRad = Math.toRadians(midAngleDeg.toDouble())
                        val slotX = ringCenterScreen.x + ringRadiusScreen * cos(midAngleRad).toFloat()
                        val slotY = ringCenterScreen.y + ringRadiusScreen * sin(midAngleRad).toFloat()
                        drawCircle(
                            color = activePrimaryColor.copy(alpha = 0.4f),
                            radius = 4.dp.toPx(),
                            center = Offset(slotX, slotY)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = 2.dp.toPx(),
                            center = Offset(slotX, slotY)
                        )
                    }
                }
            }
        }

        // 2. Parent Core Node (Emerges smoothly to center during Zoom Out)
        if (transitionDirection == TransitionDirection.ZOOM_OUT && parentNode != null) {
            val parentZoomPos = projectToScreen(parentRelWorldX, parentRelWorldY)
            val parentZoomSizeDp = (satelliteBaseSizeDp + (centerCoreSizeBaseDp - satelliteBaseSizeDp) * p).coerceIn(36.dp, 160.dp)
            val parentZoomSizePx = with(density) { parentZoomSizeDp.toPx() }
            val parentZoomAlpha = (p * 1.5f).coerceIn(0f, 1f)

            CentralCoreNode(
                node = parentNode,
                color = parentNodeColor,
                size = parentZoomSizeDp,
                allNodes = allNodesInMap,
                isHeldInPocket = pocketNode?.id == parentNode.id,
                isDropTargetHover = false,
                onClick = {},
                modifier = Modifier
                    .graphicsLayer {
                        alpha = parentZoomAlpha
                    }
                    .offset {
                        IntOffset(
                            (parentZoomPos.x - parentZoomSizePx / 2).roundToInt(),
                            (parentZoomPos.y - parentZoomSizePx / 2).roundToInt()
                        )
                    }
            )
        }

        // 3. Central Core Node
        val centerAlphaVal = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> (1f - p * 1.6f).coerceIn(0f, 1f)
            TransitionDirection.ZOOM_OUT -> 1f // Smoothly transforms into satellite without vanishing
            TransitionDirection.NONE -> 1f
        }

        CentralCoreNode(
            node = centralNode,
            color = centerNodeColor,
            size = centerCoreSizeDp,
            allNodes = allNodesInMap,
            isHeldInPocket = pocketNode?.id == centralNode.id,
            isDropTargetHover = isHoveringCentralCore,
            onClick = {
                if (!isTransitioning && draggedNode == null) {
                    if (!centralNode.imageUri.isNullOrBlank() && onImageNodeClick != null) {
                        onImageNodeClick(centralNode)
                    } else {
                        onCenterNodeClick(centralNode)
                    }
                }
            },
            onNavigateToOriginalNode = { originalId ->
                if (!isTransitioning && draggedNode == null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (onNavigateToOriginalNode != null) {
                        onNavigateToOriginalNode(originalId)
                    } else {
                        onDiveIntoNode(originalId)
                    }
                }
            },
            modifier = Modifier
                .graphicsLayer {
                    alpha = centerAlphaVal
                }
                .offset {
                    IntOffset(
                        (centerScreenPos.x - centerCoreSizePx / 2).roundToInt(),
                        (centerScreenPos.y - centerCoreSizePx / 2).roundToInt()
                    )
                }
        )

        // 4. Orbiting Satellite Nodes (Paginated up to 7 max per ring)
        visibleOrbitingNodes.forEachIndexed { localIndex, node ->
            key(node.id) {
                val isThisClicked = clickedNodeId == node.id
                val isThisBeingDragged = draggedNode?.id == node.id
                val isThisHeld = pocketNode?.id == node.id

                val slotAngleDeg = (currentRotationAngle + localIndex * angleStep) % 360f
                val slotAngleRad = Math.toRadians(slotAngleDeg.toDouble())

                val nodeWorldX = baseCenterX + activeBloomRadius * cos(slotAngleRad).toFloat()
                val nodeWorldY = baseCenterY + activeBloomRadius * sin(slotAngleRad).toFloat()

                val nodeScreenPos = projectToScreen(nodeWorldX, nodeWorldY)

                // Size transition
                val dynamicNodeSizeDp = if (isThisClicked) {
                    (satelliteBaseSizeDp + (centerCoreSizeBaseDp - satelliteBaseSizeDp) * p).coerceIn(40.dp, 165.dp)
                } else if (transitionDirection == TransitionDirection.DIVE_IN) {
                    (satelliteBaseSizeDp * (1f - p * 0.45f)).coerceIn(20.dp, 120.dp)
                } else if (transitionDirection == TransitionDirection.ZOOM_OUT) {
                    (satelliteBaseSizeDp * (1f - p * 0.55f)).coerceIn(10.dp, 120.dp)
                } else {
                    (satelliteBaseSizeDp * (0.35f + 0.65f * bloom)).coerceIn(25.dp, 120.dp)
                }
                val dynamicNodeSizePx = with(density) { dynamicNodeSizeDp.toPx() }

                // Alpha transition
                val nodeAlphaVal = if (isThisClicked) {
                    1f
                } else if (isThisBeingDragged) {
                    0.25f // Ghost placeholder in its slot on the orbit
                } else if (isThisHeld) {
                    0.35f // Faint / dim on the orbit while parked in the hold pocket
                } else if (transitionDirection == TransitionDirection.DIVE_IN) {
                    (1f - p * 1.8f).coerceIn(0f, 1f)
                } else if (transitionDirection == TransitionDirection.ZOOM_OUT) {
                    (1f - p * 1.8f).coerceIn(0f, 1f)
                } else {
                    bloom.coerceIn(0f, 1f)
                }

                val childCount = childCountByParentId[node.id] ?: 0

                SatellitePlanetNode(
                    node = node,
                    size = dynamicNodeSizeDp,
                    childCount = childCount,
                    allNodes = allNodesInMap,
                    isGhostPlaceholder = isThisBeingDragged,
                    isHeldInPocket = isThisHeld,
                    isDropTargetHover = hoveredSatelliteNode?.id == node.id,
                    onClick = {
                        if (!isTransitioning && draggedNode == null) {
                            if (!node.imageUri.isNullOrBlank() && onImageNodeClick != null) {
                                onImageNodeClick(node)
                            } else {
                                triggerMindlyDive(node.id, nodeWorldX, nodeWorldY)
                            }
                        }
                    },
                    onNavigateToOriginalNode = { originalId ->
                        if (!isTransitioning && draggedNode == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (onNavigateToOriginalNode != null) {
                                onNavigateToOriginalNode(originalId)
                            } else {
                                onDiveIntoNode(originalId)
                            }
                        }
                    },
                    onStartDrag = { localOffset ->
                        if (!isTransitioning) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            draggedNode = node
                            val fingerX = (nodeScreenPos.x - dynamicNodeSizePx / 2f) + localOffset.x
                            val fingerY = (nodeScreenPos.y - dynamicNodeSizePx / 2f) + localOffset.y
                            dragPointerPos = Offset(fingerX, fingerY)
                            activeDropTarget = DragDropTarget.NONE
                            isHoveringCentralCore = false
                            hoveredSatelliteNode = null
                            updateDropTarget(dragPointerPos)
                        }
                    },
                    onDrag = { dragAmount ->
                        dragPointerPos += dragAmount
                        updateDropTarget(dragPointerPos)
                    },
                    onEndDrag = {
                        val currentDragged = draggedNode
                        val target = activeDropTarget
                        val wasHoveringCenter = isHoveringCentralCore
                        val hoveredSat = hoveredSatelliteNode
                        val wasHeldNode = currentDragged?.id == pocketNode?.id
                        val releasePos = dragPointerPos
                        draggedNode = null
                        activeDropTarget = DragDropTarget.NONE
                        isHoveringCentralCore = false
                        hoveredSatelliteNode = null

                        if (currentDragged != null) {
                            if (hoveredSat != null && hoveredSat.id != currentDragged.id) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDropNodeOntoTarget(currentDragged, hoveredSat)
                            } else if (wasHoveringCenter && centralNode.id != currentDragged.id) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDropNodeOntoTarget(currentDragged, centralNode)
                            } else {
                                when (target) {
                                    DragDropTarget.POCKET -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (wasHeldNode) {
                                            onDropHeldNode(currentDragged)
                                        } else {
                                            onPutInPocket(currentDragged)
                                        }
                                    }
                                    DragDropTarget.EDIT -> {
                                        onEditNode(currentDragged)
                                    }
                                    DragDropTarget.DELETE -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDeleteNode(currentDragged)
                                    }
                                    DragDropTarget.NONE -> {
                                        if (wasHeldNode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onDropHeldNode(currentDragged)
                                        } else if (orbitingNodes.size > 1) {
                                            val dx = releasePos.x - baseCenterX
                                            val dy = releasePos.y - baseCenterY
                                            val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                                            if (dist > centerCoreSizePx * 0.4f) {
                                                val rawAngleDeg = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                                val relAngle = ((rawAngleDeg - currentRotationAngle) % 360f + 360f) % 360f
                                                val targetIndex = (relAngle / angleStep).roundToInt().coerceIn(0, orbitingNodes.size - 1)
                                                val currentGlobalIndex = orbitingNodes.indexOfFirst { it.id == currentDragged.id }
                                                if (currentGlobalIndex != -1 && currentGlobalIndex != targetIndex) {
                                                    val mutable = orbitingNodes.toMutableList()
                                                    val item = mutable.removeAt(currentGlobalIndex)
                                                    mutable.add(targetIndex.coerceIn(0, mutable.size), item)
                                                    onReorderNodes(mutable.map { it.id })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onCancelDrag = {
                        draggedNode = null
                        activeDropTarget = DragDropTarget.NONE
                        isHoveringCentralCore = false
                        hoveredSatelliteNode = null
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = nodeAlphaVal
                        }
                        .offset {
                            IntOffset(
                                (nodeScreenPos.x - dynamicNodeSizePx / 2).roundToInt(),
                                (nodeScreenPos.y - dynamicNodeSizePx / 2).roundToInt()
                            )
                        }
                )
            }
        }

        // 4. The (+) Add Orbiting Node Button
        val addSlotAngleDeg = (currentRotationAngle + visibleOrbitingNodes.size * angleStep) % 360f
        val addSlotAngleRad = Math.toRadians(addSlotAngleDeg.toDouble())
        val addWorldX = baseCenterX + activeBloomRadius * cos(addSlotAngleRad).toFloat()
        val addWorldY = baseCenterY + activeBloomRadius * sin(addSlotAngleRad).toFloat()

        val addScreenPos = projectToScreen(addWorldX, addWorldY)
        val addAlphaVal = if (isTransitioning) {
            (1f - p * 2.0f).coerceIn(0f, 1f)
        } else if (draggedNode != null) {
            0.4f
        } else {
            bloom.coerceIn(0f, 1f)
        }

        val addSizeDp = if (isTransitioning) {
            (68.dp * (1f - p * 0.5f)).coerceIn(20.dp, 80.dp)
        } else {
            (68.dp * (0.4f + 0.6f * bloom)).coerceIn(28.dp, 80.dp)
        }
        val addSizePx = with(density) { addSizeDp.toPx() }

        AddPlanetSlot(
            size = addSizeDp,
            themeColor = SleekPrimary,
            onClick = {
                if (!isTransitioning && draggedNode == null) onAddChildClick()
            },
            modifier = Modifier
                .graphicsLayer {
                    alpha = addAlphaVal
                }
                .offset {
                    IntOffset(
                        (addScreenPos.x - addSizePx / 2).roundToInt(),
                        (addScreenPos.y - addSizePx / 2).roundToInt()
                    )
                }
        )

        // 5. Zoomed Orbit Flipper & Focus Pill (Active when nodes exceed 8)
        if (isOrbitZoomed && !isTransitioning) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SleekSurfaceElevated.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, SleekBorderSubtle),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // Previous Node Button (Flips backward along the orbit)
                    IconButton(
                        onClick = { flipToPrevNode() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "◀",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Center Focus Display: Current Focused Node & Total Count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                isZoomActive = !isZoomActive
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isZoomActive) "🔭 مقرب" else "🌐 كامل",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isZoomActive) SleekPrimary else TextSecondary
                            )
                            Text(
                                text = "•",
                                fontSize = 10.sp,
                                color = TextTertiary
                            )
                            Text(
                                text = "${focusedNodeIndex + 1} من ${orbitingNodes.size}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        focusedNode?.let { node ->
                            Text(
                                text = node.title.take(16),
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Next Node Button (Flips forward along the orbit)
                    IconButton(
                        onClick = { flipToNextNode() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "▶",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary
                        )
                    }

                    // Toggle View mode button (Zoomed Close-up vs Full Overview)
                    IconButton(
                        onClick = {
                            isZoomActive = !isZoomActive
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .padding(start = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isZoomActive) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                            contentDescription = if (isZoomActive) "تصغير لعرض المدار كاملاً" else "تكبير لمشهد مقرب",
                            tint = if (isZoomActive) SleekPrimary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 6. Floating Dragged Node Avatar directly under user's finger
        draggedNode?.let { node ->
            val hoveredTargetTitle = hoveredSatelliteNode?.title ?: if (isHoveringCentralCore) centralNode.title else null
            FloatingDraggedAvatar(
                node = node,
                position = dragPointerPos,
                activeDropTarget = activeDropTarget,
                isHeldInPocket = pocketNode?.id == node.id,
                isHoveringCenter = isHoveringCentralCore,
                hoveredTargetTitle = hoveredTargetTitle,
                density = density
            )
        }

        // 7. Top Drag Hint Banner
        AnimatedVisibility(
            visible = draggedNode != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 58.dp)
        ) {
            val isHeld = draggedNode?.id == pocketNode?.id
            val hoveredTargetTitle = hoveredSatelliteNode?.title ?: if (isHoveringCentralCore) centralNode.title else null
            val isDropOntoNode = hoveredTargetTitle != null

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SleekSurfaceElevated.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isDropOntoNode) 1.5.dp else 1.dp,
                    color = if (isDropOntoNode) Color(0xFF06B6D4) else if (isHeld) Color(0xFF06B6D4) else SleekBorderSubtle
                ),
                shadowElevation = 10.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    if (isDropOntoNode) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(16.dp).padding(end = 6.dp)
                        )
                        Text(
                            text = "إفلات للإسقاط داخل: $hoveredTargetTitle (نقل • نسخ • مزامنة)",
                            color = Color(0xFF06B6D4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isHeld) {
                        Icon(
                            imageVector = Icons.Default.GeneratingTokens,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(15.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = "عقدة ممسوكة: اتركها فوق أي كوكب للإسقاط أو في المدار",
                            color = Color(0xFF06B6D4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.drag_reorder_hint),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 8. Bottom Action Drop Containers (Hold / Edit / Delete)
        AnimatedVisibility(
            visible = draggedNode != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            BottomActionDropBar(
                activeDropTarget = activeDropTarget,
                isDraggingHeldNode = draggedNode?.id == pocketNode?.id,
                onPocketPositioned = { pocketTargetBounds = it },
                onEditPositioned = { editTargetBounds = it },
                onDeletePositioned = { deleteTargetBounds = it }
            )
        }

        // 9. Floating Orbital Pocket Badge in the Corner
        // Supports Tap for Options dialog, Clear button, and LONG-PRESS TO DRAG onto Canvas
        AnimatedVisibility(
            visible = pocketNode != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 86.dp, end = 20.dp)
        ) {
            pocketNode?.let { heldNode ->
                var badgeBounds by remember { mutableStateOf(Rect.Zero) }
                val isBeingDragged = draggedNode?.id == heldNode.id

                FloatingOrbitalPocketBadge(
                    pocketNode = heldNode,
                    isBeingDragged = isBeingDragged,
                    onClick = {
                        if (draggedNode == null) onOpenPocketDialog()
                    },
                    onClear = {
                        if (draggedNode == null) onClearPocket()
                    },
                    onStartDrag = { localOffset ->
                        if (!isTransitioning) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            draggedNode = heldNode
                            val initialPos = badgeBounds.topLeft + localOffset
                            dragPointerPos = initialPos
                            activeDropTarget = DragDropTarget.NONE
                            isHoveringCentralCore = false
                            hoveredSatelliteNode = null
                            updateDropTarget(initialPos)
                        }
                    },
                    onDrag = { dragAmount ->
                        dragPointerPos += dragAmount
                        updateDropTarget(dragPointerPos)
                    },
                    onEndDrag = {
                        val currentDragged = draggedNode
                        val target = activeDropTarget
                        val wasHoveringCenter = isHoveringCentralCore
                        val hoveredSat = hoveredSatelliteNode
                        draggedNode = null
                        activeDropTarget = DragDropTarget.NONE
                        isHoveringCentralCore = false
                        hoveredSatelliteNode = null
                        if (currentDragged != null) {
                            if (hoveredSat != null && hoveredSat.id != currentDragged.id) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDropNodeOntoTarget(currentDragged, hoveredSat)
                            } else if (wasHoveringCenter && centralNode.id != currentDragged.id) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDropNodeOntoTarget(currentDragged, centralNode)
                            } else if (target == DragDropTarget.DELETE) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteNode(currentDragged)
                            } else if (target == DragDropTarget.EDIT) {
                                onEditNode(currentDragged)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDropHeldNode(currentDragged)
                            }
                        }
                    },
                    onCancelDrag = {
                        draggedNode = null
                        activeDropTarget = DragDropTarget.NONE
                        isHoveringCentralCore = false
                        hoveredSatelliteNode = null
                    },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        badgeBounds = coordinates.boundsInRoot()
                    }
                )
            }
        }
    }
}

/**
 * Animated Floating Orbital Pocket badge displayed when a node is held for moving, cloning, or live syncing.
 * Supports tap to open options, clear button, and LONG PRESS TO DRAG across the canvas.
 */
@Composable
private fun FloatingOrbitalPocketBadge(
    pocketNode: MindNodeEntity,
    isBeingDragged: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
    onStartDrag: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeColor = remember(pocketNode.colorHex) {
        OrbitColors.parseColor(pocketNode.colorHex)
    }
    val textColor = remember(nodeColor) {
        OrbitColors.getContrastingTextColor(nodeColor)
    }
    val icon = remember(pocketNode.iconName) {
        OrbitIcons.getIcon(pocketNode.iconName)
    }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnClear by rememberUpdatedState(onClear)
    val currentOnStartDrag by rememberUpdatedState(onStartDrag)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnEndDrag by rememberUpdatedState(onEndDrag)
    val currentOnCancelDrag by rememberUpdatedState(onCancelDrag)

    val infiniteTransition = rememberInfiniteTransition(label = "pocket_halo")
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashPhase"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val cyanAccent = Color(0xFF06B6D4)

    Box(
        modifier = modifier
            .testTag("floating_orbital_pocket")
            .graphicsLayer {
                alpha = if (isBeingDragged) 0.3f else 1f
            }
    ) {
        // Main Circular Node with Dashed Ring around it
        Box(
            modifier = Modifier
                .size(74.dp)
                .scale(if (isBeingDragged) 0.95f else pulseScale)
                .drawBehind {
                    // Draw outer dashed ring (دايرة وحواليها شرط)
                    val strokeWidth = 3.dp.toPx()
                    val radius = (size.minDimension / 2f) - strokeWidth / 2f
                    drawCircle(
                        color = cyanAccent,
                        radius = radius,
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), dashPhase)
                        )
                    )
                }
                .padding(5.dp)
                .shadow(12.dp, CircleShape, ambientColor = cyanAccent.copy(alpha = 0.5f), spotColor = cyanAccent)
                .clip(CircleShape)
                .background(nodeColor)
                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .pointerInput(pocketNode.id) {
                    detectTapGestures(
                        onTap = { currentOnClick() }
                    )
                }
                .pointerInput(pocketNode.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset -> currentOnStartDrag(offset) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount)
                        },
                        onDragEnd = { currentOnEndDrag() },
                        onDragCancel = { currentOnCancelDrag() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (!pocketNode.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = pocketNode.imageUri,
                    contentDescription = pocketNode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                if (icon != null && pocketNode.imageUri.isNullOrBlank()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = pocketNode.title,
                    color = if (!pocketNode.imageUri.isNullOrBlank()) Color.White else textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Small Clear / Dismiss Button at top corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .size(22.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(SleekSurfaceElevated)
                .border(1.dp, SleekBorderSubtle, CircleShape)
                .clickable(onClick = { currentOnClear() }),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_clear),
                tint = TextSecondary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * Floating avatar that follows the user's finger during drag.
 */
@Composable
private fun FloatingDraggedAvatar(
    node: MindNodeEntity,
    position: Offset,
    activeDropTarget: DragDropTarget,
    isHeldInPocket: Boolean = false,
    isHoveringCenter: Boolean = false,
    hoveredTargetTitle: String? = null,
    density: androidx.compose.ui.unit.Density
) {
    val nodeColor = remember(node.colorHex) {
        OrbitColors.parseColor(node.colorHex)
    }
    val textColor = remember(nodeColor) {
        OrbitColors.getContrastingTextColor(nodeColor)
    }
    val icon = remember(node.iconName) {
        OrbitIcons.getIcon(node.iconName)
    }

    val isTargetingNode = hoveredTargetTitle != null || isHoveringCenter

    val borderColor = when {
        activeDropTarget == DragDropTarget.POCKET -> Color(0xFF06B6D4)
        activeDropTarget == DragDropTarget.EDIT -> Color(0xFF6366F1)
        activeDropTarget == DragDropTarget.DELETE -> Color(0xFFEF4444)
        isTargetingNode -> Color(0xFF06B6D4)
        isHeldInPocket -> Color(0xFF06B6D4)
        else -> Color.White
    }

    val avatarScale by animateFloatAsState(
        targetValue = if (activeDropTarget != DragDropTarget.NONE || isTargetingNode) 1.25f else 1.08f,
        animationSpec = spring(stiffness = 600f),
        label = "avatarScale"
    )

    val avatarSizeDp = 86.dp
    val avatarSizePx = with(density) { avatarSizeDp.toPx() }

    val hasImage = !node.imageUri.isNullOrBlank()

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (position.x - avatarSizePx / 2).roundToInt(),
                    (position.y - avatarSizePx / 2).roundToInt()
                )
            }
            .scale(avatarScale)
            .size(avatarSizeDp)
            .shadow(
                elevation = if (isTargetingNode) 20.dp else 16.dp,
                shape = CircleShape,
                ambientColor = if (isTargetingNode) Color(0xFF06B6D4).copy(alpha = 0.6f) else nodeColor.copy(alpha = 0.5f),
                spotColor = borderColor
            )
            .clip(CircleShape)
            .background(nodeColor)
            .border(
                width = if (isHeldInPocket || isTargetingNode) 3.5.dp else 3.dp,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasImage) {
            AsyncImage(
                model = node.imageUri,
                contentDescription = node.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Translucent gradient overlay for high contrast text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.08f))

            if (icon != null && !hasImage) {
                Icon(
                    imageVector = icon,
                    contentDescription = node.title,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = node.title,
                color = if (hasImage) Color.White else textColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Spacer(modifier = Modifier.weight(0.08f))
        }
    }
}

/**
 * Bottom Dock bar containing the drop target containers.
 */
@Composable
private fun BottomActionDropBar(
    activeDropTarget: DragDropTarget,
    isDraggingHeldNode: Boolean = false,
    onPocketPositioned: (Rect) -> Unit,
    onEditPositioned: (Rect) -> Unit,
    onDeletePositioned: (Rect) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SleekSurfaceElevated.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
        shadowElevation = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Container 1: Hold in Pocket / Insert (إمساك أو إدراج)
            DropTargetContainer(
                title = if (isDraggingHeldNode) "إدراج الخيارات" else stringResource(R.string.drag_hold_pocket),
                subtitle = if (isDraggingHeldNode) "خيارات الإدراج" else "إمساك بالجيب",
                icon = if (isDraggingHeldNode) Icons.Default.DriveFileMove else Icons.Default.GeneratingTokens,
                accentColor = Color(0xFF06B6D4),
                isActive = activeDropTarget == DragDropTarget.POCKET,
                onPositioned = onPocketPositioned,
                modifier = Modifier.weight(1.1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Container 2: Edit (التعديل)
            DropTargetContainer(
                title = stringResource(R.string.drag_edit),
                subtitle = "تعديل",
                icon = Icons.Default.Edit,
                accentColor = Color(0xFF6366F1),
                isActive = activeDropTarget == DragDropTarget.EDIT,
                onPositioned = onEditPositioned,
                modifier = Modifier.weight(0.95f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Container 3: Delete (الحذف)
            DropTargetContainer(
                title = stringResource(R.string.drag_delete),
                subtitle = "حذف",
                icon = Icons.Default.Delete,
                accentColor = Color(0xFFEF4444),
                isActive = activeDropTarget == DragDropTarget.DELETE,
                onPositioned = onDeletePositioned,
                modifier = Modifier.weight(0.95f)
            )
        }
    }
}

/**
 * Individual drop target container in the bottom dock.
 */
@Composable
private fun DropTargetContainer(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isActive: Boolean,
    onPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1.0f,
        animationSpec = spring(stiffness = 700f),
        label = "dropTargetScale"
    )

    val bgColor = if (isActive) accentColor.copy(alpha = 0.22f) else SleekSurface
    val borderCol = if (isActive) accentColor else SleekBorderSubtle
    val borderWidth = if (isActive) 2.dp else 1.dp

    Box(
        modifier = modifier
            .scale(scale)
            .height(64.dp)
            .onGloballyPositioned { coords ->
                onPositioned(coords.boundsInRoot())
            }
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(borderWidth, borderCol, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) accentColor else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (isActive) accentColor else TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Mindly-style compact contextual lateral parent halo.
 * Discrete, sleek mini-chip providing instant return context without cluttering the canvas.
 */
@Composable
private fun ParentOrbitHalo(
    parentNode: MindNodeEntity,
    parentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = remember(parentNode.iconName) {
        OrbitIcons.getIcon(parentNode.iconName)
    }

    val textColor = remember(parentColor) {
        OrbitColors.getContrastingTextColor(parentColor)
    }

    Surface(
        shape = CircleShape,
        color = parentColor,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderSubtle),
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            )
            .testTag("parent_orbit_halo")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Return to parent orbit",
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )

            if (icon != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = parentNode.title,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = parentNode.title,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 10-segment circular progress ring around a planet node.
 * Fixed to 10 segments (levels 0-10).
 * Completed progress is vivid emerald green (#10B981), remaining segments are distinct sleek slate ring segments.
 */
@Composable
private fun ProgressRing10(
    progress: Int,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF10B981),
    inactiveColor: Color = Color(0x8864748B)
) {
    val clamped = progress.coerceIn(0, 10)
    Canvas(modifier = modifier) {
        val totalSegments = 10
        val gapAngle = 6.0f // Gap degrees between segments
        val sweepPerSegment = (360f / totalSegments) - gapAngle
        val strokePx = strokeWidth.toPx()

        // Ensure the ring arcs are drawn strictly within the canvas bounds
        val radius = (size.minDimension - strokePx) / 2f
        if (radius <= 0f) return@Canvas

        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        val arcSize = Size(radius * 2f, radius * 2f)
        val arcTopLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)

        for (i in 0 until totalSegments) {
            val startAngle = -90f + i * (sweepPerSegment + gapAngle) + (gapAngle / 2f)
            val isFilled = i < clamped
            val segColor = if (isFilled) activeColor else inactiveColor

            drawArc(
                color = segColor,
                startAngle = startAngle,
                sweepAngle = sweepPerSegment,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Visual badge indicating the impact power level (⚡ 1..5) on the node perimeter.
 */
@Composable
private fun ImpactBadge(
    impact: Int,
    isSatellite: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clamped = impact.coerceIn(1, 5)
    val activeColor = when (clamped) {
        1 -> Color(0xFF94A3B8)
        2 -> Color(0xFF38BDF8)
        3 -> Color(0xFFF59E0B)
        4 -> Color(0xFFF97316)
        5 -> Color(0xFFEF4444)
        else -> Color(0xFF94A3B8)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SleekSurfaceElevated.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, activeColor),
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = if (isSatellite) 3.5.dp else 4.5.dp,
                vertical = 1.dp
            )
        ) {
            Text(
                text = "⚡",
                fontSize = if (isSatellite) 7.sp else 8.sp
            )
            Spacer(modifier = Modifier.width(1.5.dp))
            Text(
                text = "$clamped",
                color = activeColor,
                fontSize = if (isSatellite) 8.5.sp else 9.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Visual pulsing/glowing ring drawn outside the node when a deadline is set.
 * The ring adopts warm urgent colors (warm gold / deep orange / crimson) as the due date approaches or passes.
 */
@Composable
private fun DueDateGlowRing(
    status: DueDateStatus,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    val glowColor = NodeDueDateHelper.getStatusGlowColor(status)
    val shouldPulse = status == DueDateStatus.APPROACHING || status == DueDateStatus.DUE_TODAY || status == DueDateStatus.OVERDUE

    val pulseAlpha by if (shouldPulse) {
        val infiniteTransition = rememberInfiniteTransition(label = "dueDateGlow")
        infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dueDatePulse"
        )
    } else {
        remember { mutableFloatStateOf(0.40f) }
    }

    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val arcTopLeft = Offset(strokePx / 2f, strokePx / 2f)
        val arcSize = Size(size.width - strokePx, size.height - strokePx)

        // Outer ambient glow ring
        if (shouldPulse) {
            drawCircle(
                color = glowColor.copy(alpha = pulseAlpha * 0.35f),
                radius = (size.width / 2f),
                style = Stroke(width = strokePx * 1.8f)
            )
        }

        // Crisp perimeter alert ring
        drawArc(
            color = glowColor.copy(alpha = pulseAlpha),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * Compact chip badge displaying the formatted relative remaining time or deadline state.
 */
@Composable
private fun DueDateBadge(
    dueDate: Long,
    isSatellite: Boolean = false,
    modifier: Modifier = Modifier
) {
    val status = remember(dueDate) { NodeDueDateHelper.calculateStatus(dueDate) }
    val glowColor = NodeDueDateHelper.getStatusGlowColor(status)
    val relativeText = remember(dueDate) { NodeDueDateHelper.formatRelativeDueDate(dueDate) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SleekSurfaceElevated.copy(alpha = 0.96f),
        border = BorderStroke(1.2.dp, glowColor),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = if (isSatellite) 4.dp else 5.dp,
                vertical = 1.5.dp
            )
        ) {
            Icon(
                imageVector = if (status == DueDateStatus.OVERDUE || status == DueDateStatus.DUE_TODAY) {
                    Icons.Default.WarningAmber
                } else {
                    Icons.Default.Schedule
                },
                contentDescription = "Due Date",
                tint = glowColor,
                modifier = Modifier.size(if (isSatellite) 8.5.dp else 10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = relativeText,
                color = glowColor,
                fontSize = if (isSatellite) 7.5.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CentralCoreNode(
    node: MindNodeEntity,
    color: Color,
    size: Dp,
    allNodes: List<MindNodeEntity>,
    isHeldInPocket: Boolean = false,
    isDropTargetHover: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onNavigateToOriginalNode: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val icon = remember(node.iconName) {
        OrbitIcons.getIcon(node.iconName)
    }

    val hasImage = !node.imageUri.isNullOrBlank()

    val textColor = remember(color, hasImage) {
        if (hasImage) Color.White else OrbitColors.getContrastingTextColor(color)
    }

    // Dash rotation animation for held nodes (only runs when active)
    val dashPhase = if (isHeldInPocket || isDropTargetHover) {
        val infiniteTransition = rememberInfiniteTransition(label = "dashTransition")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 40f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dashPhase"
        )
        phase
    } else {
        0f
    }

    val coreScale by animateFloatAsState(
        targetValue = if (isDropTargetHover) 1.12f else 1.0f,
        animationSpec = spring(stiffness = 500f),
        label = "coreScale"
    )

    val isCloneTwin = remember(node.id, node.syncMasterId, allNodes) {
        if (!node.isSyncTwin) false
        else {
            val directMaster = allNodes.firstOrNull { it.id == node.syncMasterId && it.id != node.id }
            if (directMaster != null) {
                true
            } else if (!node.syncMasterId.isNullOrBlank() && node.id != node.syncMasterId) {
                val earliest = allNodes.filter { it.syncMasterId == node.syncMasterId }.minByOrNull { it.createdAt }
                earliest != null && earliest.id != node.id
            } else {
                false
            }
        }
    }

    val isParentCloneTwin = remember(node.parentId, allNodes) {
        if (node.parentId == null) false
        else {
            val parent = allNodes.firstOrNull { it.id == node.parentId }
            parent?.isSyncTwin == true && (parent.syncMasterId != null && parent.id != parent.syncMasterId)
        }
    }

    // Only show sync styling & badge on clone twins (not the original root node)
    val isMainSyncTwin = isCloneTwin && !isParentCloneTwin

    // Identify the original/master node if this current central node is a synchronized clone
    val originalNode = remember(node.id, node.syncMasterId, allNodes, isCloneTwin) {
        if (isCloneTwin) {
            val directMaster = allNodes.firstOrNull { it.id == node.syncMasterId && it.id != node.id }
            if (directMaster != null) {
                directMaster
            } else if (!node.syncMasterId.isNullOrBlank() && node.id != node.syncMasterId) {
                allNodes.filter { it.syncMasterId == node.syncMasterId && it.id != node.id }
                    .minByOrNull { it.createdAt }
            } else {
                null
            }
        } else null
    }

    val checklistItems = remember(node.checklistJson) { node.checklist }
    val totalTasks = checklistItems.size
    val doneTasks = if (totalTasks > 0) checklistItems.count { it.isDone } else 0
    val hasChecklist = totalTasks > 0
    val isTasksComplete = hasChecklist && doneTasks == totalTasks
    val nodeProgress = node.progress
    val nodeImpact = node.impact
    val nodeDueDate = node.dueDate
    val dueDateStatus = remember(nodeDueDate) { nodeDueDate?.let { d: Long -> NodeDueDateHelper.calculateStatus(d) } }

    Box(
        modifier = modifier
            .size(size)
            .scale(coreScale),
        contentAlignment = Alignment.Center
    ) {
        // Due Date Alert Glow Ring (Outside planet body, beneath progress ring)
        if (dueDateStatus != null) {
            DueDateGlowRing(
                status = dueDateStatus,
                strokeWidth = 3.5.dp,
                modifier = Modifier
                    .size(if (nodeProgress != null) size + 22.dp else size + 14.dp)
                    .align(Alignment.Center)
            )
        }
        // Main Circle Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (isHeldInPocket || isDropTargetHover) 18.dp else 12.dp,
                    shape = CircleShape,
                    ambientColor = if (isHeldInPocket || isDropTargetHover) Color(0xFF06B6D4) else color.copy(alpha = 0.35f),
                    spotColor = if (isHeldInPocket || isDropTargetHover) Color(0xFF06B6D4) else color
                )
                .clip(CircleShape)
                .background(color)
                .drawBehind {
                    if (isHeldInPocket || isDropTargetHover) {
                        val strokeW = 3.5.dp.toPx()
                        val r = (size.toPx() - strokeW) / 2f
                        drawCircle(
                            color = Color(0xFF06B6D4),
                            radius = r,
                            style = Stroke(
                                width = strokeW,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), dashPhase)
                            )
                        )
                    }
                }
                .then(
                    if (isHeldInPocket || isDropTargetHover) {
                        Modifier
                    } else Modifier
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .testTag("central_core_node"),
            contentAlignment = Alignment.Center
        ) {
            if (hasImage) {
                AsyncImage(
                    model = node.imageUri,
                    contentDescription = node.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Subtle scrim overlay for crisp legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Central Icon (Compact at the top, consistent spacing)
                if (icon != null && !hasImage) {
                    Icon(
                        imageVector = icon,
                        contentDescription = node.title,
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Node Title (Constant stable font size & max lines)
                Text(
                    text = node.title,
                    color = textColor,
                    fontSize = (size.value * 0.13f).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = (size.value * 0.15f).sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Internal Notes Icon at the bottom inside the node
                if (node.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Notes",
                        tint = textColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // 10-Segment Progress Ring (Drawn cleanly outside the planet body)
        if (nodeProgress != null) {
            ProgressRing10(
                progress = nodeProgress,
                strokeWidth = 4.dp,
                modifier = Modifier
                    .size(size + 14.dp)
                    .align(Alignment.Center)
            )
        }

        // ==========================================
        // EXTERNAL PERIMETER BADGES (Top / Corners)
        // ==========================================

        // 1. Top Start: Task / Checklist Outer Badge
        if (hasChecklist) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isTasksComplete) Color(0xFF059669) else SleekSurfaceElevated,
                border = BorderStroke(1.5.dp, if (isTasksComplete) Color(0xFF10B981) else SleekBorderSubtle),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-6).dp, y = (-4).dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Icon(
                        imageVector = if (isTasksComplete) Icons.Default.DoneAll else Icons.Default.Check,
                        contentDescription = "Tasks",
                        tint = if (isTasksComplete) Color.White else Color(0xFF10B981),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$doneTasks/$totalTasks",
                        color = if (isTasksComplete) Color.White else TextPrimary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Bottom Start: Impact Power Badge
        if (nodeImpact != null) {
            ImpactBadge(
                impact = nodeImpact,
                isSatellite = false,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-6).dp, y = 4.dp)
            )
        }

        // 3. Top Center / Offset: Due Date Alert Badge
        if (nodeDueDate != null) {
            DueDateBadge(
                dueDate = nodeDueDate,
                isSatellite = false,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
            )
        }

        // 3. Bottom End: Pocket / Sync Return Indicator (Outside the node)
        if (isHeldInPocket || isMainSyncTwin) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
            ) {
                if (isHeldInPocket) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF06B6D4),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GeneratingTokens,
                                contentDescription = "ممسوكة",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                } else if (isMainSyncTwin) {
                    val isReturnAction = originalNode != null && onNavigateToOriginalNode != null
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF06B6D4),
                        shadowElevation = if (isReturnAction) 6.dp else 4.dp,
                        border = if (isReturnAction) BorderStroke(1.5.dp, Color.White) else null,
                        modifier = Modifier
                            .size(24.dp)
                            .then(
                                if (isReturnAction) {
                                    Modifier.clickable {
                                        onNavigateToOriginalNode(originalNode.id)
                                    }
                                } else Modifier
                            )
                            .testTag("sync_badge_return_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = if (isReturnAction) stringResource(R.string.return_to_original_node_cd) else "Live Synced",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SatellitePlanetNode(
    node: MindNodeEntity,
    size: Dp,
    childCount: Int,
    allNodes: List<MindNodeEntity> = emptyList(),
    isGhostPlaceholder: Boolean = false,
    isHeldInPocket: Boolean = false,
    isDropTargetHover: Boolean = false,
    onClick: () -> Unit,
    onNavigateToOriginalNode: ((String) -> Unit)? = null,
    onStartDrag: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnStartDrag by rememberUpdatedState(onStartDrag)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnEndDrag by rememberUpdatedState(onEndDrag)
    val currentOnCancelDrag by rememberUpdatedState(onCancelDrag)

    val nodeColor = remember(node.colorHex) {
        OrbitColors.parseColor(node.colorHex)
    }

    val hasImage = !node.imageUri.isNullOrBlank()

    val textColor = remember(nodeColor, hasImage) {
        if (hasImage) Color.White else OrbitColors.getContrastingTextColor(nodeColor)
    }

    val icon = remember(node.iconName) {
        OrbitIcons.getIcon(node.iconName)
    }

    val planetScale by animateFloatAsState(
        targetValue = if (isDropTargetHover) 1.22f else 1.0f,
        animationSpec = spring(stiffness = 500f),
        label = "planetScale"
    )

    val isCloneTwin = remember(node.id, node.syncMasterId, allNodes) {
        if (!node.isSyncTwin) false
        else {
            val directMaster = allNodes.firstOrNull { it.id == node.syncMasterId && it.id != node.id }
            if (directMaster != null) {
                true
            } else if (!node.syncMasterId.isNullOrBlank() && node.id != node.syncMasterId) {
                val earliest = allNodes.filter { it.syncMasterId == node.syncMasterId }.minByOrNull { it.createdAt }
                earliest != null && earliest.id != node.id
            } else {
                false
            }
        }
    }

    val isParentCloneTwin = remember(node.parentId, allNodes) {
        if (node.parentId == null) false
        else {
            val parent = allNodes.firstOrNull { it.id == node.parentId }
            parent?.isSyncTwin == true && (parent.syncMasterId != null && parent.id != parent.syncMasterId)
        }
    }

    // Only show sync styling & badge on clone twins (not the original root node)
    val isMainSyncTwin = isCloneTwin && !isParentCloneTwin

    val originalNode = remember(node.id, node.syncMasterId, allNodes, isCloneTwin) {
        if (isCloneTwin) {
            val directMaster = allNodes.firstOrNull { it.id == node.syncMasterId && it.id != node.id }
            if (directMaster != null) {
                directMaster
            } else if (!node.syncMasterId.isNullOrBlank() && node.id != node.syncMasterId) {
                allNodes.filter { it.syncMasterId == node.syncMasterId && it.id != node.id }
                    .minByOrNull { it.createdAt }
            } else {
                null
            }
        } else null
    }

    // Dash rotation animation for held node border & drop target hover (only runs when active)
    val dashPhase = if (isDropTargetHover || isHeldInPocket) {
        val infiniteTransition = rememberInfiniteTransition(label = "satDashTransition")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 32f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "satDashPhase"
        )
        phase
    } else {
        0f
    }

    val checklistItems = remember(node.checklistJson) { node.checklist }
    val totalTasks = checklistItems.size
    val doneTasks = if (totalTasks > 0) checklistItems.count { it.isDone } else 0
    val hasChecklist = totalTasks > 0
    val isTasksComplete = hasChecklist && doneTasks == totalTasks
    val nodeProgress = node.progress
    val nodeImpact = node.impact
    val nodeDueDate = node.dueDate
    val dueDateStatus = remember(nodeDueDate) { nodeDueDate?.let { d: Long -> NodeDueDateHelper.calculateStatus(d) } }

    Box(
        modifier = modifier
            .scale(planetScale)
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        // Due Date Alert Glow Ring (Outside planet body)
        if (dueDateStatus != null && !isGhostPlaceholder) {
            DueDateGlowRing(
                status = dueDateStatus,
                strokeWidth = 2.8.dp,
                modifier = Modifier
                    .size(if (nodeProgress != null) size + 18.dp else size + 11.dp)
                    .align(Alignment.Center)
            )
        }
        // Main Planet Circle Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (isGhostPlaceholder) 0.dp else if (isDropTargetHover) 14.dp else if (isHeldInPocket) 10.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = if (isDropTargetHover || isHeldInPocket) Color(0xFF06B6D4) else nodeColor.copy(alpha = 0.25f),
                    spotColor = if (isDropTargetHover || isHeldInPocket) Color(0xFF06B6D4) else nodeColor
                )
                .clip(CircleShape)
                .background(if (isGhostPlaceholder) nodeColor.copy(alpha = 0.3f) else nodeColor)
                .drawBehind {
                    if (isDropTargetHover || isHeldInPocket) {
                        val strokeW = if (isDropTargetHover) 3.5.dp.toPx() else 3.dp.toPx()
                        val r = (size.toPx() - strokeW) / 2f
                        drawCircle(
                            color = Color(0xFF06B6D4),
                            radius = r,
                            style = Stroke(
                                width = strokeW,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), dashPhase)
                            )
                        )
                    }
                }
                .then(
                    if (isGhostPlaceholder) {
                        Modifier.border(
                            width = 2.5.dp,
                            color = SleekPrimary,
                            shape = CircleShape
                        )
                    } else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = { currentOnClick() }
                )
                .pointerInput(node.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset -> currentOnStartDrag(offset) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount)
                        },
                        onDragEnd = { currentOnEndDrag() },
                        onDragCancel = { currentOnCancelDrag() }
                    )
                }
                .testTag("satellite_node_${node.id}"),
            contentAlignment = Alignment.Center
        ) {
            if (hasImage) {
                // Full picture presentation
                AsyncImage(
                    model = node.imageUri,
                    contentDescription = node.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Subtle bottom gradient for caption
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.50f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.80f)
                            )
                        )
                )
                // Top-right photo badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Image Node",
                        tint = Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 3.dp)
            ) {
                if (!hasImage) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = node.title,
                            tint = textColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Text(
                        text = node.title,
                        color = textColor,
                        fontSize = (size.value * 0.13f).sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = (size.value * 0.15f).sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    // Internal Notes Icon at bottom inside the node
                    if (node.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(1.5.dp))
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Notes",
                            tint = textColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))

                    if (node.title.isNotBlank() && node.title != "صورة") {
                        Text(
                            text = node.title,
                            color = Color.White,
                            fontSize = (size.value * 0.125f).sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // 10-Segment Progress Ring (Drawn cleanly outside & on top of the satellite planet)
        if (nodeProgress != null && !isGhostPlaceholder) {
            ProgressRing10(
                progress = nodeProgress,
                strokeWidth = 3.2.dp,
                modifier = Modifier
                    .size(size + 11.dp)
                    .align(Alignment.Center)
            )
        }

        // ==========================================
        // EXTERNAL PERIMETER BADGES (Top / Corners)
        // ==========================================

        // 1. Top End: Child Count Outer Badge (+N)
        if (childCount > 0) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SleekSurfaceElevated,
                border = BorderStroke(1.2.dp, SleekBorderSubtle),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-4).dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+$childCount",
                        color = Color(0xFF06B6D4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Top Start: Task / Checklist Counter Badge (M/N)
        if (hasChecklist) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isTasksComplete) Color(0xFF059669) else SleekSurfaceElevated,
                border = BorderStroke(1.2.dp, if (isTasksComplete) Color(0xFF10B981) else SleekBorderSubtle),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-5).dp, y = (-4).dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isTasksComplete) Icons.Default.DoneAll else Icons.Default.Check,
                        contentDescription = "Tasks",
                        tint = if (isTasksComplete) Color.White else Color(0xFF10B981),
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$doneTasks/$totalTasks",
                        color = if (isTasksComplete) Color.White else TextPrimary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Bottom End: Pocket / Sync Return Indicator (Outside the node)
        if (isHeldInPocket || isMainSyncTwin) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
            ) {
                if (isHeldInPocket) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF06B6D4),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(17.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GeneratingTokens,
                                contentDescription = "ممسوكة",
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                } else if (isMainSyncTwin) {
                    val isReturnAction = originalNode != null && onNavigateToOriginalNode != null
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF06B6D4),
                        shadowElevation = if (isReturnAction) 5.dp else 3.dp,
                        border = if (isReturnAction) BorderStroke(1.dp, Color.White) else null,
                        modifier = Modifier
                            .size(19.dp)
                            .then(
                                if (isReturnAction) {
                                    Modifier.clickable {
                                        onNavigateToOriginalNode(originalNode.id)
                                    }
                                } else Modifier
                            )
                            .testTag("satellite_sync_badge_return_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = if (isReturnAction) stringResource(R.string.return_to_original_node_cd) else "Live Synced",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Bottom Start: Impact Power Badge
        if (nodeImpact != null && !isGhostPlaceholder) {
            ImpactBadge(
                impact = nodeImpact,
                isSatellite = true,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-4).dp, y = 3.dp)
            )
        }

        // 5. Top Center: Due Date Alert Badge
        if (nodeDueDate != null && !isGhostPlaceholder) {
            DueDateBadge(
                dueDate = nodeDueDate,
                isSatellite = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-11).dp)
            )
        }
    }
}

@Composable
private fun AddPlanetSlot(
    size: Dp,
    themeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(SleekSurface)
            .border(
                width = 1.5.dp,
                color = SleekBorder,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = themeColor),
                onClick = onClick
            )
            .testTag("add_planet_slot"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add orbiting idea",
                tint = themeColor,
                modifier = Modifier.size((size.value * 0.36f).dp)
            )
            Text(
                text = "Add",
                color = themeColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

