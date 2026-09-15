package com.example.crudoperation.abm;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/abm")
public class AbmDeviceController {
    private final AbmDeviceSyncService syncService;
    private final AbmSyncStateStore store;

    public AbmDeviceController(AbmDeviceSyncService syncService, AbmSyncStateStore store) {
        this.syncService = syncService;
        this.store = store;
    }

    public static class CredentialsRequest {
        private String pemPublicKey;
        private String serverToken;

        public String getPemPublicKey() {
            return pemPublicKey;
        }

        public void setPemPublicKey(String pemPublicKey) {
            this.pemPublicKey = pemPublicKey;
        }

        public String getServerToken() {
            return serverToken;
        }

        public void setServerToken(String serverToken) {
            this.serverToken = serverToken;
        }
    }

    /**
     * Call this after uploading a new .pem in ABM and installing the newly
     * downloaded server token. It drops the stale DEP cursor so the next sync
     * does a full fetch instead of {@code INVALID_CURSOR}.
     */
    @PutMapping("/{tenantId}/credentials")
    public ResponseEntity<Map<String, String>> replaceCredentials(
            @PathVariable String tenantId,
            @RequestBody CredentialsRequest request) {
        String fingerprint = AbmCredentialFingerprint.of(request.getPemPublicKey(), request.getServerToken());
        store.save(new AbmSyncState(tenantId, null, fingerprint));
        Map<String, String> body = new HashMap<String, String>();
        body.put("tenantId", tenantId);
        body.put("cursor", "");
        body.put("status", "cursor_reset");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{tenantId}/sync")
    public ResponseEntity<Map<String, Object>> sync(
            @PathVariable String tenantId,
            @RequestBody CredentialsRequest request) {
        List<String> serials = syncService.syncDevices(
                tenantId, request.getPemPublicKey(), request.getServerToken());
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("tenantId", tenantId);
        body.put("deviceCount", serials.size());
        body.put("serialNumbers", serials);
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(DepApiException.class)
    public ResponseEntity<Map<String, Object>> handleDep(DepApiException e) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("error", e.getErrorCode().name());
        body.put("status", e.getStatus());
        body.put("message", e.getMessage());
        HttpStatus http = e.getStatus() >= 400 && e.getStatus() < 600
                ? HttpStatus.valueOf(e.getStatus())
                : HttpStatus.BAD_GATEWAY;
        return new ResponseEntity<Map<String, Object>>(body, http);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleMissingClient(IllegalStateException e) {
        return new ResponseEntity<Map<String, String>>(
                Collections.singletonMap("message", e.getMessage()),
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
