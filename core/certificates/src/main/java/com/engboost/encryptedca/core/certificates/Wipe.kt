package com.engboost.encryptedca.core.certificates

/** Overwrites a password in memory once it's no longer needed. */
fun CharArray.wipe() = fill('\u0000')
