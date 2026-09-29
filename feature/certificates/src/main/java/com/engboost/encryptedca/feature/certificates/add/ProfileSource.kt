package com.engboost.encryptedca.feature.certificates.add

/** Where a new profile's .p12 and CA come from. Picked on the profile list and fixed for the whole add flow. */
internal enum class ProfileSource {
    /** Two documents: the client .p12 and the CA certificate. */
    FILES,

    /** QR codes read with the camera. */
    QR_CAMERA,

    /** QR codes read from pictures: screenshots or photos of the printed sheet. */
    QR_PHOTOS,
}
