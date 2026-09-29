# Sample certificates

A self-contained mTLS pair for importing by hand through the Android file picker:

- `client.p12` — client private key and certificate;
- `ca.pem` — self-signed certificate authority.

The `client.p12` password is `1234`.

The client key is RSA 4096. The container was created with:

```shell
openssl pkcs12 -export -legacy -out client.p12 -inkey client_key.pem -in client.pem
```

The key is protected with 3DES, the certificate with RC2, the MAC is SHA-1. The CA is shipped separately and is not included in the `.p12`. This is the legacy format the import has to handle, not a recommendation for production.

`qr/` holds the same pair as QR codes in the format of `docs/qr-profile-format.md`: `profile_1.png`…`profile_8.png` one by one and `profile_sheet.png` with all of them on one sheet, for testing the QR import from the screen or from an image.

For local development only. Don't use these certificates in production and don't commit real keys.
