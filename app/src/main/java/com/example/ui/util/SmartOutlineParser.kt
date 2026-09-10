package com.example.ui.util

import com.example.data.model.ChecklistItem
import com.example.data.model.MindNodeEntity
import java.util.UUID

data class ParsedOutlineResult(
    val mapTitle: String,
    val rootTitle: String,
    val nodes: List<MindNodeEntity>
)

data class RawOutlineItem(
    val depth: Int,
    val title: String,
    val notes: String = "",
    val checklist: List<ChecklistItem> = emptyList(),
    val progress: Int? = null,
    val impact: Int? = null,
    val iconName: String = "lightbulb"
)

object SmartOutlineParser {

    private val PALETTE_COLORS = listOf(
        "#3B82F6", // Celestial Blue
        "#8B5CF6", // Nebula Purple
        "#06B6D4", // Cyan Orbit
        "#10B981", // Emerald Oasis
        "#F59E0B", // Solar Flare Amber
        "#EC4899", // Magenta Comet
        "#6366F1", // Indigo Galaxy
        "#14B8A6", // Teal Starlight
        "#F97316", // Supernova Orange
        "#84CC16"  // Aurora Lime
    )

    /**
     * Parses raw multiline text into a structured Mind Map with root and orbiting nodes.
     */
    fun parseTextToMindMap(
        rawText: String,
        customMapTitle: String? = null,
        targetMapId: Long = 0L
    ): ParsedOutlineResult {
        val lines = rawText.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            val rootId = UUID.randomUUID().toString()
            val defaultTitle = customMapTitle?.ifBlank { "خريطة جديدة" } ?: "خريطة جديدة"
            val rootNode = MindNodeEntity(
                id = rootId,
                mapId = targetMapId,
                parentId = null,
                title = defaultTitle,
                colorHex = PALETTE_COLORS[0],
                iconName = "stars"
            )
            return ParsedOutlineResult(defaultTitle, defaultTitle, listOf(rootNode))
        }

        val rawItems = mutableListOf<RawOutlineItem>()

        for (line in lines) {
            val parsedItem = parseSingleLine(line) ?: continue
            rawItems.add(parsedItem)
        }

        if (rawItems.isEmpty()) {
            val rootId = UUID.randomUUID().toString()
            val defaultTitle = customMapTitle?.ifBlank { "خريطة جديدة" } ?: "خريطة جديدة"
            val rootNode = MindNodeEntity(
                id = rootId,
                mapId = targetMapId,
                parentId = null,
                title = defaultTitle,
                colorHex = PALETTE_COLORS[0],
                iconName = "stars"
            )
            return ParsedOutlineResult(defaultTitle, defaultTitle, listOf(rootNode))
        }

        // Determine Root Title and Map Title
        val firstItem = rawItems.first()
        val hasExplicitSingleRoot = rawItems.count { it.depth == rawItems.minOf { m -> m.depth } } == 1
        
        val mapTitle: String
        val rootTitle: String
        val itemsToProcess: List<RawOutlineItem>

        if (hasExplicitSingleRoot && firstItem.depth == rawItems.minOf { m -> m.depth }) {
            rootTitle = firstItem.title
            mapTitle = customMapTitle?.ifBlank { rootTitle } ?: rootTitle
            itemsToProcess = rawItems.drop(1)
        } else {
            rootTitle = customMapTitle?.ifBlank { firstItem.title } ?: firstItem.title
            mapTitle = rootTitle
            itemsToProcess = rawItems
        }

        val rootId = UUID.randomUUID().toString()
        val rootNode = MindNodeEntity(
            id = rootId,
            mapId = targetMapId,
            parentId = null,
            title = rootTitle,
            colorHex = PALETTE_COLORS[0],
            iconName = detectIconForTitle(rootTitle, isRoot = true),
            notes = if (hasExplicitSingleRoot && firstItem.notes.isNotBlank()) firstItem.notes else ""
        )

