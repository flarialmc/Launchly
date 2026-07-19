package com.zeuroux.launchly

import android.app.Application
import com.zeuroux.launchly.reset.AppResetCoordinator
import com.zeuroux.launchly.reset.ResetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class LaunchlyApp : Application() {
    lateinit var container: AppContainer
        private set
    lateinit var resetCoordinator: AppResetCoordinator
        private set
    lateinit var resetResult: ResetResult
        private set

    override fun onCreate() {
        super.onCreate()
        resetCoordinator = AppResetCoordinator(this)
        resetResult = runBlocking {
            withContext(Dispatchers.IO) { resetCoordinator.runBeforeRepositories() }
        }
        container = DefaultAppContainer(this)
    }
}
