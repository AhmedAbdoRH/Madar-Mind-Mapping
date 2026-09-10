package com.example.ui.util

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DriveBackupFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val modifiedTimeIso: String,
    val description: String?,
    val webViewLink: String?
) {
    val isFullBackup: Boolean
        get() = name.contains("All_Universes", ignoreCase = true) || description?.contains("Full Backup", ignoreCase = true) == true

    val formattedSize: String
        get() = when {
            sizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024f * 1024f))
            sizeBytes >= 1024 -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024f)
            sizeBytes > 0 -> "$sizeBytes B"
            else -> "Cloud File"
        }

    val formattedDate: String
        get() {
            return try {
                if (modifiedTimeIso.isNotBlank()) {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val date = inputFormat.parse(modifiedTimeIso.substring(0, minOf(19, modifiedTimeIso.length)))
                    if (date != null) {
                        val outputFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                        outputFormat.format(date)
                    } else modifiedTimeIso
                } else ""
            } catch (e: Exception) {
                modifiedTimeIso
            }
        }

    val modifiedTimeEpochMs: Long
        get() {
            return try {
                if (modifiedTimeIso.isNotBlank()) {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val date = inputFormat.parse(modifiedTimeIso.substring(0, minOf(19, modifiedTimeIso.length)))
                    date?.time ?: 0L
                } else 0L
            } catch (e: Exception) {
                0L
            }
        }
}

