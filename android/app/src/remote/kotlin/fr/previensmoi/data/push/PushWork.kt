package fr.previensmoi.data.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.*
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import fr.previensmoi.R
import fr.previensmoi.app.MainActivity
import fr.previensmoi.data.auth.RemoteSessionRepository
import fr.previensmoi.data.remote.*
import fr.previensmoi.domain.model.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Check synchronous persistence before using the identifier for device registration.
@SuppressLint("UseKtx")
class DeviceInstallation(context: Context) {
    private val prefs = context.getSharedPreferences("installation",Context.MODE_PRIVATE)
    val id: String = prefs.getString("id",null) ?: UUID.randomUUID().toString().also { check(prefs.edit().putString("id",it).commit()) }
}
private suspend fun <T> Task<T>.result(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { if(c.isActive) c.resume(it) }
    addOnFailureListener { if(c.isActive) c.resumeWithException(it) }
}
object PushWork {
    fun register(context: Context, owner: String) { enqueue(context,owner,"register",null,null,null,null) }
    fun finding(context: Context, owner: String, finding: String, watch: String, version: String, expiresAt: Long) { enqueue(context,owner,"finding",finding,watch,version,expiresAt) }
    fun preparation(context: Context, owner: String, notice: String, watch: String, version: String, expiresAt: Long) { enqueue(context,owner,"preparation",notice,watch,version,expiresAt) }
    private fun enqueue(context: Context, owner: String, kind: String, finding: String?, watch: String?, version: String?, expiresAt: Long?) {
        val data = workDataOf("owner" to owner,"kind" to kind,"finding" to finding,"watch" to watch,"version" to version,"expiresAt" to expiresAt)
        val request = OneTimeWorkRequestBuilder<PushWorker>().setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).addTag("session-push").build()
        WorkManager.getInstance(context).enqueueUniqueWork("$owner:$kind:${finding ?: "device"}",ExistingWorkPolicy.REPLACE,request)
    }
}
@AndroidEntryPoint
class PushService : FirebaseMessagingService() {
    @Inject lateinit var session: RemoteSessionRepository
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) { session.currentUserId()?.let { PushWork.register(this,it) } }
    override fun onMessageReceived(message: RemoteMessage) {
        // Data-only messages. No notification payload can bypass authenticated reload.
        if (message.notification != null) return
        val preparation = message.data.keys == setOf("kind","noticeId","watchId","watchVersion") && message.data["kind"] == "preparation"
        if (!preparation && message.data.keys != setOf("findingId","watchId","planVersion")) return
        val id = message.data[if (preparation) "noticeId" else "findingId"] ?: return
        val watch = message.data["watchId"] ?: return
        val version = message.data[if (preparation) "watchVersion" else "planVersion"] ?: return
        if (runCatching { UUID.fromString(id); UUID.fromString(watch); require(version.toLong()>0) }.isFailure) return
        val expiresAt = pushExpiry(message.sentTime,message.ttl,System.currentTimeMillis()) ?: return
        session.currentUserId()?.let {
            if (preparation) PushWork.preparation(this,it,id,watch,version,expiresAt)
            else PushWork.finding(this,it,id,watch,version,expiresAt)
        }
    }
}
class PushWorker(context: Context, params: WorkerParameters,
    private val repository: RemoteRepositories, private val sessions: RemoteSessionRepository,
    private val api: ApplicationApi, private val installation: DeviceInstallation, private val claims: NotificationClaims,
) : CoroutineWorker(context,params) {
    @Suppress("DEPRECATION")
    override suspend fun doWork(): Result {
        val owner = inputData.getString("owner") ?: return Result.failure()
        if (sessions.currentUserId() != owner) return Result.success()
        if (runAttemptCount >= 3) return Result.failure()
        if (inputData.getString("kind") == "register") {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) return Result.failure()
            val token = try { FirebaseMessaging.getInstance().token.result() } catch (e: CancellationException) { throw e } catch (_: Exception) { return Result.retry() }
            val bearer = sessions.bearer() ?: return Result.failure()
            val language = java.util.Locale.getDefault().language.let { if(it == "fr") "fr" else "en" }
            val r = apiCall { api.registerDevice(bearer,installation.id,DeviceRegistrationDto(token,language)) }
            return if (r is ApiResult.Empty) Result.success() else Result.retry()
        }
        val expiresAt = inputData.getLong("expiresAt",0)
        if (System.currentTimeMillis() >= expiresAt) return Result.success()
        // Refresh the shared StateFlows even when notifications are disabled or the app is open.
        // A data-only push is a hint to re-read authenticated state, never trusted UI content.
        if (repository.refresh() != RefreshResult.Success) return Result.retry()
        if (sessions.currentUserId() != owner || System.currentTimeMillis() >= expiresAt) return Result.success()
        if (inputData.getString("kind") == "preparation") return preparation(owner,expiresAt)
        val findingId = inputData.getString("finding") ?: return Result.failure()
        val value = repository.getFinding(findingId)
        if (value is Outcome.Error) return if (value.reason == Failure.TemporaryFailure) Result.retry() else Result.success()
        val finding = (value as Outcome.Success).value
        if (finding.watchId != inputData.getString("watch") || finding.planVersion.toString() != inputData.getString("version") || sessions.currentUserId() != owner) return Result.success()
        val bearer = sessions.bearer() ?: return Result.success()
        val watchResponse = apiCall { api.watch(bearer,finding.watchId) }
        if (watchResponse !is ApiResult.Value) return if (watchResponse is ApiResult.ConnectionFailure) Result.retry() else Result.success()
        val current = watchResponse.body
        if (current.status !in setOf("active","completed") || current.plan.id != finding.planId || current.plan.version != finding.planVersion) return Result.success()
        return showFinding(owner,finding,expiresAt)
    }
    @Suppress("DEPRECATION")
    private suspend fun showFinding(owner: String, finding: Finding, expiresAt: Long): Result {
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled()) return Result.success()
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("findings",applicationContext.getString(R.string.notification_channel),NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(applicationContext,MainActivity::class.java).setAction("finding:${finding.id}").putExtra("findingId",finding.id)
        val pending = PendingIntent.getActivity(applicationContext,0,intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = if(Build.VERSION.SDK_INT>=26) Notification.Builder(applicationContext,"findings") else @Suppress("DEPRECATION") Notification.Builder(applicationContext)
        val resultTitle = finding.title.ifBlank { applicationContext.getString(R.string.finding_title) }
        val title = if (finding.noveltyBasis == "initial") applicationContext.getString(R.string.notification_result_found,resultTitle) else resultTitle
        val notification = builder.setSmallIcon(R.drawable.ic_demo).setContentTitle(title)
            .setContentText(finding.fact).setStyle(Notification.BigTextStyle().bigText(finding.fact)).setOnlyAlertOnce(true).setContentIntent(pending).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
        // Last local session fence before displaying; the fetched result was authorized above.
        if (sessions.currentUserId() == owner && System.currentTimeMillis() < expiresAt) {
            val first = try { claims.claim(owner,finding.id) } catch (e: CancellationException) { throw e } catch (_: Exception) { return Result.retry() }
            if (first && sessions.currentUserId() == owner) manager.notify(finding.id,0,notification)
        }
        return Result.success()
    }
    @Suppress("DEPRECATION")
    private suspend fun preparation(owner: String, expiresAt: Long): Result {
        val id = inputData.getString("finding") ?: return Result.failure()
        val watchId = inputData.getString("watch") ?: return Result.failure()
        val bearer = sessions.bearer() ?: return Result.success()
        val response = apiCall { api.watch(bearer,watchId) }
        if (response !is ApiResult.Value) return if (response is ApiResult.ConnectionFailure) Result.retry() else Result.success()
        val watch = response.body
        if (sessions.currentUserId() != owner || watch.version.toString() != inputData.getString("version") || !watch.plan.backgroundPreparation || watch.status !in setOf("active","paused","completed")) return Result.success()
        if (watch.status == "paused" && watch.health != "degraded") return Result.success()
        val initial = preparationFinding(watch.domain(),repository.observeFindings().first())
        if (initial != null) return showFinding(owner,initial,expiresAt)
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled()) return Result.success()
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("preparation",applicationContext.getString(R.string.notification_channel),NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(applicationContext,MainActivity::class.java).setAction("preparation:$id").putExtra("watchId",watchId)
        val pending = PendingIntent.getActivity(applicationContext,0,intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(applicationContext,"preparation") else Notification.Builder(applicationContext)
        val title = applicationContext.getString(if (watch.status in setOf("active","completed")) R.string.notification_ready else R.string.notification_preparation_failed)
        val notification = builder.setSmallIcon(R.drawable.ic_demo).setContentTitle(title).setContentText(applicationContext.getString(R.string.notification_preparation_body)).setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
        if (sessions.currentUserId() == owner && System.currentTimeMillis() < expiresAt) {
            val first = try { claims.claim(owner,id) } catch (e: CancellationException) { throw e } catch (_: Exception) { return Result.retry() }
            if (first && sessions.currentUserId() == owner) manager.notify(id,0,notification)
        }
        return Result.success()
    }

}
class PushWorkerFactory @Inject constructor(private val repository: RemoteRepositories, private val sessions: RemoteSessionRepository,
    private val api: ApplicationApi, private val installation: DeviceInstallation, private val claims: NotificationClaims) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        if (workerClassName == PushWorker::class.java.name) PushWorker(appContext,workerParameters,repository,sessions,api,installation,claims) else null
}
