package com.zeuroux.launchly

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.zeuroux.launchly.auth.AuthRepository
import com.zeuroux.launchly.auth.DefaultAuthRepository
import com.zeuroux.launchly.auth.EncryptedAuthStore
import com.zeuroux.launchly.catalog.CachedVersionCatalogRepository
import com.zeuroux.launchly.catalog.VersionCatalogRepository
import com.zeuroux.launchly.data.AppDatabase
import com.zeuroux.launchly.data.AppPreferences
import com.zeuroux.launchly.data.ManagedVersionRepository
import com.zeuroux.launchly.data.RoomManagedVersionRepository
import com.zeuroux.launchly.download.DownloadCoordinator
import com.zeuroux.launchly.download.WorkManagerDownloadCoordinator
import com.zeuroux.launchly.gplay.GPlayService
import com.zeuroux.launchly.media.UserImageStore
import com.zeuroux.launchly.packageops.AckpinePackageCoordinator
import com.zeuroux.launchly.packageops.ApkSetValidator
import com.zeuroux.launchly.packageops.PackageCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface AppContainer {
    val okHttpClient: OkHttpClient
    val database: AppDatabase
    val preferences: AppPreferences
    val authRepository: AuthRepository
    val managedVersions: ManagedVersionRepository
    val catalog: VersionCatalogRepository
    val gPlayService: GPlayService
    val userImageStore: UserImageStore
    val apkSetValidator: ApkSetValidator
    val downloads: DownloadCoordinator
    val packages: PackageCoordinator
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .followRedirects(true)
        .followSslRedirects(false)
        .build()

    override val database: AppDatabase = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java,
        "launchly.db"
    ).build()

    override val preferences = AppPreferences(applicationContext)
    override val authRepository: AuthRepository = DefaultAuthRepository(
        EncryptedAuthStore(applicationContext),
        applicationScope
    )
    override val managedVersions: ManagedVersionRepository =
        RoomManagedVersionRepository(database.managedVersionDao())
    override val catalog: VersionCatalogRepository = CachedVersionCatalogRepository(
        applicationContext,
        okHttpClient,
        preferences,
        applicationScope
    )
    override val gPlayService = GPlayService(applicationContext, authRepository)
    override val userImageStore = UserImageStore(applicationContext)
    override val apkSetValidator = ApkSetValidator(applicationContext, preferences)
    override val downloads: DownloadCoordinator = WorkManagerDownloadCoordinator(
        applicationContext,
        WorkManager.getInstance(applicationContext),
        managedVersions,
        database.downloadRecordDao()
    )
    override val packages: PackageCoordinator = AckpinePackageCoordinator(
        applicationContext,
        managedVersions,
        apkSetValidator,
        preferences,
        applicationScope
    )

    init {
        (authRepository as DefaultAuthRepository).setProfileLoader {
            val current = authRepository.currentSession() ?: error("Sign in again to refresh your profile.")
            val profile = gPlayService.getProfile()
            current.copy(
                email = profile.email?.takeIf { it.isNotBlank() } ?: current.email,
                displayName = profile.displayName?.takeIf { it.isNotBlank() } ?: current.displayName,
                profileArtworkUrl = profile.artworkUrl ?: current.profileArtworkUrl
            )
        }
        applicationScope.launch(Dispatchers.IO) { downloads.reconcile() }
    }
}
