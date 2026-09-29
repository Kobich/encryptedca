# EncryptedCA

Android app that finds hardware devices in the Wi-Fi network the phone is connected to and checks them over mutual TLS. The device is always the TLS client: it authenticates with the key from a `.p12` and trusts a server only if its certificate chains to the profile's `ca.pem`.

## Modules

Three top-level folders, one per layer. Every module in them is either `api` (what other modules may use) or `impl` (the hidden implementation).

| Folder | Layer | `api` | `impl` |
| --- | --- | --- | --- |
| `core/` | Data shared by features | Interfaces and models: `CertificateProfileRepository`, `WifiMonitor`, `DeviceScanner`, `SslContextFactory` | Keystore, encrypted CA, profile index, Wi-Fi, subnet scan, TLS |
| `feature/` | A feature's logic, no Compose | Interactor interfaces and models, one interactor per screen | `interactor/` implementations, `data/` sources, `di/` |
| `ui/` | Presentation | Route, `navigateTo<X>()` and `interface <X>Ui { @Composable fun Content(...) }` | Screens, ViewModels, `di/` |

Dependencies go one way: `ui/<x>/impl` → `feature/<x>/api` → `core/*/api`. An `impl` is seen only by `:app`, which starts Koin with every module's `di` and hosts the navigation: it takes each `<X>Ui` from Koin and calls `Content`. Features don't depend on each other.

| Feature | What it does |
| --- | --- |
| `certificates` | Certificate profiles: list, selection, deletion, adding from files, camera QR or photo QR |
| `scanner` | Devices in the current Wi-Fi network and whether they pass the mTLS check |
| `webpanel` | The device's web panel in a WebView with the profile's client certificate |

### Inside `ui/<x>/impl`

One folder per screen, e.g. `list/`, `add/`:

| File | Contents |
| --- | --- |
| `<Screen>Screen.kt` | A whole screen. It takes `state`, one `onAction` and navigation callbacks |
| `<Screen>ViewModel.kt` | Turns actions into state; talks only to its interactor from `feature/<x>/api` |
| `<Screen>State.kt` | What the screen shows |
| `<Screen>Action.kt` | Everything the user can do on the screen |
| `components/` | Parts of the screen's UI, never whole screens |

A screen folder may hold several screens of one flow: `add/` has `AddProfileFormScreen`, `QrCameraScreen` and `QrPhotosScreen`, and `AddProfileScreen` picks which one to show.

Comments: only a short header at the top of a file whose purpose isn't obvious from the code.

## Certificate storage

- The `.p12` is not kept. Its private key and chain go into Android Keystore under `mtls_client_<profileId>`, not extractable. RSA keys also allow raw private-key operations: Conscrypt needs them for RSA-PSS in TLS 1.3.
- The CA is not kept as PEM. Its DER is encrypted with AES-256-GCM (Keystore key `mtls_ca_storage_key`) and stored as `[IV length][IV][ciphertext]` in `noBackupFilesDir/<profileId>.ca.enc`.
- The profile index (ids, names, creation time, active profile) is in SharedPreferences `certificate_profiles` and excluded from backup, since Keystore keys are never restored.
- Passwords are never stored. `importProfile` takes ownership of the password `CharArray` and clears it.

`CertificateProfileRepository` is the only public entry point; the storages behind it are internal. It runs calls one at a time, changes can't be cancelled halfway, and the index is published as a `StateFlow`. `loadActiveCredentials()` returns the selected profile's Keystore key handle, its certificate chain and CA; `:core:network` builds the `SSLContext` from them and never sees Keystore aliases. Import order is: read PKCS#12 and CA → save the client key → encrypt CA → register the profile; a failure is rolled back step by step.

An empty password opens containers exported without one: Android's PKCS#12 provider expects a single NUL character for them. The selected CA may be a legacy self-signed certificate without `BasicConstraints CA:TRUE`.

Sample certificates for manual import are in `sample-certificates/` (password `1234`).

## Device scan

The scan uses the Wi-Fi network from `ConnectivityManager` (a network without internet access counts too) and binds sockets to it, so traffic doesn't leak to mobile data. For every host of the subnet (narrowed to /24 if wider) it opens a TCP connection to port 443 and, if the port is open, performs a TLS handshake with the active profile. A completed handshake marks the device as trusted. A host whose port can't be reached is still listed when the connection is refused or it answers `ping`. Ping runs as a separate process and follows the default route, not the Wi-Fi network, so it may miss hosts when Wi-Fi isn't the default; it never affects whether a device is trusted. Devices are addressed by IP, so the server certificate is checked only against the profile's CA.

## Device web panel

Tapping a device that passed the check opens `https://<ip>:443/` in a WebView. The scan remembers the SHA-256 of the certificate the device presented over mTLS; WebView doesn't know the profile's CA, reports the device as an SSL error, and the page proceeds only with that exact certificate on that IP. The client key is given only to the device's host, links to other hosts open in the browser, and WebView's remembered client-certificate choice is cleared first, so a profile change takes effect. Screenshots the panel offers as `data:image/...` downloads are saved to DCIM.

## Import from QR codes

Adding a profile always starts from the `+` on the profile list, which offers three sources: two files, QR codes with the camera, or QR codes from photos (screenshots or pictures of the printed sheet). Each source has its own screen; closing the camera or the photos screen goes back to the list. The codes can be read in any order, several per camera frame or photo, and photos can be added over several picks until every code is read. The camera uses CameraX with the ML Kit QR model bundled in the APK, so Google Play services aren't required. The code format is described in [docs/qr-profile-format.md](docs/qr-profile-format.md), and `tools/qr/make_profile_qr.py` generates codes from any `.p12` and CA; the `.p12` password is never in the codes and is entered as usual. The collected profile stays in memory only.
