package fr.previensmoi.data.local

import android.content.Context
import android.util.AtomicFile
import fr.previensmoi.data.remote.PendingCommands
import java.io.File

class FilePendingCommands(context: Context) : PendingCommands {
    private val directory = context.noBackupFilesDir
    private fun file(owner: String): AtomicFile {
        require(Regex("[0-9a-f-]{36}").matches(owner))
        return AtomicFile(File(directory, "command-$owner"))
    }
    @Synchronized override fun read(owner: String): String? = file(owner).let { f -> if (f.baseFile.exists()) f.openRead().bufferedReader().use { it.readText() } else null }
    @Synchronized override fun write(owner: String, id: String) {
        require(Regex("[0-9a-f-]{36}").matches(id))
        val f = file(owner); val out = f.startWrite()
        try { out.write(id.encodeToByteArray()); f.finishWrite(out) } catch (e: Exception) { f.failWrite(out); throw e }
    }
    @Synchronized override fun clear(owner: String) { file(owner).delete() }
}
