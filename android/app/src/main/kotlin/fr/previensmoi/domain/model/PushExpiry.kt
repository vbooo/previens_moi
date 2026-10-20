package fr.previensmoi.domain.model

/** FCM and queued device work share a single bounded expiry. */
fun pushExpiry(sentAt: Long, ttlSeconds: Int, now: Long): Long? {
    if (sentAt <= 0 || sentAt > now + 300_000 || ttlSeconds !in 1..86_400) return null
    val expires = sentAt + ttlSeconds * 1000L
    return expires.takeIf { it > now }
}
