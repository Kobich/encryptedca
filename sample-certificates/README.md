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

For local development only. Don't use these certificates in production and don't commit real keys.
