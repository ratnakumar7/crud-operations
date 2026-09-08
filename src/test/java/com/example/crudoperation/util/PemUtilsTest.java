package com.example.crudoperation.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PemUtilsTest {

    private static final String DER_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvQqyV8wvQOaF7OzHrnfphH8odc5quwYzmJmsjbc1dFMacANMGhlU"
                    + "aij7An307iLUcdtkMN2Y+4W2bVjgrgf8Z9DLJFx8jfIz10fmypvNixUPoqcffCwI";

    @Test
    void wrapsRawBase64AsCertificatePem() {
        String pem = PemUtils.toPemCertificate(DER_BASE64);

        assertTrue(pem.startsWith("-----BEGIN CERTIFICATE-----\n"));
        assertTrue(pem.endsWith("-----END CERTIFICATE-----\n"));
        assertTrue(pem.contains("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvQqyV8wvQOaF7OzHrnfp"));
        assertLineLengthAtMost64(pem);
    }

    @Test
    void normalizesExistingPem() {
        String alreadyPem = "-----BEGIN CERTIFICATE-----\n"
                + DER_BASE64
                + "\n-----END CERTIFICATE-----";

        String pem = PemUtils.toPemCertificate(alreadyPem);

        assertTrue(pem.startsWith("-----BEGIN CERTIFICATE-----\n"));
        assertEquals(PemUtils.toPemCertificate(DER_BASE64), pem);
    }

    @Test
    void decodesBase64OfFullPemString() {
        String pem = PemUtils.toPemCertificate(DER_BASE64);
        String storedAsBase64 = Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));

        assertEquals(pem, PemUtils.toPemCertificate(storedAsBase64));
    }

    @Test
    void ignoresWhitespaceInStoredBase64() {
        String withWhitespace = DER_BASE64.replaceAll("(.{10})", "$1\n");

        assertEquals(PemUtils.toPemCertificate(DER_BASE64), PemUtils.toPemCertificate(withWhitespace));
    }

    @Test
    void supportsCustomPemLabel() {
        String pem = PemUtils.toPem(DER_BASE64, "PRIVATE KEY");

        assertTrue(pem.startsWith("-----BEGIN PRIVATE KEY-----\n"));
        assertTrue(pem.endsWith("-----END PRIVATE KEY-----\n"));
    }

    @Test
    void rejectsBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> PemUtils.toPemCertificate("  "));
        assertThrows(IllegalArgumentException.class, () -> PemUtils.toPemCertificate(null));
    }

    private static void assertLineLengthAtMost64(String pem) {
        String[] lines = pem.split("\n");
        for (String line : lines) {
            if (line.startsWith("-----")) {
                continue;
            }
            assertTrue(line.length() <= 64, "PEM body line longer than 64: " + line.length());
        }
    }
}
