package fr.previensmoi.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SupportRequestDto(
    val id: String,
    val category: String,
    val message: String,
    val replyEmail: String?,
    val appVersion: String,
    val language: String,
) {
    override fun toString() = "SupportRequestDto(id=$id, content=redacted)"
}

@Serializable data class SupportReceiptDto(val id: String, val state: String)