        val generatedNodes = mutableListOf<MindNodeEntity>()
        generatedNodes.add(rootNode)

        // Stack to track active parent at each hierarchy level: Pair(depth, nodeId, colorHex)
        val stack = mutableListOf<Triple<Int, String, String>>()
        stack.add(Triple(-1, rootId, PALETTE_COLORS[0]))

        var topLevelBranchIndex = 0

        for ((index, item) in itemsToProcess.withIndex()) {
            val itemDepth = item.depth
            
            // Pop stack items that are deeper than or equal to current item depth
            while (stack.size > 1 && stack.last().first >= itemDepth) {
                stack.removeAt(stack.size - 1)
            }

            val parent = stack.last()
            val parentId = parent.second
            val isDirectChildOfRoot = (parentId == rootId)

            val branchColor = if (isDirectChildOfRoot) {
                val color = PALETTE_COLORS[topLevelBranchIndex % PALETTE_COLORS.size]
                topLevelBranchIndex++
                color
            } else {
                parent.third
            }

            val nodeId = UUID.randomUUID().toString()
            val nodeEntity = MindNodeEntity(
                id = nodeId,
                mapId = targetMapId,
                parentId = parentId,
                title = item.title,
                notes = item.notes,
                checklistJson = ChecklistItem.listToJson(item.checklist),
                colorHex = branchColor,
                iconName = item.iconName.ifBlank { detectIconForTitle(item.title, isRoot = false) },
                progress = item.progress,
                impact = item.impact,
                orderIndex = index
            )

            generatedNodes.add(nodeEntity)
            stack.add(Triple(itemDepth, nodeId, branchColor))
        }

