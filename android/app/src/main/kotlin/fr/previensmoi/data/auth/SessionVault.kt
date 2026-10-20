package fr.previensmoi.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
class Credentials(val userId: String, val accessToken: String, val refreshToken: String, val expiresAt: Long) {
    override fun toString() = "Credentials([redacted])"
}
interface SessionVault {
    fun read(): Credentials?
    fun write(value: Credentials)
    fun clear()
}
// Atomic, authenticated encryption; the key never leaves Android Keystore. Both file and
// installation key are excluded from cloud backup/device transfer by the application manifest.
class KeystoreSessionVault(context: Context) : SessionVault {
    private val file = AtomicFile(File(context.noBackupFilesDir, "session.enc"))
    private val alias = context.packageName + ".session.v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized override fun read(): Credentials? {
        if (!file.baseFile.exists()) return null
        val bytes = file.openRead().use { it.readBytes() }
        require(bytes.size in 29..32768)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return Json.decodeFromString<Credentials>(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString())
    }
    @Synchronized override fun write(value: Credentials) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val output = file.startWrite()
        try {
            output.write(cipher.iv); output.write(cipher.doFinal(Json.encodeToString(value).encodeToByteArray()))
            file.finishWrite(output)
        } catch (e: Exception) { file.failWrite(output); throw e }
    }
    @Synchronized override fun clear() { file.delete() }
}
