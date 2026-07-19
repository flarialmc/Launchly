package com.zeuroux.launchly.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.ReleaseTrack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManagedVersionDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun durableVersionAndDownloadStateRemainSeparate() = runBlocking {
        val version = ManagedVersionEntity(
            "11111111-1111-1111-1111-111111111111", "Test", 871000500, "1.21",
            ReleaseTrack.RELEASE, Architecture.ARM64, null, 1, 1
        )
        database.managedVersionDao().upsert(version)

        assertEquals(listOf(version), database.managedVersionDao().observeAll().first())
        assertEquals(emptyList<DownloadRecordEntity>(), database.downloadRecordDao().observeAll().first())
    }
}
