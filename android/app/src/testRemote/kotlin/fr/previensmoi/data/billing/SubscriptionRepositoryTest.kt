package fr.previensmoi.data.billing

import fr.previensmoi.data.auth.*
import fr.previensmoi.data.remote.*
import fr.previensmoi.domain.model.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import mockwebserver3.*
import org.junit.Assert.*
import org.junit.Test

class SubscriptionRepositoryTest {
    private class Vault : SessionVault {
        var credentials: Credentials? = Credentials("u", "access", "refresh", Long.MAX_VALUE)

        override fun read() = credentials

        override fun write(value: Credentials) {
            credentials = value
        }

        override fun clear() {
            credentials = null
        }
    }

    private fun response(body: String, code: Int = 200) =
        MockResponse.Builder()
            .code(code)
            .body(body)
            .addHeader("Content-Type", "application/json")
            .build()

    private fun body(
        status: String = "active",
        active: Boolean = true,
        packageName: String = "fr.previensmoi.app",
    ) =
        """{"status":"$status","active":$active,"expiresAt":"2027-01-01T00:00:00Z","remainingCreations":4,"canPurchase":false,"accountId":"${"a".repeat(64)}","trialEndsAt":null,"trialEnded":false,"packageName":"$packageName","productId":"previens_moi_plus","basePlanId":"monthly"}"""

    private fun repository(server: MockWebServer): RemoteSubscriptionRepository {
        val session =
            RemoteSessionRepository(
                createAuthApi(server.url("/auth/v1/"), "public", true),
                Vault(),
                Clock { 1000L },
            )
        return RemoteSubscriptionRepository(
            ApiClient.create(server.url("/functions/v1/"), true),
            session,
        )
    }

    @Test
    fun `server proof is authenticated and purchase token never appears in a URL or log representation`() =
        runTest {
            MockWebServer()
                .apply { start() }
                .use { server ->
                    server.enqueue(response(body()))
                    val repo = repository(server)
                    val result = repo.verify("synthetic-purchase-token") as BillingOutcome.Success
                    assertTrue(result.value.active)
                    assertEquals(4, result.value.remainingCreations)
                    val request = server.takeRequest(5, TimeUnit.SECONDS)!!
                    assertEquals("Bearer access", request.headers["Authorization"])
                    assertEquals(
                        "/functions/v1/api/v1/subscription/purchases",
                        request.url.encodedPath,
                    )
                    assertTrue(request.body!!.utf8().contains("synthetic-purchase-token"))
                    assertFalse(
                        PurchaseVerificationDto("synthetic-purchase-token")
                            .toString()
                            .contains("synthetic-purchase-token")
                    )
                }
        }

    @Test
    fun `mismatched catalog or pending entitlement is rejected instead of displayed active`() =
        runTest {
            MockWebServer()
                .apply { start() }
                .use { server ->
                    val repo = repository(server)
                    server.enqueue(response(body(packageName = "other.package.app")))
                    assertEquals(BillingOutcome.Error(BillingProblem.Verification), repo.access())
                    server.enqueue(response(body(status = "pending")))
                    assertEquals(BillingOutcome.Error(BillingProblem.Verification), repo.access())
                    server.enqueue(response(body(status = "hold", active = false)))
                    assertFalse((repo.access() as BillingOutcome.Success).value.active)
                }
        }

    @Test
    fun `purchase reuse denial stays distinct from service failure`() = runTest {
        MockWebServer()
            .apply { start() }
            .use { server ->
                val repo = repository(server)
                server.enqueue(
                    response(
                        """{"code":"purchase_account_mismatch","requestId":"test","retryAfterSeconds":null,"scope":null}""",
                        403,
                    )
                )
                assertEquals(
                    BillingOutcome.Error(BillingProblem.WrongAccount),
                    repo.verify("synthetic-purchase-token"),
                )
                server.enqueue(response("{}", 503))
                assertEquals(BillingOutcome.Error(BillingProblem.Verification), repo.access())
            }
    }
}
