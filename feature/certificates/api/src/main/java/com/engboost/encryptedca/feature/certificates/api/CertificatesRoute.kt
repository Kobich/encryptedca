package com.engboost.encryptedca.feature.certificates.api

import androidx.navigation.NavController

object CertificatesRoute {
    const val ROUTE = "certificates"
}

fun NavController.navigateToCertificates() = navigate(CertificatesRoute.ROUTE)
