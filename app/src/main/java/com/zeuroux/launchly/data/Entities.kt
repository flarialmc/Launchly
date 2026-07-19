package com.zeuroux.launchly.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.DownloadRecord
import com.zeuroux.launchly.model.DownloadStatus
import com.zeuroux.launchly.model.ManagedVersion
import com.zeuroux.launchly.model.ReleaseTrack
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "managed_versions")
data class ManagedVersionEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val versionCode: Long,
    val versionName: String,
    val track: ReleaseTrack,
    val architecture: Architecture,
    val customIconPath: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toModel() = ManagedVersion(
        id, displayName, versionCode, versionName, track, architecture,
        customIconPath, createdAt, updatedAt
    )
}

@Entity(
    tableName = "download_records",
    foreignKeys = [ForeignKey(
        entity = ManagedVersionEntity::class,
        parentColumns = ["id"],
        childColumns = ["versionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DownloadRecordEntity(
    @PrimaryKey val versionId: String,
    val workId: String?,
    val status: DownloadStatus,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val speedBytesPerSecond: Long?,
    val failureType: String?,
    val failureMessage: String?,
    val updatedAt: Long
) {
    fun toModel() = DownloadRecord(
        versionId, workId, status, bytesDownloaded, totalBytes,
        speedBytesPerSecond, failureType, failureMessage, updatedAt
    )
}

@Dao
interface ManagedVersionDao {
    @Query("SELECT * FROM managed_versions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ManagedVersionEntity>>

    @Query("SELECT * FROM managed_versions WHERE id = :id")
    suspend fun get(id: String): ManagedVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: ManagedVersionEntity)

    @Query("DELETE FROM managed_versions WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DownloadRecordDao {
    @Query("SELECT * FROM download_records ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadRecordEntity>>

    @Query("SELECT * FROM download_records WHERE versionId = :versionId")
    fun observe(versionId: String): Flow<DownloadRecordEntity?>

    @Query("SELECT * FROM download_records WHERE versionId = :versionId")
    suspend fun get(versionId: String): DownloadRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: DownloadRecordEntity)

    @Query("DELETE FROM download_records WHERE versionId = :versionId")
    suspend fun delete(versionId: String)
}
