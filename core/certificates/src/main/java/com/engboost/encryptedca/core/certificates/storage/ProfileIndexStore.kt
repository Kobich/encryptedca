// Список профилей (id, имена, время создания, активный профиль) в SharedPreferences.
// Имя файла и ключи менять нельзя: на телефонах уже есть данные в этом формате.
// Запись идёт через commit(), а не apply(): вызывающий должен узнать об ошибке, чтобы откатить изменения.
// Если удаляется активный профиль, выбор тоже сбрасывается.
package com.engboost.encryptedca.core.certificates.storage

import android.content.Context
import android.content.SharedPreferences
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.STORAGE_FAILED
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.model.ProfileIndex
import com.engboost.encryptedca.core.certificates.model.ProfileSummary

internal class ProfileIndexStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val activeProfileId: String? get() = preferences.getString(ACTIVE_ID, null)

    fun read(): ProfileIndex = ProfileIndex(
        profiles = storedIds()
            .map { id ->
                ProfileSummary(
                    profileId = id,
                    displayName = preferences.getString(DISPLAY_NAME_PREFIX + id, null),
                    createdAt = preferences.getLong(CREATED_AT_PREFIX + id, 0L),
                )
            }
            .sortedWith(compareBy(ProfileSummary::createdAt, ProfileSummary::profileId)),
        activeProfileId = activeProfileId,
    )

    fun contains(profileId: String): Boolean = profileId in storedIds()

    fun setActive(profileId: String) = commit("Could not save active profile") { putString(ACTIVE_ID, profileId) }

    fun register(profileId: String, displayName: String?) {
        val ids = storedIds().apply { add(profileId) }
        commit("Could not persist certificate profile index") {
            putStringSet(IDS, ids)
            putString(DISPLAY_NAME_PREFIX + profileId, displayName)
            putLong(CREATED_AT_PREFIX + profileId, System.currentTimeMillis())
        }
    }

    fun unregister(profileId: String) {
        val ids = storedIds()
        if (!ids.remove(profileId)) return
        val wasActive = profileId == activeProfileId
        commit("Could not update certificate profile index") {
            putStringSet(IDS, ids)
            remove(DISPLAY_NAME_PREFIX + profileId)
            remove(CREATED_AT_PREFIX + profileId)
            if (wasActive) remove(ACTIVE_ID)
        }
    }

    private inline fun commit(failureMessage: String, edit: SharedPreferences.Editor.() -> Unit) {
        if (!preferences.edit().apply(edit).commit()) {
            throw CertificateProfileException(STORAGE_FAILED, failureMessage)
        }
    }

    private fun storedIds(): MutableSet<String> = LinkedHashSet(preferences.getStringSet(IDS, null).orEmpty())

    private companion object {
        const val PREFS = "certificate_profiles"
        const val IDS = "profile_ids"
        const val ACTIVE_ID = "active_profile_id"
        const val DISPLAY_NAME_PREFIX = "display_name_"
        const val CREATED_AT_PREFIX = "created_at_"
    }
}
