package com.lumenui.app.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.lumenui.app.util.AppLog
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES/GCM encryption backed by the Android Keystore. The master key never leaves
 * the secure hardware/keystore, so persisted secrets (the mihomo API secret) are
 * unreadable from the on-disk prefs alone.
 */
object Crypto {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "lumenui_master_key"

    /** Alias used before the app was renamed; kept only so old ciphertext still decrypts. */
    private const val LEGACY_ALIAS = "zash_master_key"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val PLAIN_PREFIX = "plain:"

    /** Looks a key up without ever generating one, so decrypting can't create state. */
    private fun existingKey(alias: String): SecretKey? {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun secretKey(): SecretKey = existingKey(ALIAS) ?: run {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        generator.generateKey()
    }

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        }.getOrElse { error ->
            // The Keystore is unavailable, so the secret is persisted with obfuscation only.
            // That is a real downgrade, so say so loudly rather than degrading silently.
            AppLog.e("Keystore encryption failed; the secret is stored unencrypted", error)
            PLAIN_PREFIX + Base64.encodeToString(plain.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        if (stored.startsWith(PLAIN_PREFIX)) {
            val raw = stored.removePrefix(PLAIN_PREFIX)
            return runCatching { String(Base64.decode(raw, Base64.NO_WRAP), Charsets.UTF_8) }
                .getOrElse { error ->
                    AppLog.w("Stored secret is not valid base64", error)
                    ""
                }
        }
        existingKey(ALIAS)?.let { decryptWith(stored, it) }?.let { return it }
        existingKey(LEGACY_ALIAS)?.let { decryptWith(stored, it) }?.let { return it }
        // Either the key was removed (e.g. restored from a backup) or the value is corrupt.
        AppLog.w("Could not decrypt the stored secret; the Keystore key is missing or unusable")
        return ""
    }

    private fun decryptWith(stored: String, key: SecretKey): String? = runCatching {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        if (bytes.size <= IV_LEN) return null
        val iv = bytes.copyOfRange(0, IV_LEN)
        val body = bytes.copyOfRange(IV_LEN, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(body), Charsets.UTF_8)
    }.getOrNull()
}
