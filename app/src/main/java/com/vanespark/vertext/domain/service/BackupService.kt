package com.vanespark.vertext.domain.service

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.vanespark.vertext.data.database.VectorTextDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for encrypted backup and restore of the VectorText database
 */
@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: VectorTextDatabase
) {

    companion object {
        private const val BACKUP_DIR = "backups"
        private const val BACKUP_PREFIX = "vertext_backup_"
        private const val BACKUP_EXTENSION = ".vbak"
        private const val BUFFER_SIZE = 8192
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Create encrypted backup of database
     */
    suspend fun createBackup(): Flow<BackupProgress> = flow {
        try {
            emit(BackupProgress.InProgress(0, "Preparing backup..."))

            // Close database connections
            withContext(Dispatchers.IO) {
                database.close()
            }

            emit(BackupProgress.InProgress(10, "Reading database..."))

            // Get database file
            val dbFile = context.getDatabasePath(VectorTextDatabase.DATABASE_NAME)
            if (!dbFile.exists()) {
                emit(BackupProgress.Error("Database file not found"))
                return@flow
            }

            val totalSize = dbFile.length()
            Timber.d("Database size: $totalSize bytes")

            emit(BackupProgress.InProgress(20, "Creating backup file..."))

            // Create backup directory if needed
            val backupDir = File(context.filesDir, BACKUP_DIR)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Generate backup filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "$BACKUP_PREFIX$timestamp$BACKUP_EXTENSION")

            emit(BackupProgress.InProgress(30, "Encrypting database..."))

            // Create encrypted file
            val encryptedFile = EncryptedFile.Builder(
                context,
                backupFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            // Copy database to encrypted backup with progress
            withContext(Dispatchers.IO) {
                FileInputStream(dbFile).use { input ->
                    encryptedFile.openFileOutput().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead = 0L
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read

                            // Calculate progress (30% to 90%)
                            val progress = 30 + ((bytesRead.toFloat() / totalSize) * 60).toInt()
                            emit(BackupProgress.InProgress(progress, "Encrypting... ${formatSize(bytesRead)}"))
                        }
                    }
                }
            }

            emit(BackupProgress.InProgress(95, "Finalizing backup..."))

            // Create backup metadata
            val backup = BackupMetadata(
                filename = backupFile.name,
                path = backupFile.absolutePath,
                timestamp = System.currentTimeMillis(),
                size = backupFile.length(),
                messageCount = database.messageDao().getTotalMessageCount()
            )

            Timber.d("Backup created: ${backup.filename} (${formatSize(backup.size)})")

            emit(BackupProgress.Success(backup))

        } catch (e: Exception) {
            Timber.e(e, "Backup failed")
            emit(BackupProgress.Error(e.message ?: "Backup failed"))
        }
    }

    /**
     * Restore database from encrypted backup
     */
    suspend fun restoreBackup(backupPath: String): Flow<RestoreProgress> = flow {
        try {
            emit(RestoreProgress.InProgress(0, "Preparing restore..."))

            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                emit(RestoreProgress.Error("Backup file not found"))
                return@flow
            }

            val totalSize = backupFile.length()
            Timber.d("Backup size: $totalSize bytes")

            emit(RestoreProgress.InProgress(10, "Closing database..."))

            // Close database connections
            withContext(Dispatchers.IO) {
                database.close()
            }

            emit(RestoreProgress.InProgress(20, "Decrypting backup..."))

            // Create encrypted file reader
            val encryptedFile = EncryptedFile.Builder(
                context,
                backupFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            // Get database file path
            val dbFile = context.getDatabasePath(VectorTextDatabase.DATABASE_NAME)

            emit(RestoreProgress.InProgress(30, "Restoring database..."))

            // Restore database from encrypted backup with progress
            withContext(Dispatchers.IO) {
                encryptedFile.openFileInput().use { input ->
                    FileOutputStream(dbFile).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesWritten = 0L
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesWritten += read

                            // Calculate progress (30% to 90%)
                            val progress = 30 + ((bytesWritten.toFloat() / totalSize) * 60).toInt()
                            emit(RestoreProgress.InProgress(progress, "Restoring... ${formatSize(bytesWritten)}"))
                        }
                    }
                }
            }

            emit(RestoreProgress.InProgress(95, "Verifying database..."))

            // Verify database integrity
            withContext(Dispatchers.IO) {
                try {
                    database.openHelper.writableDatabase.query("PRAGMA integrity_check").use { cursor ->
                        if (cursor.moveToFirst()) {
                            val result = cursor.getString(0)
                            if (result != "ok") {
                                emit(RestoreProgress.Error("Database integrity check failed: $result"))
                                return@withContext
                            }
                        }
                    }
                } catch (e: Exception) {
                    emit(RestoreProgress.Error("Database verification failed: ${e.message}"))
                    return@withContext
                }
            }

            Timber.d("Restore completed successfully")

            emit(RestoreProgress.Success)

        } catch (e: Exception) {
            Timber.e(e, "Restore failed")
            emit(RestoreProgress.Error(e.message ?: "Restore failed"))
        }
    }

    /**
     * Get list of available backups
     */
    suspend fun getBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, BACKUP_DIR)
            if (!backupDir.exists()) {
                return@withContext Result.success(emptyList())
            }

            val backups = backupDir.listFiles { file ->
                file.name.startsWith(BACKUP_PREFIX) && file.name.endsWith(BACKUP_EXTENSION)
            }?.map { file ->
                BackupMetadata(
                    filename = file.name,
                    path = file.absolutePath,
                    timestamp = file.lastModified(),
                    size = file.length(),
                    messageCount = 0 // We don't store this in filename
                )
            }?.sortedByDescending { it.timestamp } ?: emptyList()

            Timber.d("Found ${backups.size} backups")
            Result.success(backups)

        } catch (e: Exception) {
            Timber.e(e, "Failed to list backups")
            Result.failure(e)
        }
    }

    /**
     * Delete a backup file
     */
    suspend fun deleteBackup(backupPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(backupPath)
            if (file.exists()) {
                if (file.delete()) {
                    Timber.d("Deleted backup: ${file.name}")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete backup file"))
                }
            } else {
                Result.failure(Exception("Backup file not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete backup")
            Result.failure(e)
        }
    }

    /**
     * Format file size for display
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}

/**
 * Backup metadata
 */
data class BackupMetadata(
    val filename: String,
    val path: String,
    val timestamp: Long,
    val size: Long,
    val messageCount: Int
)

/**
 * Backup progress states
 */
sealed class BackupProgress {
    data class InProgress(val progress: Int, val message: String) : BackupProgress()
    data class Success(val backup: BackupMetadata) : BackupProgress()
    data class Error(val message: String) : BackupProgress()
}

/**
 * Restore progress states
 */
sealed class RestoreProgress {
    data class InProgress(val progress: Int, val message: String) : RestoreProgress()
    data object Success : RestoreProgress()
    data class Error(val message: String) : RestoreProgress()
}
