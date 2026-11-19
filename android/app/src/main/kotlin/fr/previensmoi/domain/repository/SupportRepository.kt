package fr.previensmoi.domain.repository

import fr.previensmoi.domain.model.Outcome

data class SupportMessage(
    val id: String,
    val category: String,
    val message: String,
    val replyEmail: String?,
    val appVersion: String,
    val language: String,
) {
    override fun toString() = "SupportMessage(id=$id, content=redacted)"
}

interface SupportRepository {
    /** Success means durable receipt by support, not proof that an email was read. */
    suspend fun submit(message: SupportMessage): Outcome<Unit>
}
