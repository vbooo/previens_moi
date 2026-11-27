package fr.previensmoi.data.billing

import fr.previensmoi.data.auth.*
import fr.previensmoi.data.remote.*
import fr.previensmoi.domain.model.*
import java.io.IOException
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

/**
 * Recreates the session and repository over serialized storage, with all network calls controlled.
 */
class OfflineSubscriptionTest {
    private class Disk : SubscriptionCache {
        var bytes: String? = null

        override fun read() = bytes?.let { Json.decodeFromString<SubscriptionSnapshot>(it) }

        override fun write(value: SubscriptionSnapshot) {
            bytes = Json.encodeToString(value)
        }

        override fun clear() {
            bytes = null
        }
    }

    private class Vault(var user: String = "owner", var expired: Boolean = false) : SessionVault {
        override fun read() =
            Credentials(user, "access", "refresh", if (expired) 0 else Long.MAX_VALUE)

        override fun write(value: Credentials) {}

        override fun clear() {}
    }

    private fun offline(args: Array<out Any?>?): Any {
        @Suppress("UNCHECKED_CAST") val continuation = args!!.last() as Continuation<Any?>
        continuation.resumeWith(Result.failure(IOException("controlled offline")))
        return COROUTINE_SUSPENDED
    }

    private inner class Fixture {
        var now = 1_000_000L
        val expiry = 2_000_000L
        val disk = Disk()
        val vault = Vault()
        var offline = false
        var code = 200
        var body =
            SubscriptionAccessDto(
                "active",
                true,
                java.time.Instant.ofEpochMilli(expiry).toString(),
                3,
                true,
                "a".repeat(64),
                null,
                true,
                "fr.previensmoi.app",
                "previens_moi_plus",
                "monthly",
            )
        private val auth =
            Proxy.newProxyInstance(
                AuthApi::class.java.classLoader,
                arrayOf(AuthApi::class.java),
            ) { _, _, args ->
                offline(args)
            } as AuthApi
        private val api =
            Proxy.newProxyInstance(
                ApplicationApi::class.java.classLoader,
                arrayOf(ApplicationApi::class.java),
            ) { _, _, args ->
                if (offline) return@newProxyInstance offline(args)
                if (code == 200) Response.success(body)
                else
                    Response.error<SubscriptionAccessDto>(
                        code,
                        "{}".toResponseBody(),
                    )
            } as ApplicationApi

        fun repository(cache: SubscriptionCache = disk) =
            RemoteSubscriptionRepository(
                api,
                RemoteSessionRepository(auth, vault, Clock { now }),
                cache,
                Clock { now },
            )

        suspend fun seed() {
            assertTrue((repository().access() as BillingOutcome.Success).value.active)
        }

        fun access(result: BillingOutcome<SubscriptionAccess>) =
            (result as BillingOutcome.Success).value
    }

    @Test
    fun `confirmed subscription survives process recreation and offline startup`() = runTest {
        val f = Fixture()
        f.seed()
        f.offline = true
        val access = f.access(f.repository().access())
        assertTrue(access.active)
        assertTrue(access.fromCache)
        assertFalse(access.canPurchase)
        assertEquals(3, access.remainingCreations)
        assertEquals(f.expiry, access.expiresAt)
    }

    @Test
    fun `expired login token with no network does not erase the paid subscription`() = runTest {
        val f = Fixture()
        f.seed()
        f.vault.expired = true
        f.offline = true
        assertTrue(f.access(f.repository().access()).active)
    }

    @Test
    fun `confirmed purchase is persisted but a new purchase cannot be validated offline`() =
        runTest {
            val f = Fixture()
            assertTrue(f.access(f.repository().verify("synthetic-token")).active)
            assertFalse(f.disk.bytes!!.contains("synthetic-token"))
            f.offline = true
            assertEquals(
                BillingOutcome.Error(BillingProblem.Offline),
                f.repository().verify("new-token"),
            )
            assertTrue(f.access(f.repository().access()).active)
        }

