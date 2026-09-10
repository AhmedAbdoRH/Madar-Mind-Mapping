package com.example.ui.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.repository.MindMapRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val timestamp: Long) : SyncStatus()
    data class Error(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
}

/**
 * Intelligent Debounced Auto-Sync Manager for Google Drive.
 * Automatically synchronizes changes to Google Drive in the background whenever
 * universes, nodes, or links are modified, with smart debouncing to avoid quota limits
 * and minimize battery/data usage.
 */
class AutoSyncManager(
    private val context: Context,
    private val repository: MindMapRepository
) {
    companion object {
        private const val TAG = "AutoSyncManager"
        private const val PREFS_NAME = "madar_auto_sync_prefs"
        private const val KEY_AUTO_SYNC_ENABLED = "key_auto_sync_enabled"
        private const val KEY_LAST_SYNC_FILE_ID = "key_last_sync_file_id"
        private const val KEY_LAST_SYNC_TIME = "key_last_sync_time"
        private const val DEFAULT_DEBOUNCE_MS = 5000L // 5 seconds debounce after last change
        const val AUTO_SYNC_FILE_NAME = "Madar_Universe.json"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var debounceJob: Job? = null

    private val _isAutoSyncEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    )
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(
        prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    )
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(
        if (_lastSyncTimestamp.value > 0) SyncStatus.Success(_lastSyncTimestamp.value) else SyncStatus.Idle
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
        _isAutoSyncEnabled.value = enabled
        if (enabled) {
            triggerSyncDebounced(1000L)
        } else {
            debounceJob?.cancel()
            _syncStatus.value = SyncStatus.Idle
        }
    }

    /**
     * Schedules a debounced sync. Call this on any DB modification (add, edit, delete, move).
     * If called repeatedly within [delayMs], the timer resets to prevent duplicate syncs.
     */
    fun triggerSyncDebounced(delayMs: Long = DEFAULT_DEBOUNCE_MS) {
        if (!_isAutoSyncEnabled.value) return
        val account = GoogleDriveSyncManager.getLastSignedInAccount(context)
        if (account == null) return

        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            executeSyncInternal()
        }
    }

    /**
     * Checks if Google Drive has a newer version of the universe file (e.g., edited by Claude)
     * and automatically downloads and replaces the local data in-place if newer.
     */
    fun checkAndPullRemoteChangesIfNewer(onComplete: ((Boolean, String?) -> Unit)? = null) {
        val account = GoogleDriveSyncManager.getLastSignedInAccount(context)
        if (account == null) {
            onComplete?.invoke(false, "Google account not connected")
            return
        }
        scope.launch {
            val result = executeSyncInternal(allowPullFromRemote = true, forceUpload = false)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(result.isSuccess, result.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    /**
     * Immediately runs sync without debouncing (e.g., when user taps "Sync Now").
     */
    fun triggerSyncImmediate(onComplete: ((Boolean, String?) -> Unit)? = null) {
        val account = GoogleDriveSyncManager.getLastSignedInAccount(context)
        if (account == null) {
            onComplete?.invoke(false, "Google account not connected")
            return
        }
        debounceJob?.cancel()
        scope.launch {
            val result = executeSyncInternal(allowPullFromRemote = true, forceUpload = false)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(result.isSuccess, result.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    private suspend fun executeSyncInternal(
        allowPullFromRemote: Boolean = false,
        forceUpload: Boolean = false
    ): Result<DriveBackupFile> {
        val account = GoogleDriveSyncManager.getLastSignedInAccount(context)
            ?: return Result.failure(Exception("Not logged in to Google Drive"))

        _syncStatus.value = SyncStatus.Syncing

        return try {
            val existingFileId = prefs.getString(KEY_LAST_SYNC_FILE_ID, null)

            // 1. Check if remote file exists on Google Drive
            val remoteFileResult = GoogleDriveSyncManager.findFileByName(context, account, AUTO_SYNC_FILE_NAME)
            val remoteFile = remoteFileResult.getOrNull()

            val localLastSyncTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)

            // 2. If remote file is newer than our last sync time (e.g. modified via Claude or web), pull & replace in place!
            if (allowPullFromRemote && !forceUpload && remoteFile != null && remoteFile.modifiedTimeEpochMs > (localLastSyncTime + 3000L)) {
                Log.i(TAG, "Remote Drive file is newer (${remoteFile.modifiedTimeEpochMs} vs $localLastSyncTime). Pulling & replacing local data...")
                val downloadRes = GoogleDriveSyncManager.downloadBackupFileContent(context, account, remoteFile.id)
                if (downloadRes.isSuccess) {
                    val remoteJson = downloadRes.getOrNull() ?: ""
                    if (remoteJson.isNotBlank()) {
                        val importRes = OrbitMindBackupManager.executeImport(
                            context = context,
                            repository = repository,
                            jsonStr = remoteJson,
                            importAsNewCopy = false // In-place replace!
                        )
                        if (importRes.isSuccess) {
                            val newTimestamp = remoteFile.modifiedTimeEpochMs
                            prefs.edit()
                                .putString(KEY_LAST_SYNC_FILE_ID, remoteFile.id)
                                .putLong(KEY_LAST_SYNC_TIME, newTimestamp)
                                .apply()

                            _lastSyncTimestamp.value = newTimestamp
                            _syncStatus.value = SyncStatus.Success(newTimestamp)
                            Log.i(TAG, "Successfully replaced local universe with newer version from Google Drive!")
                            return Result.success(remoteFile)
                        } else {
                            Log.e(TAG, "Failed to apply remote JSON import: ${importRes.exceptionOrNull()?.localizedMessage}")
                        }
                    }
                }
            }

            // 3. Otherwise, export local universe and update/upload to Google Drive
            val packages = repository.getAllMapsWithNodesSync()
            val fullJson = OrbitMindBackupManager.createFullUniverseBackupJson(
                context = context,
                allMapsWithNodes = packages
            )

            val targetFileId = remoteFile?.id ?: existingFileId

            val uploadResult = GoogleDriveSyncManager.updateOrCreateAutoSyncFile(
                context = context,
                account = account,
                existingFileId = targetFileId,
                fileName = AUTO_SYNC_FILE_NAME,
                jsonContent = fullJson,
                description = "Madar Smart Auto-Sync Universe Backup"
            )

            uploadResult.fold(
                onSuccess = { driveFile ->
                    val now = System.currentTimeMillis()
                    prefs.edit()
                        .putString(KEY_LAST_SYNC_FILE_ID, driveFile.id)
                        .putLong(KEY_LAST_SYNC_TIME, now)
                        .apply()

                    _lastSyncTimestamp.value = now
                    _syncStatus.value = SyncStatus.Success(now)
                    Log.i(TAG, "Smart Auto-Sync successfully completed to file ID: ${driveFile.id}")
                    Result.success(driveFile)
                },
                onFailure = { error ->
                    Log.e(TAG, "Smart Auto-Sync failed", error)
                    _syncStatus.value = SyncStatus.Error(error.localizedMessage ?: "Sync error")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during smart auto sync execution", e)
            _syncStatus.value = SyncStatus.Error(e.localizedMessage ?: "Unexpected sync error")
            Result.failure(e)
        }
    }
}
