package dev.raflos.cocina.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** El Personal Access Token de GitHub, cifrado on-device. Es la única credencial de la app. */
class GithubTokenStore(context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "cocina_github",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getToken(): String? = prefs.getString(KEY_PAT, null)

    fun setToken(pat: String) {
        prefs.edit().putString(KEY_PAT, pat).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_PAT).apply()
    }

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    companion object {
        private const val KEY_PAT = "github_pat"
    }
}