    @Test
    fun `another account never inherits cached subscription or quota`() = runTest {
        val f = Fixture()
        f.seed()
        f.vault.user = "another-owner"
        f.offline = true
        assertEquals(BillingOutcome.Error(BillingProblem.Offline), f.repository().access())
        assertNull(f.disk.bytes)
    }

    @Test
    fun `offline state is never extended beyond the confirmed expiration or by rolling clock back`() =
        runTest {
            val f = Fixture()
            f.seed()
            f.offline = true
            f.now = f.expiry
            assertEquals(BillingOutcome.Error(BillingProblem.Offline), f.repository().access())
            f.offline = false
            f.now = 1_000_000L
            f.seed()
            f.now -= 1
            f.offline = true
            assertEquals(BillingOutcome.Error(BillingProblem.Offline), f.repository().access())
        }

    @Test
    fun `payment hold and expiration replace previous active cache after reconnection`() = runTest {
        for (status in listOf("hold", "expired", "paused", "none")) {
            val f = Fixture()
            f.seed()
            f.body = f.body.copy(status = status, active = false)
            val online = f.access(f.repository().access())
            assertFalse(online.active)
            assertFalse(online.fromCache)
            f.offline = true
            val offline = f.access(f.repository().access())
            assertFalse(offline.active)
            assertEquals(online.status, offline.status)
        }
    }

    @Test
    fun `cancellation and grace keep only the period already confirmed by server`() = runTest {
        for (status in listOf("cancelled", "grace")) {
            val f = Fixture()
            f.body = f.body.copy(status = status)
            f.seed()
            f.offline = true
            assertTrue(f.access(f.repository().access()).active)
            f.now = f.expiry
            assertEquals(BillingOutcome.Error(BillingProblem.Offline), f.repository().access())
        }
    }

    @Test
    fun `no cache or unreadable storage never invents premium and write failure keeps verified access`() =
        runTest {
            val f = Fixture()
            f.offline = true
            assertEquals(BillingOutcome.Error(BillingProblem.Offline), f.repository().access())
            f.disk.bytes = "broken encrypted payload"
            assertEquals(BillingOutcome.Error(BillingProblem.Offline), f.repository().access())
            assertNull(f.disk.bytes)
            f.offline = false
            val broken =
                object : SubscriptionCache {
                    override fun read(): SubscriptionSnapshot? = throw IOException()

                    override fun write(value: SubscriptionSnapshot) {
                        throw IOException()
                    }

                    override fun clear() {}
                }
            assertTrue(f.access(f.repository(broken).access()).active)
        }

    @Test
    fun `server rejection or invalid response never falls back to old premium`() = runTest {
        val f = Fixture()
        f.seed()
        f.code = 503
        assertEquals(BillingOutcome.Error(BillingProblem.Verification), f.repository().access())
        f.code = 200
        f.body = f.body.copy(packageName = "other.app")
        assertEquals(BillingOutcome.Error(BillingProblem.Verification), f.repository().access())
    }

    @Test
    fun `real HTTP connection failure after repository recreation uses confirmed state`() =
        runTest {
            val f = Fixture()
            f.seed()
            val server = mockwebserver3.MockWebServer().apply { start() }
            val api = ApiClient.create(server.url("/functions/v1/"), true)
            val auth = createAuthApi(server.url("/auth/v1/"), "public-fixture", true)
            server.close()
            val restarted =
                RemoteSubscriptionRepository(
                    api,
                    RemoteSessionRepository(auth, f.vault, Clock { f.now }),
                    f.disk,
                    Clock { f.now },
                )
            val restored = f.access(restarted.access())
            assertTrue(restored.active)
            assertTrue(restored.fromCache)
            assertFalse(restored.canPurchase)
        }

    @Test
    fun `manual verification cannot report a cached subscription as a successful fresh check`() =
        runTest {
            val f = Fixture()
            f.seed()
            f.offline = true
            assertTrue(f.access(f.repository().access()).active)
            assertEquals(BillingOutcome.Error(BillingProblem.Offline), f.repository().check())
        }
}
