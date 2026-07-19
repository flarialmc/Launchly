package com.zeuroux.launchly.data

import com.zeuroux.launchly.model.ManagedVersion
import com.zeuroux.launchly.model.VersionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface ManagedVersionRepository {
    val versions: Flow<List<ManagedVersion>>
    suspend fun get(id: String): ManagedVersion?
    suspend fun add(version: VersionData, displayName: String = version.name): ManagedVersion
    suspend fun update(value: ManagedVersion)
    suspend fun delete(id: String)
}

class RoomManagedVersionRepository(
    private val dao: ManagedVersionDao
) : ManagedVersionRepository {
    override val versions: Flow<List<ManagedVersion>> = dao.observeAll().map { values ->
        values.map(ManagedVersionEntity::toModel)
    }

    override suspend fun get(id: String): ManagedVersion? = dao.get(id)?.toModel()

    override suspend fun add(version: VersionData, displayName: String): ManagedVersion {
        val now = System.currentTimeMillis()
        val value = ManagedVersion(
            id = UUID.randomUUID().toString(),
            displayName = displayName.trim().ifBlank { version.name },
            versionCode = version.code,
            versionName = version.name,
            track = version.track,
            architecture = version.architecture,
            customIconPath = null,
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(value.toEntity())
        return value
    }

    override suspend fun update(value: ManagedVersion) {
        dao.upsert(value.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun delete(id: String) = dao.delete(id)
}

internal fun ManagedVersion.toEntity() = ManagedVersionEntity(
    id, displayName, versionCode, versionName, track, architecture,
    customIconPath, createdAt, updatedAt
)
