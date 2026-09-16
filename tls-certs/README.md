# RADIUS TLS test certificates

Generate a **lab-only** certificate authority, server certificate, and client certificate for testing TLS on a custom RADIUS server (RadSec on TCP/TLS, or EAP-TLS).

Do not use these files in production. They use a local CA, unencrypted keys, and a default PKCS#12 password.

## Generate

Requires `openssl`.

```bash
cd tls-certs
chmod +x generate-certs.sh
./generate-certs.sh
```

Optional overrides:

```bash
SERVER_CN=radius.example.test \
CLIENT_CN=alice@example.test \
CERT_DAYS=90 \
P12_PASS=changeit \
./generate-certs.sh
```

Output (under `tls-certs/generated/`, gitignored):

| File | Use |
| --- | --- |
| `ca/ca.crt` | Trust store on the RADIUS server **and** every client / NAS |
| `ca/ca.key` | Signs certs; keep offline even in a lab |
| `server/server.crt` + `server/server.key` | RADIUS server TLS identity |
| `server/server-fullchain.pem` | Server cert + CA (some stacks want this) |
| `server/server.p12` | Java / PKCS#12 server identity (password `changeit`) |
| `client/client.crt` + `client/client.key` | EAP-TLS supplicant or RadSec mTLS client |
| `client/client.p12` | Windows / macOS / Java client identity |

The server cert SAN includes `localhost`, `radius.local`, `127.0.0.1`, and `::1`. Edit `server_san` in `openssl.cnf` if your hostname or IP is different, then regenerate.

## What the RADIUS server needs

For **RadSec** (RADIUS over TLS, typically TCP 2083) or a TLS listener on your custom server:

1. Load `server/server.crt` (or `server-fullchain.pem`) and `server/server.key`.
2. Trust `ca/ca.crt` so you can verify NAS / client certificates if you enable mutual TLS.
3. Present a name that matches the cert: connect as `localhost` or add `radius.local` to `/etc/hosts`.

Typical checks:

```bash
openssl x509 -in generated/server/server.crt -noout -subject -ext subjectAltName,extendedKeyUsage
openssl s_client -connect 127.0.0.1:2083 -CAfile generated/ca/ca.crt -servername radius.local
```

For **EAP-TLS**:

1. Server still uses the server cert above for the inner TLS tunnel.
2. Install `client.p12` (or `client.crt` + `client.key`) on the supplicant.
3. Install `ca.crt` as the trusted CA so the client accepts the RADIUS server.
4. The client CN defaults to `user@radius.local`; map that identity in your users file if you match on certificate CN.

## Java (this Spring Boot repo, or a Java RADIUS stack)

Import the PKCS#12 as a keystore and the CA as a truststore:

```bash
keytool -importcert -noprompt -alias radius-ca \
  -file generated/ca/ca.crt \
  -keystore generated/truststore.jks \
  -storepass changeit
```

The server PKCS#12 is already a keystore: `generated/server/server.p12` with password `changeit`.

## Customize SANs

If the NAS connects by IP `192.0.2.10` or hostname `radius.corp.test`, add them under `[ server_san ]` in `openssl.cnf`:

```
DNS.3 = radius.corp.test
IP.3  = 192.0.2.10
```

Then run `./generate-certs.sh` again. Hostname mismatch is the usual reason a TLS handshake fails even when the files are valid.
