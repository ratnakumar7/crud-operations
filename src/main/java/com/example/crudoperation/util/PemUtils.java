package com.example.crudoperation.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts stored certificate material (raw Base64 / DER Base64 / PEM text)
 * into a standard PEM block.
 */
public final class PemUtils {

    private static final String CERTIFICATE_LABEL = "CERTIFICATE";
    private static final int PEM_LINE_LENGTH = 64;
    private static final Pattern PEM_BLOCK = Pattern.compile(
            "-----BEGIN ([A-Z0-9 ]+)-----([A-Za-z0-9+/=\r\n]+)-----END \\1-----",
            Pattern.DOTALL);

    private PemUtils() {
    }

    /**
     * Converts a stored certificate value into PEM:
     * <pre>
     * -----BEGIN CERTIFICATE-----
     * (base64 wrapped at 64 columns)
     * -----END CERTIFICATE-----
     * </pre>
     *
     * Accepts:
     * <ul>
     *   <li>raw Base64 of DER bytes (typical DB storage)</li>
     *   <li>PEM body only (Base64, with or without whitespace)</li>
     *   <li>a full PEM string</li>
     *   <li>Base64 encoding of a full PEM string</li>
     * </ul>
     */
    public static String toPemCertificate(String storedValue) {
        return toPem(storedValue, CERTIFICATE_LABEL);
    }

    /**
     * Same as {@link #toPemCertificate(String)} but for any PEM type
     * (for example {@code CERTIFICATE}, {@code PRIVATE KEY}, {@code RSA PRIVATE KEY}).
     */
    public static String toPem(String storedValue, String label) {
        if (storedValue == null || storedValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Certificate value is required");
        }
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("PEM label is required");
        }

        String trimmed = storedValue.trim();
        String normalizedLabel = label.trim().toUpperCase();

        Matcher pemMatcher = PEM_BLOCK.matcher(trimmed);
        if (pemMatcher.find()) {
            return wrapPemBody(compactBase64(pemMatcher.group(2)), pemMatcher.group(1).trim());
        }

        byte[] decoded = tryDecodeBase64(compactBase64(trimmed));
        if (decoded != null && decoded.length > 0) {
            String asText = new String(decoded, StandardCharsets.UTF_8).trim();
            Matcher nestedPem = PEM_BLOCK.matcher(asText);
            if (nestedPem.find()) {
                return wrapPemBody(compactBase64(nestedPem.group(2)), nestedPem.group(1).trim());
            }
            if (isDer(decoded)) {
                return wrapPemBody(Base64.getEncoder().encodeToString(decoded), normalizedLabel);
            }
        }

        String body = compactBase64(trimmed);
        if (!isBase64Alphabet(body)) {
            throw new IllegalArgumentException("Value is not valid Base64 or PEM certificate data");
        }
        return wrapPemBody(body, normalizedLabel);
    }

    private static String wrapPemBody(String base64Body, String label) {
        String compact = compactBase64(base64Body);
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(label).append("-----\n");
        for (int i = 0; i < compact.length(); i += PEM_LINE_LENGTH) {
            int end = Math.min(i + PEM_LINE_LENGTH, compact.length());
            pem.append(compact.substring(i, end)).append('\n');
        }
        pem.append("-----END ").append(label).append("-----\n");
        return pem.toString();
    }

    private static String compactBase64(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static byte[] tryDecodeBase64(String compact) {
        if (compact.isEmpty() || !isBase64Alphabet(compact)) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(compact);
        } catch (IllegalArgumentException ignored) {
            try {
                return Base64.getUrlDecoder().decode(compact);
            } catch (IllegalArgumentException ignoredAgain) {
                return null;
            }
        }
    }

    /**
     * DER certificates and keys are ASN.1 SEQUENCEs, which start with 0x30.
     */
    private static boolean isDer(byte[] bytes) {
        return bytes.length > 0 && (bytes[0] & 0xFF) == 0x30;
    }

    private static boolean isBase64Alphabet(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '+' || c == '/' || c == '='
                    || c == '-' || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
