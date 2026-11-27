package fr.previensmoi.data.billing

import android.content.Context
import fr.previensmoi.data.auth.KeystoreBlob
import fr.previensmoi.data.remote.SubscriptionAccessDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SubscriptionSnapshot(
    val owner: String,
    val verifiedAt: Long,
    val access: SubscriptionAccessDto,
) {
    override fun toString() = "SubscriptionSnapshot([redacted])"
}

interface SubscriptionCache {
    fun read(): SubscriptionSnapshot?

    fun write(value: SubscriptionSnapshot)

    fun clear()
}

/** Last server-confirmed state only; encrypted and excluded from backup/device transfer. */
class KeystoreSubscriptionCache(context: Context) : SubscriptionCache {
    private val blob = KeystoreBlob(context, "subscription.enc", "subscription.v1", 32768)

    override fun read(): SubscriptionSnapshot? =
        blob.read()?.let { Json.decodeFromString<SubscriptionSnapshot>(it.decodeToString()) }

    override fun write(value: SubscriptionSnapshot) =
        blob.write(Json.encodeToString(value).encodeToByteArray())

    override fun clear() = blob.clear()
}
