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
class Credentials(val userId: String, val accessToken: String, val refreshToken: String, val expiresAt: Long, val email: String = "") {
    override fun toString() = "Credentials([redacted])"
}
interface SessionVault {
    fun read(): Credentials?
    fun write(value: Credentials)
    fun clear()
}
// Atomic, authenticated encryption; the key never leaves Android Keystore. Both file and
// installation key are excluded from cloud backup/device transfer by the application manifest.
class KeystoreBlob(context: Context, name: String, purpose: String, private val maxBytes: Int = 131072) {
    private val file = AtomicFile(File(context.noBackupFilesDir, name))
    private val alias = context.packageName + "." + purpose
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun read(): ByteArray? {
        if (!file.baseFile.exists()) return null
        val bytes = file.openRead().use { it.readBytes() }
        require(bytes.size in 29..maxBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size))
    }
    @Synchronized fun write(value: ByteArray) {
        require(value.size + 28 <= maxBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val output = file.startWrite()
        try {
            output.write(cipher.iv); output.write(cipher.doFinal(value))
            file.finishWrite(output)
        } catch (e: Exception) { file.failWrite(output); throw e }
    }
    @Synchronized fun clear() { file.delete() }
}

class KeystoreSessionVault(context: Context) : SessionVault {
    private val blob = KeystoreBlob(context,"session.enc","session.v1",32768)
    override fun read(): Credentials? = blob.read()?.let { Json.decodeFromString<Credentials>(it.decodeToString()) }
    override fun write(value: Credentials) = blob.write(Json.encodeToString(value).encodeToByteArray())
    override fun clear() = blob.clear()
}
