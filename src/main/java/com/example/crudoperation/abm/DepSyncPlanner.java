package com.example.crudoperation.abm;

/**
 * Decides whether the next Apple call is a full fetch or an incremental sync.
 *
 * <p>Apple's cursor is bound to a specific MDM server token. Deleting the MDM
 * server in ABM, uploading a new .pem, and installing a newly downloaded token
 * makes any previously stored cursor return {@code INVALID_CURSOR}. The only
 * recovery is to fetch with an empty cursor.
 */
public final class DepSyncPlanner {

    public enum Phase {
        FETCH,
        SYNC
    }

    private DepSyncPlanner() {
    }

    public static Phase initialPhase(AbmSyncState state, String currentCredentialFingerprint) {
        if (credentialsChanged(state, currentCredentialFingerprint)) {
            return Phase.FETCH;
        }
        if (state != null && state.hasCursor()) {
            return Phase.SYNC;
        }
        return Phase.FETCH;
    }

    public static boolean credentialsChanged(AbmSyncState state, String currentCredentialFingerprint) {
        if (currentCredentialFingerprint == null || currentCredentialFingerprint.isEmpty()) {
            return false;
        }
        if (state == null || state.getCredentialFingerprint() == null
                || state.getCredentialFingerprint().isEmpty()) {
            return false;
        }
        return !currentCredentialFingerprint.equals(state.getCredentialFingerprint());
    }

    /**
     * @return next phase after an Apple error, or {@code null} if the error is not recoverable
     */
    public static Phase recoverFrom(DepApiException error, String cursorUsed) {
        if (error == null) {
            return null;
        }
        DepErrorCode code = error.getErrorCode();
        if (code.requiresFullFetch()) {
            boolean alreadyEmpty = cursorUsed == null || cursorUsed.trim().isEmpty();
            if (alreadyEmpty) {
                return null;
            }
            return Phase.FETCH;
        }
        if (code == DepErrorCode.EXHAUSTED_CURSOR) {
            return Phase.SYNC;
        }
        return null;
    }

    public static String cursorAfterCredentialChange(AbmSyncState state, String currentCredentialFingerprint) {
        if (credentialsChanged(state, currentCredentialFingerprint)) {
            return null;
        }
        return state == null ? null : state.getCursor();
    }
}
