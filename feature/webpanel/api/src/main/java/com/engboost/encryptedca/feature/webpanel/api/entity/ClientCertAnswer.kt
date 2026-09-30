package com.engboost.encryptedca.feature.webpanel.api.entity

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials

sealed interface ClientCertAnswer {
    data class Granted(val credentials: ClientCredentials) : ClientCertAnswer
    data object OtherHost : ClientCertAnswer
    data object NoProfile : ClientCertAnswer
    data object ProfileUnavailable : ClientCertAnswer
}
