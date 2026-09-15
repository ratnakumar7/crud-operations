package com.example.crudoperation.abm;

/**
 * Apple DEP / ABM device-list error codes returned in HTTP 400 bodies.
 */
public enum DepErrorCode {
    INVALID_CURSOR,
    EXPIRED_CURSOR,
    EXHAUSTED_CURSOR,
    UNKNOWN;

    public static DepErrorCode fromMessage(String message) {
        if (message == null) {
            return UNKNOWN;
        }
        String upper = message.toUpperCase();
        if (upper.contains("INVALID_CURSOR")) {
            return INVALID_CURSOR;
        }
        if (upper.contains("EXPIRED_CURSOR")) {
            return EXPIRED_CURSOR;
        }
        if (upper.contains("EXHAUSTED_CURSOR")) {
            return EXHAUSTED_CURSOR;
        }
        return UNKNOWN;
    }

    /**
     * True when the stored pagination cursor must be discarded and a full
     * fetch (empty cursor) started. Happens after an MDM server is deleted
     * and recreated, a new .p7m token is uploaded, or the cursor is older
     * than Apple's 7-day window.
     */
    public boolean requiresFullFetch() {
        return this == INVALID_CURSOR || this == EXPIRED_CURSOR;
    }
}
