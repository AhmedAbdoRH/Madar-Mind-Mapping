package com.example.ui.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MindNodeEntity
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.util.OrbitColors
import com.example.ui.util.OrbitIcons
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Smooth organic bezier curves matching Mindly's fluid physics
private val MindlyZoomEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
private val BloomEasing = CubicBezierEasing(0.18f, 0.89f, 0.32f, 1.15f)

private enum class TransitionDirection {
    NONE, DIVE_IN, ZOOM_OUT
}

@Composable
fun OrbitalMindCanvas(
    centralNode: MindNodeEntity,
    orbitingNodes: List<MindNodeEntity>,
    allNodesInMap: List<MindNodeEntity>,
    currentRotationAngle: Float,
    onRotateBy: (Float) -> Unit,
    onDiveIntoNode: (String) -> Unit,
    onCenterNodeClick: (MindNodeEntity) -> Unit,
    onOrbitNodeLongClick: (MindNodeEntity) -> Unit,
    onAddChildClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Find parent node if exists (to render Mindly-style parent horizon/halo)
    val parentNode = remember(centralNode.parentId, allNodesInMap) {
        allNodesInMap.firstOrNull { it.id == centralNode.parentId }
    }

    // Dive-in and Zoom-out transition state
    var transitionDirection by remember { mutableStateOf(TransitionDirection.NONE) }
    var clickedNodeId by remember { mutableStateOf<String?>(null) }
    var targetNodeWorldPos by remember { mutableStateOf<Offset?>(null) }

    val transitionProgress = remember { Animatable(0f) }

    // Radial bloom reveal for incoming orbit children
    val orbitBloomProgress = remember(centralNode.id) { Animatable(0f) }

    LaunchedEffect(centralNode.id) {
        transitionProgress.snapTo(0f)
        transitionDirection = TransitionDirection.NONE
        clickedNodeId = null
        targetNodeWorldPos = null

        // Bloom newly revealed orbiting satellites smoothly outward
        orbitBloomProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 360, easing = BloomEasing)
        )
    }

    val isTransitioning = transitionDirection != TransitionDirection.NONE

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .testTag("orbital_canvas_container")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val baseCenterX = widthPx / 2f
        val baseCenterY = heightPx / 2f

        // Responsive Orbit Radius perfectly fitted to screen
        val minDim = minOf(widthPx, heightPx)
        val baseRadiusPx = minDim * 0.36f

        val centerNodeColor = remember(centralNode.colorHex) {
            OrbitColors.parseColor(centralNode.colorHex)
        }

        val parentNodeColor = remember(parentNode?.colorHex) {
            parentNode?.let { OrbitColors.parseColor(it.colorHex) } ?: centerNodeColor
        }

        // Angular Drag Gesture Detector for smooth orbital spinning with single finger
        var previousTouchAngle by remember { mutableFloatStateOf(0f) }

        val dragModifier = Modifier.pointerInput(centralNode.id, baseCenterX, baseCenterY, isTransitioning) {
            if (isTransitioning) return@pointerInput
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

        // -------------------------------------------------------------
        // GEOMETRIC CAMERA PROJECTION (MINDLY MATHEMATICAL ZOOM MODEL)
        // -------------------------------------------------------------
        val p = transitionProgress.value
        val bloom = orbitBloomProgress.value
        val targetPos = targetNodeWorldPos ?: Offset(baseCenterX, baseCenterY)

        // Camera Scale & Focus point calculated based on transition direction
        val cameraScale = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> 1f + p * 1.35f
            TransitionDirection.ZOOM_OUT -> 1f - p * 0.45f
            TransitionDirection.NONE -> 1f
        }

        val focusX = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> (1f - p) * baseCenterX + p * targetPos.x
            TransitionDirection.ZOOM_OUT -> (1f - p) * baseCenterX + p * (baseCenterX - widthPx * 0.35f)
            TransitionDirection.NONE -> baseCenterX
        }

        val focusY = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> (1f - p) * baseCenterY + p * targetPos.y
            TransitionDirection.ZOOM_OUT -> (1f - p) * baseCenterY + p * (baseCenterY - heightPx * 0.35f)
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
        val activeBloomRadius = baseRadiusPx * bloom
        val ringRadiusScreen = activeBloomRadius * cameraScale

        // Orbit distribution
        val totalOrbitSlots = orbitingNodes.size + 1
        val angleStep = 360f / totalOrbitSlots

        // Trigger precise Mindly Dive In
        fun triggerMindlyDive(nodeId: String, worldX: Float, worldY: Float) {
            if (isTransitioning) return
            transitionDirection = TransitionDirection.DIVE_IN
            clickedNodeId = nodeId
            targetNodeWorldPos = Offset(worldX, worldY)

            coroutineScope.launch {
                transitionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 380, easing = MindlyZoomEasing)
                )
                onDiveIntoNode(nodeId)
            }
        }

        // Trigger Mindly Zoom Out to Parent
        fun triggerMindlyZoomOut(parentId: String) {
            if (isTransitioning) return
            transitionDirection = TransitionDirection.ZOOM_OUT

            coroutineScope.launch {
                transitionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 360, easing = MindlyZoomEasing)
                )
                onDiveIntoNode(parentId)
            }
        }

        // 1. Draw Background Grid, Parent Horizon Arc & Orbital Tracks
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(dragModifier)
        ) {
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

            // Parent Orbit Horizon Arc (Mindly-style contextual background horizon)
            if (parentNode != null) {
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
                    color = centerNodeColor.copy(alpha = 0.06f * ringAlpha),
                    radius = ringRadiusScreen * 1.28f,
                    center = ringCenterScreen
                )

                // Orbital Ring Track
                drawCircle(
                    color = canvasTrackColor.copy(alpha = ringAlpha),
                    radius = ringRadiusScreen,
                    center = ringCenterScreen,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                )
            }
        }

        // 2. Central Core Node
        val centerScreenPos = projectToScreen(baseCenterX, baseCenterY)
        val centerCoreSizeBaseDp = 124.dp
        val centerCoreSizeDp = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> (centerCoreSizeBaseDp * (1f - p * 0.4f)).coerceIn(36.dp, 160.dp)
            TransitionDirection.ZOOM_OUT -> (centerCoreSizeBaseDp * (1f - p * 0.35f)).coerceIn(36.dp, 160.dp)
            TransitionDirection.NONE -> centerCoreSizeBaseDp
        }
        val centerCoreSizePx = with(density) { centerCoreSizeDp.toPx() }
        val centerAlphaVal = when (transitionDirection) {
            TransitionDirection.DIVE_IN -> (1f - p * 1.6f).coerceIn(0f, 1f)
            TransitionDirection.ZOOM_OUT -> (1f - p * 1.4f).coerceIn(0f, 1f)
            TransitionDirection.NONE -> 1f
        }

        CentralCoreNode(
            node = centralNode,
            color = centerNodeColor,
            size = centerCoreSizeDp,
            allNodes = allNodesInMap,
            onClick = {
                if (!isTransitioning) onCenterNodeClick(centralNode)
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

        // 3. Orbiting Satellite Nodes
        val satelliteBaseSizeDp = 82.dp

        orbitingNodes.forEachIndexed { index, node ->
            val slotAngleDeg = (currentRotationAngle + index * angleStep) % 360f
            val slotAngleRad = Math.toRadians(slotAngleDeg.toDouble())

            val nodeWorldX = baseCenterX + activeBloomRadius * cos(slotAngleRad).toFloat()
            val nodeWorldY = baseCenterY + activeBloomRadius * sin(slotAngleRad).toFloat()

            // Calculate precise Screen Coordinate
            val nodeScreenPos = projectToScreen(nodeWorldX, nodeWorldY)

            val isThisClicked = clickedNodeId == node.id

            // Size transition
            val dynamicNodeSizeDp = if (isThisClicked) {
                (satelliteBaseSizeDp + (centerCoreSizeBaseDp - satelliteBaseSizeDp) * p).coerceIn(40.dp, 165.dp)
            } else if (isTransitioning) {
                (satelliteBaseSizeDp * (1f - p * 0.5f)).coerceIn(20.dp, 120.dp)
            } else {
                (satelliteBaseSizeDp * (0.4f + 0.6f * bloom)).coerceIn(30.dp, 120.dp)
            }
            val dynamicNodeSizePx = with(density) { dynamicNodeSizeDp.toPx() }

            // Alpha transition
            val nodeAlphaVal = if (isThisClicked) {
                1f
            } else if (isTransitioning) {
                (1f - p * 1.8f).coerceIn(0f, 1f)
            } else {
                bloom.coerceIn(0f, 1f)
            }

            val childCount = remember(node.id, allNodesInMap) {
                allNodesInMap.count { it.parentId == node.id }
            }

            SatellitePlanetNode(
                node = node,
                size = dynamicNodeSizeDp,
                childCount = childCount,
                onClick = {
                    triggerMindlyDive(node.id, nodeWorldX, nodeWorldY)
                },
                onLongClick = {
                    if (!isTransitioning) onOrbitNodeLongClick(node)
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

        // 4. The (+) Add Orbiting Node Button
        val addSlotAngleDeg = (currentRotationAngle + orbitingNodes.size * angleStep) % 360f
        val addSlotAngleRad = Math.toRadians(addSlotAngleDeg.toDouble())
        val addWorldX = baseCenterX + activeBloomRadius * cos(addSlotAngleRad).toFloat()
        val addWorldY = baseCenterY + activeBloomRadius * sin(addSlotAngleRad).toFloat()

        val addScreenPos = projectToScreen(addWorldX, addWorldY)
        val addAlphaVal = if (isTransitioning) {
            (1f - p * 2.0f).coerceIn(0f, 1f)
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
                if (!isTransitioning) onAddChildClick()
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

        // 5. Mindly-Style Parent Orbit Halo / Return Bubble (Compact Lateral Horizon Node)
        if (parentNode != null) {
            ParentOrbitHalo(
                parentNode = parentNode,
                parentColor = parentNodeColor,
                onClick = {
                    triggerMindlyZoomOut(parentNode.id)
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 62.dp)
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
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CentralCoreNode(
    node: MindNodeEntity,
    color: Color,
    size: Dp,
    allNodes: List<MindNodeEntity>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val childCount = remember(node.id, allNodes) {
        allNodes.count { it.parentId == node.id }
    }

    val icon = remember(node.iconName) {
        OrbitIcons.getIcon(node.iconName)
    }

    val textColor = remember(color) {
        OrbitColors.getContrastingTextColor(color)
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(12.dp, CircleShape, ambientColor = color.copy(alpha = 0.35f), spotColor = color)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 3.5.dp,
                color = if (node.isSyncTwin) Color(0xFF06B6D4) else Color.White,
                shape = CircleShape
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp)
            .testTag("central_core_node"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(0.12f))

            // Central Icon (Optional)
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = node.title,
                    tint = textColor,
                    modifier = Modifier.size((size.value * 0.28f).dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Node Title
            Text(
                text = node.title,
                color = textColor,
                fontSize = if (icon != null) (size.value * 0.115f).sp else (size.value * 0.14f).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = if (icon != null) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = if (icon != null) (size.value * 0.135f).sp else (size.value * 0.16f).sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.weight(0.12f))

            // Sub-badges row (Children count, sync indicator, notes/checklist indicator)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.isSyncTwin) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF06B6D4).copy(alpha = 0.85f),
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Live Synced",
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
                if (childCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (textColor == Color.White) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "$childCount orbits",
                            color = textColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                if (node.notes.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Notes",
                        tint = textColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(11.dp)
                            .padding(start = 2.dp)
                    )
                }
                if (node.checklist.isNotEmpty()) {
                    val doneCount = node.checklist.count { it.isDone }
                    val totalCount = node.checklist.size
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (doneCount == totalCount) Color(0xFF006D44) else if (textColor == Color.White) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = "$doneCount/$totalCount",
                            color = if (doneCount == totalCount) Color.White else textColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SatellitePlanetNode(
    node: MindNodeEntity,
    size: Dp,
    childCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
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

    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, CircleShape, ambientColor = nodeColor.copy(alpha = 0.25f), spotColor = nodeColor)
            .clip(CircleShape)
            .background(nodeColor)
            .border(
                width = if (node.isSyncTwin) 2.5.dp else 2.dp,
                color = if (node.isSyncTwin) Color(0xFF06B6D4) else Color.White,
                shape = CircleShape
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(6.dp)
            .testTag("satellite_node_${node.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(0.08f))

            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = node.title,
                    tint = textColor,
                    modifier = Modifier.size((size.value * 0.28f).dp)
                )
            }

            Text(
                text = node.title,
                color = textColor,
                fontSize = if (icon != null) (size.value * 0.12f).sp else (size.value * 0.145f).sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = if (icon != null) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = if (icon != null) (size.value * 0.14f).sp else (size.value * 0.165f).sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Spacer(modifier = Modifier.weight(0.08f))

            // Bottom Indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (node.isSyncTwin) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF06B6D4).copy(alpha = 0.85f),
                        modifier = Modifier.padding(end = 2.dp, bottom = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Live Synced",
                            tint = Color.White,
                            modifier = Modifier
                                .size(11.dp)
                                .padding(1.5.dp)
                        )
                    }
                }
                if (childCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (textColor == Color.White) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 1.dp)
                    ) {
                        Text(
                            text = "+$childCount",
                            color = textColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 0.5.dp)
                        )
                    }
                }
            }
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
