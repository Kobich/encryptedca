package com.engboost.encryptedca.ui.addprofile.api

import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource

object AddProfileNavRoute {
    const val SOURCE_ARG = "source"
    const val ROUTE = "add-profile/{$SOURCE_ARG}"

    fun build(source: ProfileSource) = "add-profile/${source.name}"
}
