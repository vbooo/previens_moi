package fr.previensmoi.app

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import fr.previensmoi.BuildConfig
import fr.previensmoi.data.auth.Credentials
import fr.previensmoi.data.auth.KeystoreSessionVault
import fr.previensmoi.data.push.DeviceInstallation
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicitly opted-in cloud transport checks using an isolated synthetic account.
 * No email-delivery or real monitoring-quality claim. No server key enters the APK.
 */
@RunWith(AndroidJUnit4::class)
class CloudConnectionTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun enabled() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("cloudTestEnabled") == "true")
        check(!BuildConfig.LOCAL_SERVICE && BuildConfig.SERVICE_URL.startsWith("https://") && URL(BuildConfig.SERVICE_URL).host.endsWith(".supabase.co"))
    }
    @Test fun bootstrapSyntheticSessionAndRegisterRealFcmDevice() {
        enabled()
        val descriptor = instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/previens-moi-cloud-session.json")
        val raw = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        val session = Json.parseToJsonElement(raw).jsonObject
        check(session["user"]!!.jsonObject["email"]!!.jsonPrimitive.content.endsWith("@example.test"))
        val user = session["user"]!!.jsonObject["id"]!!.jsonPrimitive.content
        val access = session["access_token"]!!.jsonPrimitive.content
        KeystoreSessionVault(context).write(Credentials(user,access,session["refresh_token"]!!.jsonPrimitive.content,System.currentTimeMillis()+session["expires_in"]!!.jsonPrimitive.long*1000))
        val account = URL(BuildConfig.SERVICE_URL+"api/v1/account").openConnection() as HttpURLConnection
        account.setRequestProperty("Authorization","Bearer $access");account.connectTimeout=15000;account.readTimeout=15000
        assertEquals("Authenticated cloud account HTTP status",200,account.responseCode)
        val value = account.inputStream.bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject };account.disconnect()
        assertEquals(user,value["id"]!!.jsonPrimitive.content)
        val token = Tasks.await(FirebaseMessaging.getInstance().token,90,TimeUnit.SECONDS)
        assertTrue("FCM registration token available",token.isNotBlank())
        val device=DeviceInstallation(context).id
        val connection=URL(BuildConfig.SERVICE_URL+"api/v1/devices/$device").openConnection() as HttpURLConnection
        connection.requestMethod="PUT";connection.doOutput=true;connection.connectTimeout=15000;connection.readTimeout=15000
        connection.setRequestProperty("Authorization","Bearer $access");connection.setRequestProperty("Content-Type","application/json")
        connection.outputStream.use { it.write(buildJsonObject { put("fcmToken",token);put("language","fr") }.toString().encodeToByteArray()) }
        assertEquals("Cloud device registration HTTP status",204,connection.responseCode);connection.disconnect()
    }
    @Test fun realFcmResultAppearsAsAndroidNotification() {
        enabled()
        val expected=checkNotNull(InstrumentationRegistry.getArguments().getString("findingId"))
        val manager=context.getSystemService(NotificationManager::class.java)
        val deadline=System.currentTimeMillis()+120000
        while(System.currentTimeMillis()<deadline) {
            if(manager.activeNotifications.any { it.tag==expected })return
            Thread.sleep(500)
        }
        fail("No Android notification received for controlled cloud result within 120 seconds")
    }
}
