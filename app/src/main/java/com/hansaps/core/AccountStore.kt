package com.hansaps.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.util.Locale

/**
 * Local-only account store for the V1 prototype. It stores password hashes rather than raw passwords.
 * Production accounts and authorization should be backed by a trusted server.
 */
object AccountStore {
    private const val PREFS = "hansaps_accounts_v1"
    private const val KEY_ACCOUNTS = "accounts_json"

    @Synchronized
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_ACCOUNTS)) return

        val initial = JSONArray()
            .put(accountJson("Rex", "RexStudio1@", UserRole.DEVELOPER))
            .put(accountJson("Han", "han123@@", UserRole.OWNER))
        prefs.edit().putString(KEY_ACCOUNTS, initial.toString()).apply()
    }

    fun login(context: Context, username: String, password: String): AppAccount? {
        initialize(context)
        if (username.isBlank() || password.isEmpty()) return null
        val normalized = username.trim().lowercase(Locale.ROOT)
        return readAccounts(context).firstOrNull {
            it.username.lowercase(Locale.ROOT) == normalized &&
                constantTimeEquals(it.passwordHash, hash(password, it.passwordSalt))
        }
    }

    @Synchronized
    fun createAccount(
        context: Context,
        username: String,
        password: String,
        role: UserRole,
        creatorRole: UserRole,
    ): Result<Unit> {
        initialize(context)
        if (creatorRole == UserRole.USER) {
            return Result.failure(IllegalArgumentException("Role ini tidak memiliki izin membuat akun."))
        }
        if (role == UserRole.DEVELOPER && creatorRole != UserRole.DEVELOPER) {
            return Result.failure(IllegalArgumentException("Hanya Developer yang boleh membuat akun Developer."))
        }
        val cleanName = username.trim()
        if (!cleanName.matches(Regex("[A-Za-z0-9_.-]{3,24}"))) {
            return Result.failure(IllegalArgumentException("Username 3–24 karakter; gunakan huruf, angka, titik, _ atau -."))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password minimal 8 karakter."))
        }

        val existing = readAccounts(context)
        if (existing.any { it.username.equals(cleanName, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Username sudah dipakai."))
        }

        val updated = JSONArray()
        existing.forEach { updated.put(accountJson(it.username, null, it.role, it.passwordHash, it.passwordSalt)) }
        updated.put(accountJson(cleanName, password, role))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACCOUNTS, updated.toString()).apply()
        return Result.success(Unit)
    }

    fun listAccounts(context: Context): List<AppAccount> {
        initialize(context)
        return readAccounts(context).sortedWith(compareBy({ it.role.ordinal }, { it.username.lowercase(Locale.ROOT) }))
    }

    private fun readAccounts(context: Context): List<AppAccount> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ACCOUNTS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val name = item.optString("username").trim()
                    val passHash = item.optString("passwordHash")
                    if (name.isNotEmpty() && passHash.isNotEmpty()) {
                        val salt = item.optString("passwordSalt")
                        if (salt.isNotBlank()) add(AppAccount(name, passHash, UserRole.fromStored(item.optString("role")), salt))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun accountJson(
        username: String,
        password: String?,
        role: UserRole,
        existingHash: String? = null,
        existingSalt: String? = null,
    ): JSONObject {
        val saltBytes = existingSalt?.let { Base64.decode(it, Base64.NO_WRAP) } ?: ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltText = existingSalt ?: Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val passHash = existingHash ?: hash(requireNotNull(password), saltText)
        return JSONObject().put("username", username)
            .put("passwordHash", passHash)
            .put("passwordSalt", saltText)
            .put("role", role.name)
    }

    private fun hash(password: String, saltText: String): String {
        val saltBytes = Base64.decode(saltText, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, 120_000, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}
