package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ThemeMode
import com.example.data.ThemePreferences
import com.example.data.db.AppDatabase
import com.example.data.model.ChecklistItem
import com.example.data.model.MindMapEntity
import com.example.data.model.MindNodeEntity
import com.example.data.repository.MindMapRepository
import com.example.ui.util.AutoSyncManager
import com.example.ui.util.PinnedNodeShortcut
import com.example.ui.util.ShortcutHelper
import com.example.ui.util.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MindMapViewModel(application: Application) : AndroidViewModel(application) {

    private val themePreferences = ThemePreferences(application)
    private val database = AppDatabase.getInstance(application)
    private val repository = MindMapRepository(database.mindMapDao(), database.mindNodeDao())
    private val autoSyncManager = AutoSyncManager(application, repository)
    private var nodesJob: Job? = null

    val isAutoSyncEnabled: StateFlow<Boolean> = autoSyncManager.isAutoSyncEnabled
    val syncStatus: StateFlow<SyncStatus> = autoSyncManager.syncStatus
    val lastSyncTimestamp: StateFlow<Long> = autoSyncManager.lastSyncTimestamp

    fun setAutoSyncEnabled(enabled: Boolean) {
        autoSyncManager.setAutoSyncEnabled(enabled)
    }

    fun triggerManualSync(onComplete: ((Boolean, String?) -> Unit)? = null) {
        autoSyncManager.triggerSyncImmediate(onComplete)
    }

    fun checkAndSyncWithDrive(onComplete: ((Boolean, String?) -> Unit)? = null) {
        autoSyncManager.checkAndPullRemoteChangesIfNewer(onComplete)
    }

    fun scheduleAutoSync(delayMs: Long = 5000L) {
        autoSyncManager.triggerSyncDebounced(delayMs)
    }

    private val _themeMode = MutableStateFlow(themePreferences.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    val allMaps: StateFlow<List<MindMapEntity>> = repository.allMaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mapNodeCounts: StateFlow<Map<Long, Int>> = repository.nodeCountsPerMap
        .map { list -> list.associate { it.mapId to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalNodesCount: StateFlow<Int> = repository.totalNodesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _activeMapId = MutableStateFlow<Long?>(null)
    val activeMapId: StateFlow<Long?> = _activeMapId.asStateFlow()

    private val _currentCentralNodeId = MutableStateFlow<String?>(null)
    val currentCentralNodeId: StateFlow<String?> = _currentCentralNodeId.asStateFlow()

    private val _allNodes = MutableStateFlow<List<MindNodeEntity>>(emptyList())
    val allNodes: StateFlow<List<MindNodeEntity>> = _allNodes.asStateFlow()

    private val _orbitRotationAngle = MutableStateFlow(0f)
    val orbitRotationAngle: StateFlow<Float> = _orbitRotationAngle.asStateFlow()

    // Dialog & mode states
    private val _selectedNodeForEdit = MutableStateFlow<MindNodeEntity?>(null)
    val selectedNodeForEdit: StateFlow<MindNodeEntity?> = _selectedNodeForEdit.asStateFlow()

    private val _selectedNodeForImageViewer = MutableStateFlow<MindNodeEntity?>(null)
    val selectedNodeForImageViewer: StateFlow<MindNodeEntity?> = _selectedNodeForImageViewer.asStateFlow()

    private val _isAddNodeOpen = MutableStateFlow(false)
    val isAddNodeOpen: StateFlow<Boolean> = _isAddNodeOpen.asStateFlow()

    private val _isCreateMapOpen = MutableStateFlow(false)
    val isCreateMapOpen: StateFlow<Boolean> = _isCreateMapOpen.asStateFlow()

    private val _isOutlineOpen = MutableStateFlow(false)
    val isOutlineOpen: StateFlow<Boolean> = _isOutlineOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // 🪐 Orbital Pocket / Clipboard State (حلقة الجيب المداري للنقل والمزامنة)
    private val _pocketNode = MutableStateFlow<MindNodeEntity?>(null)
    val pocketNode: StateFlow<MindNodeEntity?> = _pocketNode.asStateFlow()

    private val _isPocketActionDialogOpen = MutableStateFlow(false)
    val isPocketActionDialogOpen: StateFlow<Boolean> = _isPocketActionDialogOpen.asStateFlow()

    // 🎯 Node-to-Node Drop Target State (الإسقاط المباشر بين العقد)
    data class DropTargetAction(
        val sourceNode: MindNodeEntity,
        val targetNode: MindNodeEntity,
        val isDirectDrop: Boolean = true
    )

    private val _dropTargetAction = MutableStateFlow<DropTargetAction?>(null)
    val dropTargetAction: StateFlow<DropTargetAction?> = _dropTargetAction.asStateFlow()

    // Quick Action Menu on Node Long Press
    private val _nodeForQuickActions = MutableStateFlow<MindNodeEntity?>(null)
    val nodeForQuickActions: StateFlow<MindNodeEntity?> = _nodeForQuickActions.asStateFlow()

    // 🚀 Export & Backup State
    private val _exportTargetMap = MutableStateFlow<MindMapEntity?>(null)
    val exportTargetMap: StateFlow<MindMapEntity?> = _exportTargetMap.asStateFlow()

    private val _exportTargetNodes = MutableStateFlow<List<MindNodeEntity>>(emptyList())
    val exportTargetNodes: StateFlow<List<MindNodeEntity>> = _exportTargetNodes.asStateFlow()

    private val _isExportDialogOpen = MutableStateFlow(false)
    val isExportDialogOpen: StateFlow<Boolean> = _isExportDialogOpen.asStateFlow()

    // 📥 Import & Backup Center State
    private val _isImportDialogOpen = MutableStateFlow(false)
    val isImportDialogOpen: StateFlow<Boolean> = _isImportDialogOpen.asStateFlow()

    // 📌 Pinned Node Shortcuts & Universes (Home Screen & In-App Quick Access)
    private val _pinnedShortcuts = MutableStateFlow<List<PinnedNodeShortcut>>(ShortcutHelper.getPinnedShortcuts(application))
    val pinnedShortcuts: StateFlow<List<PinnedNodeShortcut>> = _pinnedShortcuts.asStateFlow()

    private val _pinnedMapIds = MutableStateFlow<Set<Long>>(ShortcutHelper.getPinnedMapIds(application))
    val pinnedMapIds: StateFlow<Set<Long>> = _pinnedMapIds.asStateFlow()

    private val _progressSortDescending = MutableStateFlow(true)
    val progressSortDescending: StateFlow<Boolean> = _progressSortDescending.asStateFlow()

    private val _impactSortDescending = MutableStateFlow(true)
    val impactSortDescending: StateFlow<Boolean> = _impactSortDescending.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        // Sync OS shortcuts on initialization
        ShortcutHelper.syncAllShortcuts(application)

        viewModelScope.launch {
            // Seed starter maps on first launch
            val initialMaps = withContext(Dispatchers.IO) {
                database.mindMapDao().getAllMaps()
            }
            initialMaps.collect { maps ->
                if (maps.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        repository.seedStarterMaps()
                    }
                }
            }
        }

        // Check if there are remote updates on Google Drive on startup
        checkAndSyncWithDrive()
    }

    fun selectMap(mapId: Long, targetNodeId: String? = null) {
        _activeMapId.value = mapId
        _orbitRotationAngle.value = 0f
        _pocketNode.value = null
        if (targetNodeId != null) {
            _currentCentralNodeId.value = targetNodeId
        }
        nodesJob?.cancel()
        nodesJob = viewModelScope.launch {
            var initialTargetApplied = (targetNodeId == null)
            repository.getAllNodesForMap(mapId).collect { nodes ->
                _allNodes.value = nodes
                val rootNode = nodes.firstOrNull { it.parentId == null }
                if (targetNodeId != null && !initialTargetApplied && nodes.any { it.id == targetNodeId }) {
                    _currentCentralNodeId.value = targetNodeId
                    initialTargetApplied = true
                } else {
                    val currentId = _currentCentralNodeId.value
                    if (currentId == null || nodes.none { it.id == currentId }) {
                        _currentCentralNodeId.value = rootNode?.id
                    }
                }
            }
        }
    }

    fun openNodeFromShortcut(mapId: Long, nodeId: String?) {
        selectMap(mapId, nodeId)
        viewModelScope.launch {
            val map = repository.getMapByIdSync(mapId)
            val nodes = repository.getAllNodesForMapSync(mapId)
            val node = if (nodeId != null) nodes.firstOrNull { it.id == nodeId } else null
            val nodeTitle = node?.title ?: map?.title ?: "العقدة"
            _userFeedbackMessage.value = "تم فتح $nodeTitle مباشرة عبر الاختصار"
        }
    }

    fun pinNodeShortcut(map: MindMapEntity?, node: MindNodeEntity) {
        val updated = ShortcutHelper.savePinnedShortcut(getApplication(), map, node)
        _pinnedShortcuts.value = updated
        ShortcutHelper.createNodeShortcut(getApplication(), map, node)
    }

    fun pinNodeShortcutWithTarget(
        map: MindMapEntity?,
        node: MindNodeEntity,
        target: com.example.ui.dialogs.ShortcutTargetType
    ) {
        when (target) {
            com.example.ui.dialogs.ShortcutTargetType.IN_APP_HOME -> {
                val updated = ShortcutHelper.saveInAppShortcutOnly(getApplication(), map, node)
                _pinnedShortcuts.value = updated
                _userFeedbackMessage.value = "تم تثبيت الاختصار في الصفحة الرئيسية للتطبيق 📌"
            }
            com.example.ui.dialogs.ShortcutTargetType.DEVICE_LAUNCHER -> {
                ShortcutHelper.createLauncherOnlyShortcut(getApplication(), map, node)
                _userFeedbackMessage.value = "تمت إضافة الاختصار لشاشة الموبايل الرئيسية 📱"
            }
            com.example.ui.dialogs.ShortcutTargetType.BOTH -> {
                val updated = ShortcutHelper.savePinnedShortcut(getApplication(), map, node)
                _pinnedShortcuts.value = updated
                ShortcutHelper.createNodeShortcut(getApplication(), map, node)
                _userFeedbackMessage.value = "تم تثبيت الاختصار في التطبيق وعلى شاشة الهاتف 🌟"
            }
        }
    }

    fun togglePinShortcut(mapId: Long, nodeId: String) {
        val updated = ShortcutHelper.togglePinShortcut(getApplication(), mapId, nodeId)
        _pinnedShortcuts.value = updated
        val item = updated.firstOrNull { it.mapId == mapId && it.nodeId == nodeId }
        _userFeedbackMessage.value = if (item?.isPinned == true) "تم تثبيت الاختصار في المقدمة 📌" else "تم إلغاء تثبيت الاختصار"
    }

    fun unpinNodeShortcut(mapId: Long, nodeId: String) {
        val updated = ShortcutHelper.removePinnedShortcut(getApplication(), mapId, nodeId)
        _pinnedShortcuts.value = updated
        _userFeedbackMessage.value = "تمت إزالة الاختصار"
    }

    fun togglePinMap(mapId: Long) {
        val isNowPinned = ShortcutHelper.togglePinMap(getApplication(), mapId)
        _pinnedMapIds.value = ShortcutHelper.getPinnedMapIds(getApplication())
        _userFeedbackMessage.value = if (isNowPinned) "تم تثبيت العالم في المقدمة 📌" else "تم إلغاء تثبيت العالم"
    }

    fun refreshPinnedShortcuts() {
        _pinnedShortcuts.value = ShortcutHelper.getPinnedShortcuts(getApplication())
        _pinnedMapIds.value = ShortcutHelper.getPinnedMapIds(getApplication())
    }

    fun navigateBackToMapList() {
        nodesJob?.cancel()
        nodesJob = null
        _activeMapId.value = null
        _currentCentralNodeId.value = null
        _allNodes.value = emptyList()
        _searchQuery.value = ""
        _isSearchActive.value = false
        _pocketNode.value = null
    }

    fun diveIntoNode(nodeId: String) {
        _currentCentralNodeId.value = nodeId
        _orbitRotationAngle.value = 0f
    }

    fun navigateToOriginalNode(originalId: String) {
        val targetNode = _allNodes.value.firstOrNull { it.id == originalId }
        _currentCentralNodeId.value = originalId
        _orbitRotationAngle.value = 0f
        if (targetNode != null) {
            _userFeedbackMessage.value = "تم الانتقال إلى العقدة الأصلية: ${targetNode.title}"
        }
    }

    fun navigateUpLevel() {
        val currentId = _currentCentralNodeId.value ?: return
        val currentNode = _allNodes.value.firstOrNull { it.id == currentId }
        if (currentNode?.parentId != null) {
            _currentCentralNodeId.value = currentNode.parentId
            _orbitRotationAngle.value = 0f
        } else {
            // If already at root, return to map list
            navigateBackToMapList()
        }
    }

    fun navigateToBreadcrumb(nodeId: String) {
        _currentCentralNodeId.value = nodeId
        _orbitRotationAngle.value = 0f
    }

    fun rotateOrbitBy(deltaDegrees: Float) {
        _orbitRotationAngle.value = (_orbitRotationAngle.value + deltaDegrees) % 360f
    }

    fun resetOrbitRotation() {
        _orbitRotationAngle.value = 0f
        _userFeedbackMessage.value = "تمت إعادة ضبط المدار 🔄"
    }

    /**
     * Sorts children of the current central node by rating / progress, toggling between descending and ascending.
     */
    fun toggleSortSiblingsByProgress() {
        val nextDescending = !_progressSortDescending.value
        _progressSortDescending.value = nextDescending
        sortSiblingsByProgress(nextDescending)
    }

    fun sortSiblingsByProgress(descending: Boolean = true) {
        _progressSortDescending.value = descending
        val mapId = _activeMapId.value ?: return
        val currentParentId = _currentCentralNodeId.value
        val siblings = _allNodes.value.filter { it.parentId == currentParentId }
        if (siblings.size <= 1) return

        val sorted = if (descending) {
            siblings.sortedWith(compareByDescending<MindNodeEntity> { it.progress ?: -1 }.thenBy { it.title })
        } else {
            siblings.sortedWith(compareBy<MindNodeEntity> { it.progress ?: 999 }.thenBy { it.title })
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.reorderSiblingNodes(sorted.map { it.id }, mapId)
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                val orderLabel = if (descending) "من الأعلى للأقل ⬇️" else "من الأقل للأعلى ⬆️"
                _userFeedbackMessage.value = "تم ترتيب العقد حسب التقييم ($orderLabel)"
            }
        }
    }

    /**
     * Sorts children of the current central node by impact level, toggling between descending and ascending.
     */
    fun toggleSortSiblingsByImpact() {
        val nextDescending = !_impactSortDescending.value
        _impactSortDescending.value = nextDescending
        sortSiblingsByImpact(nextDescending)
    }

    fun sortSiblingsByImpact(descending: Boolean = true) {
        _impactSortDescending.value = descending
        val mapId = _activeMapId.value ?: return
        val currentParentId = _currentCentralNodeId.value
        val siblings = _allNodes.value.filter { it.parentId == currentParentId }
        if (siblings.size <= 1) return

        val sorted = if (descending) {
            siblings.sortedWith(compareByDescending<MindNodeEntity> { it.impact ?: 0 }.thenBy { it.title })
        } else {
            siblings.sortedWith(compareBy<MindNodeEntity> { it.impact ?: 999 }.thenBy { it.title })
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.reorderSiblingNodes(sorted.map { it.id }, mapId)
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                val orderLabel = if (descending) "من الأعلى للأقل ⬇️" else "من الأقل للأعلى ⬆️"
                _userFeedbackMessage.value = "تم ترتيب العقد حسب التأثير ($orderLabel)"
            }
        }
    }

    // Node operations
    fun openAddNodeDialog() {
        _isAddNodeOpen.value = true
    }

    fun closeAddNodeDialog() {
        _isAddNodeOpen.value = false
    }

    fun createChildNode(
        title: String,
        colorHex: String,
        iconName: String,
        notes: String = "",
        checklist: List<ChecklistItem> = emptyList(),
        linkUrl: String = "",
        imageUri: String? = null,
        targetParentId: String? = null
    ) {
        val mapId = _activeMapId.value ?: return
        val currentId = _currentCentralNodeId.value
        val parentId = targetParentId ?: currentId ?: _allNodes.value.firstOrNull { it.parentId == null }?.id ?: return
        _currentCentralNodeId.value = parentId

        viewModelScope.launch(Dispatchers.IO) {
            val newNode = repository.addNode(
                mapId = mapId,
                parentId = parentId,
                title = title.trim(),
                colorHex = colorHex,
                iconName = iconName,
                notes = notes.trim(),
                checklist = checklist,
                linkUrl = linkUrl.trim(),
                imageUri = imageUri
            )
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                _currentCentralNodeId.value = parentId
                if (!_allNodes.value.any { it.id == newNode.id }) {
                    _allNodes.value = _allNodes.value + newNode
                }
            }
        }
        _isAddNodeOpen.value = false
    }

    fun openEditNodeDialog(node: MindNodeEntity) {
        _selectedNodeForEdit.value = node
        _nodeForQuickActions.value = null
    }

    fun closeEditNodeDialog() {
        _selectedNodeForEdit.value = null
    }

    fun openImageViewer(node: MindNodeEntity) {
        _selectedNodeForImageViewer.value = node
        _nodeForQuickActions.value = null
    }

    fun closeImageViewer() {
        _selectedNodeForImageViewer.value = null
    }

    fun openQuickActions(node: MindNodeEntity) {
        _nodeForQuickActions.value = node
    }

    fun closeQuickActions() {
        _nodeForQuickActions.value = null
    }

    fun saveNodeEdits(
        id: String,
        title: String,
        colorHex: String,
        iconName: String,
        notes: String,
        checklist: List<ChecklistItem>,
        linkUrl: String,
        imageUri: String? = null,
        progress: Int? = null,
        impact: Int? = null,
        dueDate: Long? = null
    ) {
        val existing = _allNodes.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = existing.copy(
                title = title.trim(),
                colorHex = colorHex,
                iconName = iconName,
                notes = notes.trim(),
                checklistJson = ChecklistItem.listToJson(checklist),
                linkUrl = linkUrl.trim(),
                imageUri = imageUri,
                progress = progress,
                impact = impact,
                dueDate = dueDate,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNode(updated)
            scheduleAutoSync()
        }
        _selectedNodeForEdit.value = null
    }

    fun deleteNode(nodeId: String) {
        val mapId = _activeMapId.value ?: return
        val nodeToDelete = _allNodes.value.firstOrNull { it.id == nodeId } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            // If deleting the current central node, navigate up to parent first
            if (_currentCentralNodeId.value == nodeId) {
                val parentId = nodeToDelete.parentId
                _currentCentralNodeId.value = parentId ?: _allNodes.value.firstOrNull { it.parentId == null && it.id != nodeId }?.id
            }
            if (_pocketNode.value?.id == nodeId) {
                _pocketNode.value = null
            }
            repository.deleteNodeWithSubtree(nodeId, mapId)
            scheduleAutoSync()
        }
        _selectedNodeForEdit.value = null
        _nodeForQuickActions.value = null
    }

    fun reorderSiblingNodes(orderedNodeIds: List<String>) {
        val mapId = _activeMapId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.reorderSiblingNodes(orderedNodeIds, mapId)
            scheduleAutoSync()
        }
    }

    fun applyColorWithScope(
        nodeId: String,
        colorHex: String,
        scope: com.example.ui.util.ColorApplyScope
    ) {
        val mapId = _activeMapId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.applyColorWithScope(nodeId, colorHex, scope, mapId)
            scheduleAutoSync()
        }
    }

    fun toggleChecklistItem(node: MindNodeEntity, itemId: String) {
        val updatedList = node.checklist.map { item ->
            if (item.id == itemId) item.copy(isDone = !item.isDone) else item
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNode(
                node.copy(
                    checklistJson = ChecklistItem.listToJson(updatedList),
                    updatedAt = System.currentTimeMillis()
                )
            )
            scheduleAutoSync()
        }
    }

    // 🪐 Orbital Pocket / Clipboard Operations
    fun putNodeInPocket(node: MindNodeEntity) {
        _pocketNode.value = node
        _nodeForQuickActions.value = null
        _selectedNodeForEdit.value = null
    }

    fun clearPocket() {
        _pocketNode.value = null
        _isPocketActionDialogOpen.value = false
    }

    fun openPocketActionDialog() {
        if (_pocketNode.value != null) {
            _isPocketActionDialogOpen.value = true
        }
    }

    fun closePocketActionDialog() {
        _isPocketActionDialogOpen.value = false
    }

    fun applyPocketMoveToCurrentOrbit() {
        val node = _pocketNode.value ?: return
        val targetParentId = _currentCentralNodeId.value ?: return
        val mapId = _activeMapId.value ?: return

        // Prevent moving root or moving to itself
        if (node.parentId == null || node.id == targetParentId) {
            _isPocketActionDialogOpen.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.moveNode(node.id, targetParentId, mapId)
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                _pocketNode.value = null
                _isPocketActionDialogOpen.value = false
            }
        }
    }

    fun applyPocketCloneToCurrentOrbit() {
        val node = _pocketNode.value ?: return
        val targetParentId = _currentCentralNodeId.value ?: return
        val mapId = _activeMapId.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            repository.cloneNodeSubtree(node.id, targetParentId, mapId)
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                _pocketNode.value = null
                _isPocketActionDialogOpen.value = false
            }
        }
    }

    fun applyPocketSyncTwinToCurrentOrbit() {
        val node = _pocketNode.value ?: return
        val targetParentId = _currentCentralNodeId.value ?: return
        val mapId = _activeMapId.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            repository.createSyncTwinNode(node.id, targetParentId, mapId)
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                _pocketNode.value = null
                _isPocketActionDialogOpen.value = false
            }
        }
    }

    // 🎯 Direct Node-to-Node Drop Handlers (الإسقاط المباشر بين العقد)
    fun openDropActionForNodes(sourceNode: MindNodeEntity, targetNode: MindNodeEntity, isDirectDrop: Boolean = true) {
        if (sourceNode.id == targetNode.id) return
        _dropTargetAction.value = DropTargetAction(sourceNode, targetNode, isDirectDrop)
    }

    fun closeDropActionForNodes() {
        _dropTargetAction.value = null
    }

    fun executeDropMove(sourceNodeId: String, targetParentId: String) {
        val mapId = _activeMapId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveNode(sourceNodeId, targetParentId, mapId)
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                if (_pocketNode.value?.id == sourceNodeId) {
                    _pocketNode.value = null
                }
                _dropTargetAction.value = null
                _isPocketActionDialogOpen.value = false
            }
        }
    }

    fun executeDropClone(sourceNodeId: String, targetParentId: String) {
        val mapId = _activeMapId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.cloneNodeSubtree(sourceNodeId, targetParentId, mapId)
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                if (_pocketNode.value?.id == sourceNodeId) {
                    _pocketNode.value = null
                }
                _dropTargetAction.value = null
                _isPocketActionDialogOpen.value = false
            }
        }
    }

    fun executeDropSyncTwin(sourceNodeId: String, targetParentId: String) {
        val mapId = _activeMapId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.createSyncTwinNode(sourceNodeId, targetParentId, mapId)
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                if (_pocketNode.value?.id == sourceNodeId) {
                    _pocketNode.value = null
                }
                _dropTargetAction.value = null
                _isPocketActionDialogOpen.value = false
            }
        }
    }

    // Map operations
    fun openCreateMapDialog() {
        _isCreateMapOpen.value = true
    }

    fun closeCreateMapDialog() {
        _isCreateMapOpen.value = false
    }

    fun createMap(title: String, description: String, themeColorHex: String, rootIcon: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newMapId = repository.createMap(
                title = title.ifBlank { "New Universe" },
                description = description,
                themeColorHex = themeColorHex,
                rootIcon = rootIcon
            )
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                _isCreateMapOpen.value = false
                selectMap(newMapId)
            }
        }
    }

    fun createMapFromOutline(mapTitle: String, parsedNodes: List<MindNodeEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val rootNode = parsedNodes.firstOrNull { it.parentId == null } ?: parsedNodes.firstOrNull()
            val map = MindMapEntity(
                title = mapTitle.ifBlank { rootNode?.title ?: "خريطة جديدة" },
                description = "تم إنشاؤها عبر المعالج الذكي للنصوص",
                themeColorHex = rootNode?.colorHex ?: "#3B82F6",
                rootNodeId = rootNode?.id ?: "",
                createdAt = now,
                updatedAt = now
            )
            val newMapId = repository.insertCustomMapAndNodes(map) { targetMapId ->
                parsedNodes.map { it.copy(mapId = targetMapId) }
            }
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                _isCreateMapOpen.value = false
                _userFeedbackMessage.value = "تم إنشاء الخريطة الذكية بنجاح (${parsedNodes.size} عقدة) 🚀"
                selectMap(newMapId)
            }
        }
    }

    fun createMapFromTemplate(templateKey: String, isArabic: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val newMapId = repository.createTemplateMap(templateKey, isArabic)
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                _isCreateMapOpen.value = false
                selectMap(newMapId)
            }
        }
    }

    fun duplicateMap(mapId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.duplicateMap(mapId)
            scheduleAutoSync()
        }
    }

    fun mergeMapIntoMap(
        sourceMapId: Long,
        targetMapId: Long,
        targetParentNodeId: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        if (sourceMapId == targetMapId) return
        viewModelScope.launch(Dispatchers.IO) {
            val sourceMap = repository.getMapByIdSync(sourceMapId)
            val targetMap = repository.getMapByIdSync(targetMapId)
            val mergedBranch = repository.mergeMapIntoMap(
                sourceMapId = sourceMapId,
                targetMapId = targetMapId,
                targetParentNodeId = targetParentNodeId,
                deleteSourceMap = true
            )
            scheduleAutoSync()
            withContext(Dispatchers.Main) {
                if (mergedBranch != null) {
                    val targetTitle = targetMap?.title ?: "العالم"
                    val sourceTitle = sourceMap?.title ?: "العالم"
                    _userFeedbackMessage.value = "تم دمج «$sourceTitle» بنجاح داخل «$targetTitle» كفرع مداري جديد! 🪐"
                    onSuccess?.invoke()
                } else {
                    _userFeedbackMessage.value = "تعذر دمج العوالم، يرجى المحاولة لاحقاً"
                }
            }
        }
    }

    fun deleteMap(mapId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMap(mapId)
            scheduleAutoSync()
            if (_activeMapId.value == mapId) {
                withContext(Dispatchers.Main) {
                    navigateBackToMapList()
                }
            }
        }
    }

    // Search & Filter
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    // Outline Mode
    fun setOutlineOpen(open: Boolean) {
        _isOutlineOpen.value = open
    }

    fun getExportMarkdown(): String {
        val mapId = _activeMapId.value ?: return ""
        val currentMap = allMaps.value.firstOrNull { it.id == mapId } ?: return ""
        return repository.exportToMarkdown(currentMap, _allNodes.value)
    }

    // ==========================================
    // EXPORT & BACKUP CONTROLS
    // ==========================================

    fun openExportDialogForCurrentMap() {
        val mapId = _activeMapId.value ?: return
        val currentMap = allMaps.value.firstOrNull { it.id == mapId } ?: return
        _exportTargetMap.value = currentMap
        _exportTargetNodes.value = _allNodes.value
        _isExportDialogOpen.value = true
    }

    fun openExportDialogForMap(mapId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val map = repository.getMapByIdSync(mapId) ?: return@launch
            val nodes = repository.getAllNodesForMapSync(mapId)
            withContext(Dispatchers.Main) {
                _exportTargetMap.value = map
                _exportTargetNodes.value = nodes
                _isExportDialogOpen.value = true
            }
        }
    }

    fun closeExportDialog() {
        _isExportDialogOpen.value = false
        _exportTargetMap.value = null
        _exportTargetNodes.value = emptyList()
    }

    suspend fun generateExportJsonForTarget(): String {
        val map = _exportTargetMap.value ?: return ""
        val nodes = _exportTargetNodes.value
        return com.example.ui.util.OrbitMindBackupManager.createSingleMapBackupJson(
            context = getApplication(),
            map = map,
            nodes = nodes
        )
    }

    suspend fun generateFullBackupJson(): String {
        val packages = repository.getAllMapsWithNodesSync()
        return com.example.ui.util.OrbitMindBackupManager.createFullUniverseBackupJson(
            context = getApplication(),
            allMapsWithNodes = packages
        )
    }

    // ==========================================
    // IMPORT CONTROLS
    // ==========================================

    fun openImportDialog() {
        _isImportDialogOpen.value = true
    }

    fun closeImportDialog() {
        _isImportDialogOpen.value = false
    }

    fun importBackupJson(
        jsonString: String,
        asNewCopy: Boolean,
        onSuccess: (Long?) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.ui.util.OrbitMindBackupManager.executeImport(
                context = getApplication(),
                repository = repository,
                jsonStr = jsonString,
                importAsNewCopy = asNewCopy
            )
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { importResult ->
                        _isImportDialogOpen.value = false
                        val count = importResult.totalMaps
                        val nodes = importResult.totalNodes
                        val message = if (count > 1) {
                            "تم استيراد $count خرائط بنجاح ($nodes كوكب وعقدة)!"
                        } else {
                            "تم استيراد الخريطة بنجاح ($nodes كوكب وعقدة)!"
                        }
                        _userFeedbackMessage.value = message
                        onSuccess(importResult.primaryMapId)
                    },
                    onFailure = { error ->
                        val err = error.localizedMessage ?: "فشل استيراد الملف، تأكد من صحة التنسيق"
                        onError(err)
                    }
                )
            }
        }
    }

    fun updateChildrenProgress(progressMap: Map<String, Int>) {
        if (progressMap.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val currentNodes = _allNodes.value
            progressMap.forEach { (childId, newProg) ->
                val child = currentNodes.firstOrNull { it.id == childId }
                if (child != null && child.progress != newProg) {
                    repository.updateNode(child.copy(progress = newProg, updatedAt = System.currentTimeMillis()))
                }
            }
            scheduleAutoSync()
        }
    }

    fun importNodeJsonSubtree(parentId: String, payload: com.example.ui.dialogs.NodeJsonPayload) {
        val currentMapId = _activeMapId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            suspend fun insertSubtree(pId: String, p: com.example.ui.dialogs.NodeJsonPayload) {
                val newNode = repository.addNode(
                    mapId = currentMapId,
                    parentId = pId,
                    title = p.title.ifBlank { "عقدة فرعية" },
                    colorHex = p.colorHex,
                    iconName = p.iconName,
                    notes = p.notes,
                    checklist = p.checklist,
                    linkUrl = p.linkUrl,
                    imageUri = p.imageUri,
                    progress = p.progress,
                    impact = p.impact,
                    dueDate = p.dueDate
                )
                p.children.forEach { childPayload ->
                    insertSubtree(newNode.id, childPayload)
                }
            }

            payload.children.forEach { childPayload ->
                insertSubtree(parentId, childPayload)
            }
            scheduleAutoSync()
        }
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    // Theme Mode Management
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        themePreferences.setThemeMode(mode)
    }

    fun toggleDarkMode() {
        val nextMode = when (_themeMode.value) {
            ThemeMode.DARK, ThemeMode.ZEN_BLACK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(nextMode)
    }

    fun cycleThemeMode() {
        val nextMode = when (_themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.ZEN_BLACK
            ThemeMode.ZEN_BLACK -> ThemeMode.SYSTEM
        }
        setThemeMode(nextMode)
    }
}
