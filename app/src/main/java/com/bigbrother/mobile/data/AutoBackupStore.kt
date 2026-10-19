package com.bigbrother.mobile.data

import android.content.Context
import com.bigbrother.mobile.domain.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AutoBackupStore {

    private fun backupFile(context: Context): File {
        val dir = File(context.filesDir, "auto_backups")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "itt_auto_snapshot_latest.zip")
    }

    suspend fun createAutoSnapshot(context: Context, repository: AppRepository) = withContext(Dispatchers.IO) {
        runCatching {
            val bundle = repository.exportBundle()
            val zipBytes = repository.buildZip(bundle)
            val file = backupFile(context)
            file.writeBytes(zipBytes)
        }
    }

    fun hasSnapshot(context: Context): Boolean {
        val file = backupFile(context)
        return file.exists() && file.length() > 0
    }

    fun snapshotSizeFormatted(context: Context): String {
        val file = backupFile(context)
        if (!file.exists()) return "无快照"
        val bytes = file.length()
        val kb = bytes / 1024f
        return if (kb < 1024) String.format("%.1f KB", kb) else String.format("%.2f MB", kb / 1024f)
    }

    fun snapshotTimeFormatted(context: Context): String {
        val file = backupFile(context)
        if (!file.exists()) return "无"
        return TimeUtils.formatDateTime(file.lastModified())
    }

    suspend fun restoreLatestSnapshot(context: Context, repository: AppRepository): Boolean = withContext(Dispatchers.IO) {
        val file = backupFile(context)
        if (!file.exists()) return@withContext false
        runCatching {
            val bytes = file.readBytes()
            repository.importZipData(bytes, merge = false)
            true
        }.getOrDefault(false)
    }
}
