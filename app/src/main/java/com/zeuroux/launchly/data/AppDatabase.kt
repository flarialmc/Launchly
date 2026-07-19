package com.zeuroux.launchly.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.DownloadStatus
import com.zeuroux.launchly.model.ReleaseTrack

@Database(
    entities = [ManagedVersionEntity::class, DownloadRecordEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun managedVersionDao(): ManagedVersionDao
    abstract fun downloadRecordDao(): DownloadRecordDao
}

class AppTypeConverters {
    @TypeConverter fun releaseTrackToString(value: ReleaseTrack): String = value.name
    @TypeConverter fun stringToReleaseTrack(value: String): ReleaseTrack = enumValueOrDefault(value, ReleaseTrack.UNKNOWN)
    @TypeConverter fun architectureToString(value: Architecture): String = value.abi
    @TypeConverter fun stringToArchitecture(value: String): Architecture = Architecture.fromAbi(value)
    @TypeConverter fun downloadStatusToString(value: DownloadStatus): String = value.name
    @TypeConverter fun stringToDownloadStatus(value: String): DownloadStatus = enumValueOrDefault(value, DownloadStatus.FAILED)

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default
}
