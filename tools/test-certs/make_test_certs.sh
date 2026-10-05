#!/bin/sh
# Certificates for the unit tests, written into the modules' src/test/resources.
# They are valid for 100 years so the tests don't break when they expire; expired.pem expired in 2001.
# The .p12 password is "test". Needs OpenSSL 3.4+ (-not_before / -not_after).
# Run from anywhere: sh tools/test-certs/make_test_certs.sh  (OPENSSL=/path/to/openssl to pick a binary)
set -eu
cd "$(dirname "$0")/../.."
# Git Bash on Windows would turn "/CN=..." into a file path.
export MSYS_NO_PATHCONV=1
openssl=${OPENSSL:-openssl}
work=build/test-certs
rm -rf "$work"
mkdir -p "$work"
days=36500

ca() {
    "$openssl" req -x509 -newkey rsa:2048 -nodes -keyout "$work/$1.key" -out "$work/$1.pem" -days "$days" \
        -subj "/CN=Test $1" -addext "basicConstraints=critical,CA:TRUE" -addext "keyUsage=critical,keyCertSign"
}

leaf() {
    "$openssl" req -newkey rsa:2048 -nodes -keyout "$work/$1.key" -out "$work/$1.csr" -subj "/CN=Test $1"
    "$openssl" x509 -req -in "$work/$1.csr" -CA "$work/$2.pem" -CAkey "$work/$2.key" -days "$days" -out "$work/$1.pem"
    "$openssl" pkcs12 -export -inkey "$work/$1.key" -in "$work/$1.pem" -out "$work/$1.p12" -passout pass:test
}

ca ca
ca other-ca
leaf client ca
leaf server ca
leaf other-server other-ca
"$openssl" req -x509 -newkey rsa:2048 -nodes -keyout "$work/expired.key" -out "$work/expired.pem" \
    -subj "/CN=Test expired" -not_before 20000101000000Z -not_after 20010101000000Z

certificates=core/certificates/impl/src/test/resources
network=core/network/impl/src/test/resources
mkdir -p "$certificates" "$network"
cp "$work/ca.pem" "$work/client.p12" "$work/expired.pem" "$certificates/"
cp "$work/ca.pem" "$work/other-ca.pem" "$work/client.p12" "$work/server.p12" "$work/other-server.p12" "$network/"
echo "Test certificates written to $certificates and $network"
