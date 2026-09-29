package com.engboost.encryptedca.feature.webpanel.api.model

import com.engboost.encryptedca.core.certificates.api.model.ClientCredentials

sealed interface ClientCertAnswer {
    data class Granted(val credentials: ClientCredentials) : ClientCertAnswer
    data object OtherHost : ClientCertAnswer
    data object NoProfile : ClientCertAnswer
    data object ProfileUnavailable : ClientCertAnswer
}
