// The profile list (ids, names, creation time, active profile) in SharedPreferences.
// Don't change the file name or the keys: phones already have data stored this way.
// Writes use commit(), not apply(): the caller must learn about a failure to roll back.
// Removing the active profile also clears the selection.
package com.engboost.encryptedca.core.certificates.impl.data

import android.content.Context
import android.content.SharedPreferences
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError.STORAGE_FAILED
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileException
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import com.engboost.encryptedca.core.certificates.api.entity.ProfileSummary
import com.engboost.encryptedca.core.certificates.impl.domain.ProfileIndexRepository

internal class ProfileIndexRepositoryImpl(context: Context) : ProfileIndexRepository {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override val activeProfileId: String? get() = preferences.getString(ACTIVE_ID, null)

    override fun read(): ProfileIndex = ProfileIndex(
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

    override fun contains(profileId: String): Boolean = profileId in storedIds()

    override fun setActive(profileId: String) = commit("Could not save active profile") { putString(ACTIVE_ID, profileId) }

    override fun register(profileId: String, displayName: String?) {
        val ids = storedIds().apply { add(profileId) }
        commit("Could not persist certificate profile index") {
            putStringSet(IDS, ids)
            putString(DISPLAY_NAME_PREFIX + profileId, displayName)
            putLong(CREATED_AT_PREFIX + profileId, System.currentTimeMillis())
        }
    }

    override fun unregister(profileId: String) {
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
