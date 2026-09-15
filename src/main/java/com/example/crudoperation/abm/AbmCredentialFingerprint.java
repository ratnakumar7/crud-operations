package com.example.crudoperation.abm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Fingerprint of the PEM public key plus ABM server token so a cert/token
 * rotation can be detected and the stored DEP cursor discarded.
 */
public final class AbmCredentialFingerprint {

    private AbmCredentialFingerprint() {
    }

    public static String of(String pemPublicKey, String serverToken) {
        String material = nullToEmpty(pemPublicKey) + "\n" + nullToEmpty(serverToken);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
