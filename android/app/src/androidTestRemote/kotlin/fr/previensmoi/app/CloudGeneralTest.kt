package fr.previensmoi.app

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import fr.previensmoi.BuildConfig
import fr.previensmoi.data.auth.Credentials
import fr.previensmoi.data.auth.KeystoreSessionVault
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in, isolated synthetic-account checks. Never run on the user's emulator. */
@RunWith(AndroidJUnit4::class)
class CloudGeneralTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val args get() = InstrumentationRegistry.getArguments()
    private fun enabled() {
        assumeTrue(args.getString("cloudGeneralTestEnabled") == "true")
        check(!BuildConfig.LOCAL_SERVICE && URL(BuildConfig.SERVICE_URL).host.endsWith(".supabase.co"))
        check(android.os.Build.MODEL.contains("sdk") || android.os.Build.FINGERPRINT.contains("emulator"))
    }
    private fun findingId() = checkNotNull(args.getString("findingId"))
    @Test fun realPushWithPermissionDeniedKeepsResultAvailable() {
        enabled()
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        val credentials = checkNotNull(KeystoreSessionVault(context).read())
        val name = "${credentials.userId}:finding:${findingId()}"
        val deadline = System.currentTimeMillis() + 120_000
        var received = false
        while (System.currentTimeMillis() < deadline) {
            val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(name).get(5, TimeUnit.SECONDS)
            if (infos.any { it.state == WorkInfo.State.SUCCEEDED }) { received = true; break }
            Thread.sleep(500)
        }
        assertTrue("Actual FCM receipt must have completed its Android worker", received)
        assertFalse(context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.tag == findingId() })
        val connection = URL(BuildConfig.SERVICE_URL + "api/v1/findings/" + findingId()).openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer " + credentials.accessToken)
        connection.connectTimeout=15000;connection.readTimeout=15000
        assertEquals(200, connection.responseCode)
        assertEquals(findingId(), Json.parseToJsonElement(connection.inputStream.bufferedReader().use { it.readText() }).jsonObject["id"]!!.jsonPrimitive.content)
        connection.disconnect()
    }
    @Test fun prepareExpiredSyntheticSession() {
        enabled()
        val vault=KeystoreSessionVault(context);val old=checkNotNull(vault.read())
        vault.write(Credentials(old.userId,old.accessToken,"expired-synthetic-refresh",0))
        assertEquals(0,checkNotNull(vault.read()).expiresAt)
    }
    @Test fun rejectedRefreshClearedSyntheticVault() {
        enabled()
        assertNull(KeystoreSessionVault(context).read())
    }
}
