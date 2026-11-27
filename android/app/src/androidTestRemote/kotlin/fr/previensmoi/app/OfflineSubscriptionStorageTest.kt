package fr.previensmoi.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.previensmoi.data.billing.*
import fr.previensmoi.data.remote.SubscriptionAccessDto
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineSubscriptionStorageTest {
    @Test
    fun lastConfirmedSubscriptionSurvivesRecreationAndStaysEncrypted() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = KeystoreSubscriptionCache(context)
        // Optional two-instrumentation probe: seed, force-stop the app, then read.
        val phase =
            androidx.test.platform.app.InstrumentationRegistry.getArguments()
                .getString("offlinePhase")
        val expected =
            SubscriptionSnapshot(
                "synthetic-owner",
                1_000_000L,
                SubscriptionAccessDto(
                    "active",
                    true,
                    "2027-01-01T00:00:00Z",
                    3,
                    false,
                    "a".repeat(64),
                    null,
                    false,
                    "fr.previensmoi.app",
                    "previens_moi_plus",
                    "monthly",
                ),
            )
        if (phase == "seed") {
            first.write(expected)
            return
        }
        if (phase == "read") {
            try {
                assertEquals(expected, first.read())
            } finally {
                first.clear()
            }
            return
        }
        first.clear()
        try {
            val access =
                SubscriptionAccessDto(
                    "active",
                    true,
                    "2027-01-01T00:00:00Z",
                    3,
                    false,
                    "a".repeat(64),
                    null,
                    false,
                    "fr.previensmoi.app",
                    "previens_moi_plus",
                    "monthly",
                )
            first.write(SubscriptionSnapshot("synthetic-owner", 1_000_000L, access))
            val recreated = KeystoreSubscriptionCache(context)
            assertEquals(
                SubscriptionSnapshot("synthetic-owner", 1_000_000L, access),
                recreated.read(),
            )
            val file = java.io.File(context.noBackupFilesDir, "subscription.enc")
            assertTrue(file.isFile)
            val disk = file.readBytes().decodeToString()
            assertFalse(disk.contains("synthetic-owner"))
            assertFalse(disk.contains("previens_moi_plus"))
            file.writeText("corrupt ciphertext")
            assertTrue(runCatching { recreated.read() }.isFailure)
            recreated.clear()
            assertNull(KeystoreSubscriptionCache(context).read())
        } finally {
            first.clear()
        }
    }
}
