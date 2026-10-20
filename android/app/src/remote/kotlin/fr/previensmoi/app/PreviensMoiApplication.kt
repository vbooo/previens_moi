package fr.previensmoi.app

import android.app.Application
import android.app.NotificationManager
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import fr.previensmoi.BuildConfig
import fr.previensmoi.data.auth.RemoteSessionRepository
import fr.previensmoi.data.push.*
import javax.inject.Inject
import kotlinx.coroutines.*

@HiltAndroidApp
class PreviensMoiApplication : Application(), Configuration.Provider {
    @Inject lateinit var factory: PushWorkerFactory
    @Inject lateinit var sessions: RemoteSessionRepository
    override val workManagerConfiguration: Configuration get() = Configuration.Builder().setWorkerFactory(factory).build()
    override fun onCreate() {
        super.onCreate()
        val configured = listOf(BuildConfig.FIREBASE_APP_ID,BuildConfig.FIREBASE_PROJECT_ID,BuildConfig.FIREBASE_API_KEY,BuildConfig.FIREBASE_SENDER_ID).all { it.isNotBlank() }
        if (configured && FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this,FirebaseOptions.Builder().setApplicationId(BuildConfig.FIREBASE_APP_ID).setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY).setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID).build())
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            sessions.observeSession().collect { session ->
                if (session == null) { getSystemService(NotificationManager::class.java).cancelAll(); WorkManager.getInstance(this@PreviensMoiApplication).cancelAllWorkByTag("session-push") }
                else if (configured) PushWork.register(this@PreviensMoiApplication,session.id)
            }
        }
    }
}