        return ParsedOutlineResult(
            mapTitle = mapTitle,
            rootTitle = rootTitle,
            nodes = generatedNodes
        )
    }

    private fun parseSingleLine(rawLine: String): RawOutlineItem? {
        if (rawLine.isBlank()) return null

        // 1. Calculate Indentation Depth
        var leadingSpaces = 0
        var i = 0
        while (i < rawLine.length && (rawLine[i] == ' ' || rawLine[i] == '\t')) {
            if (rawLine[i] == '\t') leadingSpaces += 4 else leadingSpaces += 1
            i++
        }
        var baseDepth = leadingSpaces / 2 // every 2 spaces is a depth level

        val trimmed = rawLine.substring(i).trim()
        if (trimmed.isBlank()) return null

        var content = trimmed
        var detectedDepth = baseDepth
        val checklist = mutableListOf<ChecklistItem>()

        // 2. Check for Markdown Headers (#, ##, ###)
        if (content.startsWith("#")) {
            val hashCount = content.takeWhile { it == '#' }.length
            if (content.length > hashCount && content[hashCount] == ' ') {
                detectedDepth = hashCount - 1
                content = content.substring(hashCount).trim()
            }
        }

        // 3. Check for Checklists: [ ] or [x]
        val taskRegex = Regex("""^\[([ xXvV✓✔])\]\s*(.*)""")
        val taskMatch = taskRegex.find(content)
        if (taskMatch != null) {
            val mark = taskMatch.groupValues[1]
            val taskText = taskMatch.groupValues[2]
            val isDone = mark.isNotBlank() && mark != " "
            checklist.add(ChecklistItem(text = taskText, isDone = isDone))
            content = taskText
        }

        // 4. Check for Bullet Points (-, *, +, •, ⁃, ◦, ▪, ▫, –, —, ✦, ★)
        val bulletRegex = Regex("""^([\-\*\+•⁃◦▪▫–—✦★✓✔])\s+(.*)""")
        val bulletMatch = bulletRegex.find(content)
        if (bulletMatch != null) {
            content = bulletMatch.groupValues[2].trim()
        }

        // 5. Check for Numbered Outlines (1., 1.1, 1.1.1, 1-, أ-, (أ), etc.)
        val numberedDotRegex = Regex("""^(\d+(\.\d+)+)\.?\s+(.*)""")
        val numberedDotMatch = numberedDotRegex.find(content)
        if (numberedDotMatch != null) {
            val numPrefix = numberedDotMatch.groupValues[1]
            val dotCount = numPrefix.count { it == '.' }
            detectedDepth = maxOf(detectedDepth, dotCount)
            content = numberedDotMatch.groupValues[3].trim()
        } else {
            val simpleNumRegex = Regex("""^(\d+|[أ-يa-zA-Z]|\([أ-يa-zA-Z\d]+\))[\.\-\)]\s+(.*)""")
            val simpleNumMatch = simpleNumRegex.find(content)
            if (simpleNumMatch != null) {
                content = simpleNumMatch.groupValues[2].trim()
            }
        }

        // 6. Separate Title from inline Notes/Descriptions (e.g. "Title: Description" or "Title - Description")
        var title = content
        var notes = ""

        if (content.contains(" : ") || content.contains(": ")) {
            val parts = if (content.contains(" : ")) content.split(" : ", limit = 2) else content.split(": ", limit = 2)
            if (parts.size == 2 && parts[0].length in 2..50 && parts[1].length > 5) {
                title = parts[0].trim()
                notes = parts[1].trim()
            }
        }

        if (title.isBlank()) return null

        val iconName = detectIconForTitle(title, isRoot = false)

        return RawOutlineItem(
            depth = detectedDepth,
            title = title,
            notes = notes,
            checklist = checklist,
            iconName = iconName
        )
    }

    /**
     * Smart Keyword Icon Matcher (Arabic & English)
     */
    fun detectIconForTitle(title: String, isRoot: Boolean = false): String {
        if (isRoot) return "stars"
        val lower = title.lowercase()

        return when {
            lower.contains("هدف") || lower.contains("أهداف") || lower.contains("goal") || lower.contains("target") -> "flag"
            lower.contains("تسويق") || lower.contains("مبيعات") || lower.contains("marketing") || lower.contains("sales") || lower.contains("اعلان") -> "campaign"
            lower.contains("برمج") || lower.contains("تطوير") || lower.contains("كود") || lower.contains("code") || lower.contains("dev") || lower.contains("tech") -> "code"
            lower.contains("تصميم") || lower.contains("واجه") || lower.contains("ui") || lower.contains("ux") || lower.contains("design") -> "palette"
            lower.contains("مالي") || lower.contains("ميزاني") || lower.contains("سعر") || lower.contains("تكلف") || lower.contains("finance") || lower.contains("money") -> "attach_money"
            lower.contains("مهم") || lower.contains("مهام") || lower.contains("خطة") || lower.contains("task") || lower.contains("plan") || lower.contains("todo") -> "checklist"
            lower.contains("فكر") || lower.contains("أفكار") || lower.contains("ابداع") || lower.contains("ابتكار") || lower.contains("idea") -> "lightbulb"
            lower.contains("بحث") || lower.contains("دراس") || lower.contains("تحليل") || lower.contains("study") || lower.contains("research") -> "science"
            lower.contains("فريق") || lower.contains("اجتماع") || lower.contains("تواصل") || lower.contains("meeting") || lower.contains("team") || lower.contains("user") -> "groups"
            lower.contains("وقت") || lower.contains("جدول") || lower.contains("تاريخ") || lower.contains("time") || lower.contains("date") || lower.contains("schedule") -> "schedule"
            lower.contains("صحة") || lower.contains("رياض") || lower.contains("health") || lower.contains("fitness") -> "favorite"
            lower.contains("كتاب") || lower.contains("قراء") || lower.contains("ملف") || lower.contains("book") || lower.contains("doc") -> "menu_book"
            lower.contains("أمن") || lower.contains("حماي") || lower.contains("security") || lower.contains("shield") -> "shield"
            lower.contains("نجم") || lower.contains("مهم") || lower.contains("star") || lower.contains("vip") -> "star"
            else -> "lightbulb"
        }
    }
}
