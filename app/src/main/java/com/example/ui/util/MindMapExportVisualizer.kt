package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.MindMapEntity
import com.example.data.model.MindNodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object MindMapExportVisualizer {

    private data class NodeDrawLayout(
        val node: MindNodeEntity,
        val depth: Int,
        val x: Float,
        val y: Float,
        val radius: Float,
        val angleDeg: Float
    )

    /**
     * Renders the mind map tree onto an Android Bitmap with full recursive depth support,
     * beautiful orbital pathways, sub-nodes, clear typography, and rich visual styling.
     * @param withBackground If true, fills with dark/light stylish background; if false, keeps transparent background.
     */
    suspend fun renderMapToBitmap(
        context: Context,
        map: MindMapEntity,
        nodes: List<MindNodeEntity>,
        rootNodeIdOverride: String? = null,
        withBackground: Boolean = true,
        isDark: Boolean = true,
        sizePx: Int = 2400
    ): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (withBackground) {
            val bgPaint = Paint().apply {
                isAntiAlias = true
                color = if (isDark) AndroidColor.parseColor("#0A0F1D") else AndroidColor.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

            // Draw multiple concentric subtle decorative celestial rings
            val ringPaint = Paint().apply {
                isAntiAlias = true
                color = if (isDark) AndroidColor.parseColor("#1E293B") else AndroidColor.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = sizePx * 0.0015f
            }
            val center = sizePx / 2f
            canvas.drawCircle(center, center, sizePx * 0.44f, ringPaint)
            canvas.drawCircle(center, center, sizePx * 0.33f, ringPaint)
            canvas.drawCircle(center, center, sizePx * 0.20f, ringPaint)
        } else {
            canvas.drawColor(AndroidColor.TRANSPARENT)
        }

        val effectiveRoot = if (!rootNodeIdOverride.isNullOrBlank()) {
            nodes.firstOrNull { it.id == rootNodeIdOverride }
        } else {
            nodes.firstOrNull { it.parentId == null }
        } ?: return@withContext bitmap

        val center = sizePx / 2f
        val layoutMap = mutableMapOf<String, NodeDrawLayout>()

        // 1. Root Layout
        val rootRadius = sizePx * 0.065f
        layoutMap[effectiveRoot.id] = NodeDrawLayout(
            node = effectiveRoot,
            depth = 0,
            x = center,
            y = center,
            radius = rootRadius,
            angleDeg = 0f
        )

        // 2. Recursive Orbital Layout Generator (Level 1, Level 2, Level 3+)
        val primaryChildren = nodes.filter { it.parentId == effectiveRoot.id }.sortedBy { it.orderIndex }
        val primaryCount = primaryChildren.size

        if (primaryCount > 0) {
            val stepAngle = 360f / primaryCount
            val level1Dist = sizePx * 0.23f
            val level1Radius = sizePx * 0.040f

            for (i in 0 until primaryCount) {
                val child = primaryChildren[i]
                val angleDeg = i * stepAngle
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val childX = center + level1Dist * cos(angleRad).toFloat()
                val childY = center + level1Dist * sin(angleRad).toFloat()

                layoutMap[child.id] = NodeDrawLayout(
                    node = child,
                    depth = 1,
                    x = childX,
                    y = childY,
                    radius = level1Radius,
                    angleDeg = angleDeg
                )

                // Layout grandchildren (Level 2)
                val grandchildren = nodes.filter { it.parentId == child.id }.sortedBy { it.orderIndex }
                val gCount = grandchildren.size
                if (gCount > 0) {
                    val gDist = sizePx * 0.12f
                    val gRadius = sizePx * 0.024f
                    val sweepSpan = min(120f, max(40f, gCount * 25f))
                    val startGAngle = angleDeg - (sweepSpan / 2f) + (sweepSpan / (2f * gCount))
                    val gStep = if (gCount == 1) 0f else (sweepSpan / (gCount - 1).coerceAtLeast(1))

                    for (j in 0 until gCount) {
                        val grand = grandchildren[j]
                        val gAngle = if (gCount == 1) angleDeg else (startGAngle + j * gStep)
                        val gRad = Math.toRadians(gAngle.toDouble())
                        val gx = childX + gDist * cos(gRad).toFloat()
                        val gy = childY + gDist * sin(gRad).toFloat()

                        layoutMap[grand.id] = NodeDrawLayout(
                            node = grand,
                            depth = 2,
                            x = gx,
                            y = gy,
                            radius = gRadius,
                            angleDeg = gAngle
                        )

                        // Layout great-grandchildren (Level 3+)
                        val level3Children = nodes.filter { it.parentId == grand.id }.sortedBy { it.orderIndex }
                        val l3Count = level3Children.size
                        if (l3Count > 0) {
                            val l3Dist = sizePx * 0.065f
                            val l3Radius = sizePx * 0.015f
                            val l3Sweep = min(90f, max(30f, l3Count * 20f))
                            val l3Start = gAngle - (l3Sweep / 2f)
                            val l3Step = if (l3Count == 1) 0f else (l3Sweep / (l3Count - 1).coerceAtLeast(1))

                            for (k in 0 until l3Count) {
                                val l3Node = level3Children[k]
                                val l3Angle = if (l3Count == 1) gAngle else (l3Start + k * l3Step)
                                val l3Rad = Math.toRadians(l3Angle.toDouble())
                                val l3x = gx + l3Dist * cos(l3Rad).toFloat()
                                val l3y = gy + l3Dist * sin(l3Rad).toFloat()

                                layoutMap[l3Node.id] = NodeDrawLayout(
                                    node = l3Node,
                                    depth = 3,
                                    x = l3x,
                                    y = l3y,
                                    radius = l3Radius,
                                    angleDeg = l3Angle
                                )

                                // Level 4 and beyond
                                layoutRemainingDescendants(l3Node, l3x, l3y, l3Angle, nodes, layoutMap, sizePx)
                            }
                        }
                    }
                }
            }
        }

        // Setup Connecting Lines
        val linePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        // Draw connections for all positioned nodes
        for (item in layoutMap.values) {
            val parentId = item.node.parentId ?: continue
            val parentLayout = layoutMap[parentId] ?: continue

            val nodeColor = try {
                AndroidColor.parseColor(item.node.colorHex)
            } catch (_: Exception) {
                if (isDark) AndroidColor.parseColor("#38BDF8") else AndroidColor.parseColor("#0284C7")
            }

            linePaint.color = nodeColor
            linePaint.strokeWidth = when (item.depth) {
                1 -> sizePx * 0.0035f
                2 -> sizePx * 0.0022f
                else -> sizePx * 0.0014f
            }
            linePaint.alpha = when (item.depth) {
                1 -> 200
                2 -> 160
                else -> 120
            }

            canvas.drawLine(parentLayout.x, parentLayout.y, item.x, item.y, linePaint)
        }

        // Draw Nodes
        val nodePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = AndroidColor.WHITE
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        // Draw all nodes in hierarchy order
        val sortedNodesToDraw = layoutMap.values.sortedBy { it.depth }
        for (item in sortedNodesToDraw) {
            val color = try {
                AndroidColor.parseColor(item.node.colorHex)
            } catch (_: Exception) {
                AndroidColor.parseColor("#3B82F6")
            }

            // Draw Node Circle
            nodePaint.color = color
            canvas.drawCircle(item.x, item.y, item.radius, nodePaint)

            // Draw White Ring Border
            borderPaint.strokeWidth = when (item.depth) {
                0 -> sizePx * 0.0045f
                1 -> sizePx * 0.0028f
                2 -> sizePx * 0.0018f
                else -> sizePx * 0.0012f
            }
            canvas.drawCircle(item.x, item.y, item.radius, borderPaint)

            // Draw Label
            val textSize = when (item.depth) {
                0 -> sizePx * 0.019f
                1 -> sizePx * 0.013f
                2 -> sizePx * 0.0095f
                else -> sizePx * 0.0075f
            }
            textPaint.textSize = textSize
            textPaint.color = AndroidColor.WHITE

            val maxChars = when (item.depth) {
                0 -> 16
                1 -> 14
                2 -> 11
                else -> 9
            }
            val titleTrunc = if (item.node.title.length > maxChars) item.node.title.take(maxChars - 2) + ".." else item.node.title
            canvas.drawText(titleTrunc, item.x, item.y + (textSize / 3f), textPaint)
        }

        // Header Title Banner
        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = sizePx * 0.026f
            color = if (isDark) AndroidColor.WHITE else AndroidColor.parseColor("#0F172A")
            textAlign = Paint.Align.LEFT
            isFakeBoldText = true
        }
        canvas.drawText(map.title, sizePx * 0.04f, sizePx * 0.055f, headerPaint)

        val subHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = sizePx * 0.014f
            color = if (isDark) AndroidColor.parseColor("#94A3B8") else AndroidColor.parseColor("#64748B")
            textAlign = Paint.Align.LEFT
        }
        val countText = "Madar Mind Maps • Complete Structure (${layoutMap.size} of ${nodes.size} nodes rendered)"
        canvas.drawText(countText, sizePx * 0.04f, sizePx * 0.078f, subHeaderPaint)

        bitmap
    }

    private fun layoutRemainingDescendants(
        parentNode: MindNodeEntity,
        parentX: Float,
        parentY: Float,
        baseAngle: Float,
        nodes: List<MindNodeEntity>,
        layoutMap: MutableMap<String, NodeDrawLayout>,
        sizePx: Int
    ) {
        val children = nodes.filter { it.parentId == parentNode.id }.sortedBy { it.orderIndex }
        if (children.isEmpty()) return

        val dist = sizePx * 0.04f
        val radSize = sizePx * 0.010f
        val count = children.size
        val span = min(60f, max(20f, count * 15f))
        val start = baseAngle - (span / 2f)
        val step = if (count == 1) 0f else (span / (count - 1).coerceAtLeast(1))

        for (i in 0 until count) {
            val child = children[i]
            val angle = if (count == 1) baseAngle else (start + i * step)
            val rad = Math.toRadians(angle.toDouble())
            val cx = parentX + dist * cos(rad).toFloat()
            val cy = parentY + dist * sin(rad).toFloat()

            layoutMap[child.id] = NodeDrawLayout(
                node = child,
                depth = 4,
                x = cx,
                y = cy,
                radius = radSize,
                angleDeg = angle
            )

            layoutRemainingDescendants(child, cx, cy, angle, nodes, layoutMap, sizePx)
        }
    }

    /**
     * Saves bitmap to a PNG file and returns the share Uri.
     */
    suspend fun saveBitmapToCacheAndGetUri(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Uri = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(exportsDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Generates a rich multi-page vector PDF document representing the entire mind map with complete
     * fidelity, covering every single node, note, task, and link across dynamically generated pages.
     */
    suspend fun generatePdf(
        context: Context,
        map: MindMapEntity,
        nodes: List<MindNodeEntity>,
        rootNodeIdOverride: String? = null,
        isDark: Boolean = false
    ): Uri = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageWidth = 1200
        val pageHeight = 1600

        // ----------------------------------------------------
        // Page 1: High Resolution Visual Radial Mind Map
        // ----------------------------------------------------
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        val visualBitmap = renderMapToBitmap(
            context = context,
            map = map,
            nodes = nodes,
            rootNodeIdOverride = rootNodeIdOverride,
            withBackground = true,
            isDark = false, // Clean white professional document aesthetic
            sizePx = 1400
        )

        val bgPaint = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        canvas1.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Draw Header Title & Description
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 34f
            color = AndroidColor.parseColor("#0F172A")
            isFakeBoldText = true
        }
        canvas1.drawText(map.title, 60f, 80f, titlePaint)

        val subtitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 17f
            color = AndroidColor.parseColor("#64748B")
        }
        val effectiveNodesCount = if (!rootNodeIdOverride.isNullOrBlank()) {
            OrbitMindBackupManager.extractSubtreeNodes(rootNodeIdOverride, nodes).size
        } else {
            nodes.size
        }
        canvas1.drawText("Madar Mind Maps • Complete Structure ($effectiveNodesCount nodes, tasks & notes)", 60f, 115f, subtitlePaint)

        if (map.description.isNotBlank()) {
            val descPaint = Paint().apply {
                isAntiAlias = true
                textSize = 15f
                color = AndroidColor.parseColor("#475569")
            }
            canvas1.drawText(map.description, 60f, 145f, descPaint)
        }

        // Draw Bitmap Diagram
        val destRect = RectF(60f, 175f, 1140f, 1480f)
        canvas1.drawBitmap(visualBitmap, null, destRect, null)

        // Page 1 Footer
        val footerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 14f
            color = AndroidColor.parseColor("#94A3B8")
            textAlign = Paint.Align.CENTER
        }
        canvas1.drawText("Madar Mind Maps • Page 1 • Orbital Overview Diagram", pageWidth / 2f, 1550f, footerPaint)
        pdfDocument.finishPage(page1)

        // ----------------------------------------------------
        // Multi-Page Structured Tree Index & Content Pages
        // ----------------------------------------------------
        val effectiveRoot = if (!rootNodeIdOverride.isNullOrBlank()) {
            nodes.firstOrNull { it.id == rootNodeIdOverride }
        } else {
            nodes.firstOrNull { it.parentId == null }
        }

        var currentPageNumber = 2
        var currentPage: PdfDocument.Page? = null
        var currentCanvas: Canvas? = null
        var currentY = 120f

        val h1Paint = Paint().apply {
            isAntiAlias = true
            textSize = 26f
            color = AndroidColor.parseColor("#0F172A")
            isFakeBoldText = true
        }

        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 16f
            color = AndroidColor.parseColor("#1E293B")
        }

        val notePaint = Paint().apply {
            isAntiAlias = true
            textSize = 13.5f
            color = AndroidColor.parseColor("#475569")
        }

        val taskPaint = Paint().apply {
            isAntiAlias = true
            textSize = 13.5f
            color = AndroidColor.parseColor("#047857")
        }

        val linkPaint = Paint().apply {
            isAntiAlias = true
            textSize = 13f
            color = AndroidColor.parseColor("#0284C7")
        }

        fun startNewPdfPage(): Canvas {
            currentPage?.let {
                currentCanvas?.drawText(
                    "Madar Mind Maps • Page $currentPageNumber • Complete Hierarchical Index",
                    pageWidth / 2f,
                    1550f,
                    footerPaint
                )
                pdfDocument.finishPage(it)
                currentPageNumber++
            }
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            val newPage = pdfDocument.startPage(pageInfo)
            val canvas = newPage.canvas
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            canvas.drawText("Full Mind Map Outline & Details", 60f, 75f, h1Paint)
            currentY = 120f
            currentPage = newPage
            currentCanvas = canvas
            return canvas
        }

        var activeCanvas = startNewPdfPage()

        fun printNodeToPdfRecursive(node: MindNodeEntity, depth: Int) {
            if (currentY > pageHeight - 120f) {
                activeCanvas = startNewPdfPage()
            }

            val indent = 60f + (depth * 28f).coerceAtMost(320f)
            val bullet = when (depth) {
                0 -> "● "
                1 -> "○ "
                2 -> "▪ "
                else -> "▫ "
            }

            bodyPaint.isFakeBoldText = depth <= 1
            bodyPaint.color = try {
                AndroidColor.parseColor(node.colorHex)
            } catch (_: Exception) {
                AndroidColor.parseColor("#1E293B")
            }

            activeCanvas.drawText("$bullet${node.title}", indent, currentY, bodyPaint)
            currentY += 24f

            // Notes
            if (node.notes.isNotBlank()) {
                node.notes.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        if (currentY > pageHeight - 120f) {
                            activeCanvas = startNewPdfPage()
                        }
                        activeCanvas.drawText("   | $line", indent + 16f, currentY, notePaint)
                        currentY += 19f
                    }
                }
            }

            // Tasks / Checklist
            if (node.checklist.isNotEmpty()) {
                node.checklist.forEach { task ->
                    if (currentY > pageHeight - 120f) {
                        activeCanvas = startNewPdfPage()
                    }
                    val mark = if (task.isDone) "[✓]" else "[ ]"
                    activeCanvas.drawText("   $mark ${task.text}", indent + 16f, currentY, taskPaint)
                    currentY += 19f
                }
            }

            // Web Link
            if (node.linkUrl.isNotBlank()) {
                if (currentY > pageHeight - 120f) {
                    activeCanvas = startNewPdfPage()
                }
                activeCanvas.drawText("   🔗 ${node.linkUrl}", indent + 16f, currentY, linkPaint)
                currentY += 19f
            }

            currentY += 6f

            // Recursive children
            val children = nodes.filter { it.parentId == node.id }.sortedBy { it.orderIndex }
            for (c in children) {
                printNodeToPdfRecursive(c, depth + 1)
            }
        }

        effectiveRoot?.let { printNodeToPdfRecursive(it, 0) }

        // Finish the last page
        currentPage?.let {
            currentCanvas?.drawText(
                "Madar Mind Maps • Page $currentPageNumber • Complete Hierarchical Index",
                pageWidth / 2f,
                1550f,
                footerPaint
            )
            pdfDocument.finishPage(it)
        }

        // Save PDF to cache file
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanName = OrbitMindBackupManager.generateExportFileName(map.title, false, ".pdf")
        val targetFile = File(exportsDir, cleanName)

        FileOutputStream(targetFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
    }
}

