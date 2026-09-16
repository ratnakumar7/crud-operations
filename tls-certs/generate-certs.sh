#!/usr/bin/env bash
# Generate a local test PKI for RADIUS TLS (RadSec / EAP-TLS).
# For lab use only. Do not use these keys in production.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${ROOT}/generated"
CNF="${ROOT}/openssl.cnf"
DAYS="${CERT_DAYS:-365}"
KEY_SIZE="${KEY_SIZE:-2048}"
SERVER_CN="${SERVER_CN:-radius.local}"
CLIENT_CN="${CLIENT_CN:-user@radius.local}"
P12_PASS="${P12_PASS:-changeit}"

usage() {
  cat <<'EOF'
Usage: ./generate-certs.sh

Creates a test CA, RADIUS server cert, and EAP/RadSec client cert under ./generated.

Environment overrides:
  CERT_DAYS     Validity in days (default: 365)
  KEY_SIZE      RSA key size (default: 2048)
  SERVER_CN     Server certificate CN (default: radius.local)
  CLIENT_CN     Client certificate CN (default: user@radius.local)
  P12_PASS      PKCS#12 password (default: changeit)

Files written:
  generated/ca/ca.crt              Trust this CA on clients and the RADIUS server
  generated/ca/ca.key              CA private key (keep private)
  generated/server/server.crt      Server certificate (RadSec / TLS)
  generated/server/server.key      Server private key
  generated/server/server.pem      Cert + key concatenated
  generated/server/server.p12      PKCS#12 (Java / some NAS devices)
  generated/client/client.crt      Client certificate (EAP-TLS / mTLS)
  generated/client/client.key      Client private key
  generated/client/client.pem      Cert + key concatenated
  generated/client/client.p12      PKCS#12 for supplicants
  generated/ca-chain.pem           Same as ca.crt (single CA)
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

command -v openssl >/dev/null 2>&1 || {
  echo "openssl is required" >&2
  exit 1
}

rm -rf "${OUT}"
mkdir -p "${OUT}/ca/newcerts" "${OUT}/server" "${OUT}/client"
touch "${OUT}/ca/index.txt"
echo 1000 > "${OUT}/ca/serial"

echo "==> Generating CA"
openssl req -x509 -new -nodes \
  -newkey "rsa:${KEY_SIZE}" \
  -keyout "${OUT}/ca/ca.key" \
  -out "${OUT}/ca/ca.crt" \
  -days "${DAYS}" \
  -config "${CNF}" \
  -extensions v3_ca \
  -subj "/C=US/ST=Test/L=Lab/O=RADIUS Test/CN=RADIUS Test CA"

chmod 600 "${OUT}/ca/ca.key"
cp "${OUT}/ca/ca.crt" "${OUT}/ca-chain.pem"

echo "==> Generating RADIUS server certificate (CN=${SERVER_CN})"
openssl req -new -nodes \
  -newkey "rsa:${KEY_SIZE}" \
  -keyout "${OUT}/server/server.key" \
  -out "${OUT}/server/server.csr" \
  -config "${CNF}" \
  -subj "/C=US/ST=Test/L=Lab/O=RADIUS Test/CN=${SERVER_CN}"

openssl x509 -req \
  -in "${OUT}/server/server.csr" \
  -CA "${OUT}/ca/ca.crt" \
  -CAkey "${OUT}/ca/ca.key" \
  -CAcreateserial \
  -out "${OUT}/server/server.crt" \
  -days "${DAYS}" \
  -sha256 \
  -extfile "${CNF}" \
  -extensions v3_server

chmod 600 "${OUT}/server/server.key"
cat "${OUT}/server/server.crt" "${OUT}/server/server.key" > "${OUT}/server/server.pem"
cat "${OUT}/server/server.crt" "${OUT}/ca/ca.crt" > "${OUT}/server/server-fullchain.pem"

openssl pkcs12 -export \
  -inkey "${OUT}/server/server.key" \
  -in "${OUT}/server/server.crt" \
  -certfile "${OUT}/ca/ca.crt" \
  -out "${OUT}/server/server.p12" \
  -name "radius-server" \
  -passout "pass:${P12_PASS}"

echo "==> Generating client certificate (CN=${CLIENT_CN})"
openssl req -new -nodes \
  -newkey "rsa:${KEY_SIZE}" \
  -keyout "${OUT}/client/client.key" \
  -out "${OUT}/client/client.csr" \
  -config "${CNF}" \
  -subj "/C=US/ST=Test/L=Lab/O=RADIUS Test/CN=${CLIENT_CN}"

openssl x509 -req \
  -in "${OUT}/client/client.csr" \
  -CA "${OUT}/ca/ca.crt" \
  -CAkey "${OUT}/ca/ca.key" \
  -CAcreateserial \
  -out "${OUT}/client/client.crt" \
  -days "${DAYS}" \
  -sha256 \
  -extfile "${CNF}" \
  -extensions v3_client

chmod 600 "${OUT}/client/client.key"
cat "${OUT}/client/client.crt" "${OUT}/client/client.key" > "${OUT}/client/client.pem"
cat "${OUT}/client/client.crt" "${OUT}/ca/ca.crt" > "${OUT}/client/client-fullchain.pem"

openssl pkcs12 -export \
  -inkey "${OUT}/client/client.key" \
  -in "${OUT}/client/client.crt" \
  -certfile "${OUT}/ca/ca.crt" \
  -out "${OUT}/client/client.p12" \
  -name "radius-client" \
  -passout "pass:${P12_PASS}"

echo "==> Verifying chain"
openssl verify -CAfile "${OUT}/ca/ca.crt" "${OUT}/server/server.crt"
openssl verify -CAfile "${OUT}/ca/ca.crt" "${OUT}/client/client.crt"

echo
echo "Test PKI written to ${OUT}"
echo "PKCS#12 password: ${P12_PASS}"
echo
echo "Point the RADIUS server TLS config at:"
echo "  CA:     ${OUT}/ca/ca.crt"
echo "  cert:   ${OUT}/server/server.crt"
echo "  key:    ${OUT}/server/server.key"
echo "  chain:  ${OUT}/server/server-fullchain.pem"
echo
echo "For EAP-TLS / RadSec client mTLS:"
echo "  CA:     ${OUT}/ca/ca.crt"
echo "  cert:   ${OUT}/client/client.crt"
echo "  key:    ${OUT}/client/client.key"
echo
echo "Inspect a cert with: openssl x509 -in generated/server/server.crt -noout -text"
