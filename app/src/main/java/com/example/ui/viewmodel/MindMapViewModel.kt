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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MindMapViewModel(application: Application) : AndroidViewModel(application) {

    private val themePreferences = ThemePreferences(application)
    private val database = AppDatabase.getInstance(application)
    private val repository = MindMapRepository(database.mindMapDao(), database.mindNodeDao())

    private val _themeMode = MutableStateFlow(themePreferences.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    val allMaps: StateFlow<List<MindMapEntity>> = repository.allMaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Quick Action Menu on Node Long Press
    private val _nodeForQuickActions = MutableStateFlow<MindNodeEntity?>(null)
    val nodeForQuickActions: StateFlow<MindNodeEntity?> = _nodeForQuickActions.asStateFlow()

    init {
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
    }

    fun selectMap(mapId: Long) {
        _activeMapId.value = mapId
        _orbitRotationAngle.value = 0f
        _pocketNode.value = null
        viewModelScope.launch {
            repository.getAllNodesForMap(mapId).collect { nodes ->
                _allNodes.value = nodes
                // If central node is not set or not in this map, set to root
                val currentId = _currentCentralNodeId.value
                val rootNode = nodes.firstOrNull { it.parentId == null }
                if (currentId == null || nodes.none { it.id == currentId }) {
                    _currentCentralNodeId.value = rootNode?.id
                }
            }
        }
    }

    fun navigateBackToMapList() {
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
        linkUrl: String = ""
    ) {
        val mapId = _activeMapId.value ?: return
        val parentId = _currentCentralNodeId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addNode(
                mapId = mapId,
                parentId = parentId,
                title = title.trim(),
                colorHex = colorHex,
                iconName = iconName,
                notes = notes.trim(),
                checklist = checklist,
                linkUrl = linkUrl.trim()
            )
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
        linkUrl: String
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
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNode(updated)
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
        }
        _selectedNodeForEdit.value = null
        _nodeForQuickActions.value = null
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
            withContext(Dispatchers.Main) {
                _pocketNode.value = null
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
            withContext(Dispatchers.Main) {
                _isCreateMapOpen.value = false
                selectMap(newMapId)
            }
        }
    }

    fun duplicateMap(mapId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.duplicateMap(mapId)
        }
    }

    fun deleteMap(mapId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMap(mapId)
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

    // Theme Mode Management
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        themePreferences.setThemeMode(mode)
    }

    fun toggleDarkMode() {
        val nextMode = when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(nextMode)
    }

    fun cycleThemeMode() {
        val nextMode = when (_themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(nextMode)
    }
}