object GoogleDriveSyncManager {
    private const val TAG = "GoogleDriveSync"
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(DRIVE_FILE_SCOPE),
                Scope(DRIVE_APPDATA_SCOPE)
            )
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && (GoogleSignIn.hasPermissions(account, Scope(DRIVE_FILE_SCOPE)) || account.email != null)) {
            account
        } else {
            null
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        val client = getGoogleSignInClient(context)
        client.signOut().addOnCompleteListener {
            onComplete()
        }
    }

    private fun getAccountObject(account: GoogleSignInAccount): Account? {
        return account.account ?: account.email?.let { Account(it, "com.google") }
    }

    suspend fun invalidateToken(context: Context, token: String) {
        withContext(Dispatchers.IO) {
            try {
                GoogleAuthUtil.clearToken(context, token)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear expired Google Drive OAuth token", e)
            }
        }
    }

    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val accountObj = getAccountObject(account)
                    ?: return@withContext Result.failure(Exception("Google Account email not found. Please sign in again."))
                
                val scopeString = "oauth2:$DRIVE_FILE_SCOPE $DRIVE_APPDATA_SCOPE"
                val token = GoogleAuthUtil.getToken(context, accountObj, scopeString)
                Result.success(token)
            } catch (e: UserRecoverableAuthException) {
                Log.w(TAG, "User resolution required for Google Drive permissions", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Error obtaining Google Drive OAuth token", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun <T> executeWithToken(
        context: Context,
        account: GoogleSignInAccount,
        action: suspend (token: String) -> Result<T>
    ): Result<T> {
        val firstTokenRes = getAccessToken(context, account)
        if (firstTokenRes.isFailure) {
            return Result.failure(firstTokenRes.exceptionOrNull() ?: Exception("OAuth token failure"))
        }
        val token = firstTokenRes.getOrThrow()
        val actionRes = action(token)

        val exception = actionRes.exceptionOrNull()
        if (exception != null && exception.message?.contains("401") == true) {
            Log.i(TAG, "Received 401, refreshing OAuth token and retrying...")
            invalidateToken(context, token)
            val retryTokenRes = getAccessToken(context, account)
            if (retryTokenRes.isSuccess) {
                return action(retryTokenRes.getOrThrow())
            }
        }
        return actionRes
    }

    suspend fun uploadBackupFile(
        context: Context,
        account: GoogleSignInAccount,
        fileName: String,
        jsonContent: String,
        description: String = "Madar Mind Maps Backup"
    ): Result<DriveBackupFile> {
        return executeWithToken(context, account) { token ->
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(45, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val metadataJson = JSONObject().apply {
                        put("name", fileName)
                        put("mimeType", "application/json")
                        put("description", description)
                    }.toString()

                    val multipartBody = MultipartBody.Builder()
                        .setType("multipart/related".toMediaType())
                        .addPart(
                            metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                        )
                        .addPart(
                            jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType())
                        )
                        .build()

                    val request = Request.Builder()
                        .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,size,modifiedTime,description,webViewLink")
                        .addHeader("Authorization", "Bearer $token")
                        .post(multipartBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            return@withContext Result.failure(Exception("Drive upload failed (${response.code}): $responseBody"))
                        }
                        val json = JSONObject(responseBody)
                        Result.success(parseDriveFile(json))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload file to Google Drive", e)
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun updateOrCreateAutoSyncFile(
        context: Context,
        account: GoogleSignInAccount,
        existingFileId: String?,
        fileName: String,
        jsonContent: String,
        description: String = "Madar Cloud Auto-Sync Backup"
    ): Result<DriveBackupFile> {
        if (!existingFileId.isNullOrBlank()) {
            val patchResult = executeWithToken(context, account) { token ->
                withContext(Dispatchers.IO) {
                    try {
                        val client = OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(45, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .build()

                        val request = Request.Builder()
                            .url("https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media&fields=id,name,size,modifiedTime,description,webViewLink")
                            .addHeader("Authorization", "Bearer $token")
                            .patch(jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                            .build()

                        client.newCall(request).execute().use { response ->
                            val responseBody = response.body?.string() ?: ""
                            if (response.isSuccessful) {
                                val json = JSONObject(responseBody)
                                Result.success(parseDriveFile(json))
                            } else {
                                Result.failure(Exception("Drive patch failed (${response.code}): $responseBody"))
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to patch auto-sync file, will recreate", e)
                        Result.failure(e)
                    }
                }
            }
            if (patchResult.isSuccess) {
                return patchResult
            }
        }
        // Fallback: create as new upload
        return uploadBackupFile(context, account, fileName, jsonContent, description)
    }

    suspend fun listBackupFiles(
        context: Context,
        account: GoogleSignInAccount
    ): Result<List<DriveBackupFile>> {
        return executeWithToken(context, account) { token ->
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .build()

                    val query = "trashed = false and mimeType != 'application/vnd.google-apps.folder'"
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&orderBy=modifiedTime%20desc&pageSize=100&fields=files(id,name,size,modifiedTime,createdTime,description,webViewLink)"

                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $token")
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            return@withContext Result.failure(Exception("Failed to list Google Drive files (${response.code}): $responseBody"))
                        }
                        val json = JSONObject(responseBody)
                        val filesArray = json.optJSONArray("files")
                        val list = mutableListOf<DriveBackupFile>()
                        if (filesArray != null) {
                            for (i in 0 until filesArray.length()) {
                                val fileObj = filesArray.getJSONObject(i)
                                val parsed = parseDriveFile(fileObj)
                                // Filter files that are related to Madar / OrbitMind or json
                                if (parsed.name.endsWith(".madar", ignoreCase = true) ||
                                    parsed.name.endsWith(".omind", ignoreCase = true) ||
                                    parsed.name.endsWith(".json", ignoreCase = true) ||
                                    parsed.name.contains("Madar", ignoreCase = true) ||
                                    parsed.name.contains("OrbitMind", ignoreCase = true) ||
                                    parsed.description?.contains("Madar", ignoreCase = true) == true ||
                                    parsed.description?.contains("OrbitMind", ignoreCase = true) == true
                                ) {
                                    list.add(parsed)
                                }
                            }
                        }
                        Result.success(list)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to list Google Drive files", e)
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun findFileByName(
        context: Context,
        account: GoogleSignInAccount,
        fileName: String
    ): Result<DriveBackupFile?> {
        return executeWithToken(context, account) { token ->
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .build()

                    val query = "name = '$fileName' and trashed = false"
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&orderBy=modifiedTime%20desc&pageSize=1&fields=files(id,name,size,modifiedTime,createdTime,description,webViewLink)"

                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $token")
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            return@withContext Result.failure(Exception("Drive search failed (${response.code}): $responseBody"))
                        }
                        val json = JSONObject(responseBody)
                        val filesArray = json.optJSONArray("files")
                        if (filesArray != null && filesArray.length() > 0) {
                            Result.success(parseDriveFile(filesArray.getJSONObject(0)))
                        } else {
                            Result.success(null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to find file on Drive", e)
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun downloadBackupFileContent(
        context: Context,
        account: GoogleSignInAccount,
        fileId: String
    ): Result<String> {
        return executeWithToken(context, account) { token ->
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                        .addHeader("Authorization", "Bearer $token")
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@withContext Result.failure(Exception("Failed to download file from Drive (${response.code})"))
                        }
                        val content = response.body?.string() ?: ""
                        Result.success(content)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download file content from Google Drive", e)
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun deleteBackupFile(
        context: Context,
        account: GoogleSignInAccount,
        fileId: String
    ): Result<Boolean> {
        return executeWithToken(context, account) { token ->
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files/$fileId")
                        .addHeader("Authorization", "Bearer $token")
                        .delete()
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful || response.code == 204 || response.code == 404) {
                            Result.success(true)
                        } else {
                            Result.failure(Exception("Failed to delete Google Drive file (${response.code})"))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete file from Google Drive", e)
                    Result.failure(e)
                }
            }
        }
    }

    private fun parseDriveFile(json: JSONObject): DriveBackupFile {
        return DriveBackupFile(
            id = json.optString("id", ""),
            name = json.optString("name", "Untitled Map"),
            sizeBytes = json.optLong("size", 0L),
            modifiedTimeIso = json.optString("modifiedTime", json.optString("createdTime", "")),
            description = if (json.has("description") && !json.isNull("description")) json.optString("description") else null,
            webViewLink = if (json.has("webViewLink") && !json.isNull("webViewLink")) json.optString("webViewLink") else null
        )
    }
}
