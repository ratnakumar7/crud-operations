package com.example.crudoperation.abm;

/**
 * Failure talking to Apple's DEP device list APIs
 * ({@code POST /server/devices} or {@code POST /devices/sync}).
 */
public class DepApiException extends RuntimeException {
    private final int status;
    private final DepErrorCode errorCode;

    public DepApiException(int status, String message) {
        super(message);
        this.status = status;
        this.errorCode = DepErrorCode.fromMessage(message);
    }

    public DepApiException(int status, DepErrorCode errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode == null ? DepErrorCode.UNKNOWN : errorCode;
    }

    public int getStatus() {
        return status;
    }

    public DepErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Parses Halo / MicroMDM style errors such as
     * {@code unexpected dep response. status=400 DEP API Error=INVALID_CURSOR}
     * even when an outer layer remaps the HTTP status to 500.
     */
    public static DepApiException parse(int status, String body) {
        DepErrorCode code = DepErrorCode.fromMessage(body);
        int resolvedStatus = status;
        if (body != null) {
            int marker = body.toLowerCase().indexOf("status=");
            if (marker >= 0) {
                int start = marker + "status=".length();
                int end = start;
                while (end < body.length() && Character.isDigit(body.charAt(end))) {
                    end++;
                }
                if (end > start) {
                    try {
                        resolvedStatus = Integer.parseInt(body.substring(start, end));
                    } catch (NumberFormatException ignored) {
                        resolvedStatus = status;
                    }
                }
            }
        }
        return new DepApiException(resolvedStatus, code, body);
    }
}
