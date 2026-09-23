# Certificate profile prototype

`CertificateProfileStore` coordinates profile import and the SharedPreferences index.
`CertificateMaterialReader` reads and validates PKCS#12 and CA PEM input.
`EncryptedCaStorage` preserves the existing encrypted CA file format and Android Keystore AES key.
`CertificateImportViewModel` owns a running import, while `CertificateProfileImportFragment` only collects input and renders state.

The app opens `CertificateProfileListFragment`. `CertificateProfileListViewModel` loads the index and runs selection/deletion off the UI thread. `ProfileConfirmationDialog` preserves a pending confirmation across rotation. Import is a separate fragment on the back stack; completing it returns to the list without selecting the new profile. Back navigation from the form is available before import; while the write is running, it asks the user to wait. Deleting the active profile clears the selection. Existing index, aliases and CA files are unchanged.

Import order: read PKCS#12 and CA → save the client key → encrypt CA → register the profile. The rollback is compensating, not an atomic transaction across Android Keystore, file storage and SharedPreferences.

The caller owns input streams and closes them. `CertificateProfileStore.importProfile` takes ownership of its password `char[]` and clears it. The ViewModel also clears the array if a document cannot be opened before the store receives it. Passwords are not saved in UI state, bundles, files or logs.

The instrumentation fixtures are in `app/src/androidTest/assets`. Run the device tests with:

`gradlew connectedDebugAndroidTest`

This command builds and executes instrumentation tests on a connected Android device or emulator.
