package com.example.crudoperation.abm;

/**
 * Persisted ABM sync position for one tenant / MDM server token.
 */
public class AbmSyncState {
    private final String tenantId;
    private final String cursor;
    private final String credentialFingerprint;

    public AbmSyncState(String tenantId, String cursor, String credentialFingerprint) {
        this.tenantId = tenantId;
        this.cursor = cursor;
        this.credentialFingerprint = credentialFingerprint;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCursor() {
        return cursor;
    }

    public String getCredentialFingerprint() {
        return credentialFingerprint;
    }

    public boolean hasCursor() {
        return cursor != null && !cursor.trim().isEmpty();
    }
}
