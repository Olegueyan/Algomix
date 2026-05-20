package fr.olegueyan.algomix.infrastructure.cloud.supabase

import android.content.SharedPreferences

class SupabaseTokenStore(private val prefs: SharedPreferences) {
    fun load(): String? = prefs.getString(KEY_TOKEN, null)

    fun save(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()

    fun clear() = prefs.edit().remove(KEY_TOKEN).apply()

    companion object {
        private const val KEY_TOKEN = "access_token"
    }
}
