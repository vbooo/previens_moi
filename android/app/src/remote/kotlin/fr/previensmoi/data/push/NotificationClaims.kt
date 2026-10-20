package fr.previensmoi.data.push

import android.content.Context
import android.util.AtomicFile
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** IDs only, excluded from backup. Claim before display prevents duplicate alerts.
 * A process death after the claim can lose that push; the result remains in-app.
 */
class NotificationClaims(context: Context) {
    private val directory = context.noBackupFilesDir
    @Synchronized fun claim(owner: String, finding: String, now: Long = System.currentTimeMillis()): Boolean {
        require(Regex("[0-9a-f-]{36}").matches(owner) && Regex("[0-9a-f-]{36}").matches(finding))
        val file = AtomicFile(File(directory,"notification-claims-$owner"))
        val claims = if(file.baseFile.exists()) Json.decodeFromString<Map<String,Long>>(file.openRead().bufferedReader().use { it.readText() }).filterValues { it > now }.toMutableMap() else mutableMapOf()
        if(claims.containsKey(finding)) return false
        check(claims.size < 1000)
        claims[finding] = now + 90L * 86_400_000
        val output = file.startWrite()
        try { output.write(Json.encodeToString(claims).encodeToByteArray());file.finishWrite(output) }
        catch(e: Exception) { file.failWrite(output);throw e }
        return true
    }
}
