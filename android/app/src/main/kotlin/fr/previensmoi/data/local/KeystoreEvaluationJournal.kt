package fr.previensmoi.data.local

import android.content.Context
import fr.previensmoi.data.auth.KeystoreBlob
import fr.previensmoi.data.remote.EvaluationCache
import fr.previensmoi.data.remote.EvaluationJournal
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KeystoreEvaluationJournal(context: Context) : EvaluationJournal {
    private val blob = KeystoreBlob(context,"evaluation.enc","evaluation.v1")
    override fun read(): EvaluationCache? = blob.read()?.let { Json.decodeFromString<EvaluationCache>(it.decodeToString()) }
    override fun write(value: EvaluationCache) = blob.write(Json.encodeToString(value).encodeToByteArray())
}
