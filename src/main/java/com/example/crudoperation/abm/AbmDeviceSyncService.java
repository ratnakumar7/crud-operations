package com.example.crudoperation.abm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Syncs devices from Apple Business Manager and recovers from a stale DEP cursor.
 *
 * <p>Typical failure after recreating an MDM server:
 * {@code unexpected dep response. status=400 DEP API Error=INVALID_CURSOR}
 * followed by {@code failed to fetch devices from ABM ... error=500}.
 *
 * <p>This service clears the stored cursor when the PEM/token fingerprint
 * changes, and retries {@code POST /server/devices} with an empty cursor when
 * Apple returns {@code INVALID_CURSOR} or {@code EXPIRED_CURSOR}.
 */
public class AbmDeviceSyncService {
    private static final Logger log = LoggerFactory.getLogger(AbmDeviceSyncService.class);
    private static final int MAX_PAGES = 10_000;

    private final DepDeviceClient client;
    private final AbmSyncStateStore store;

    public AbmDeviceSyncService(DepDeviceClient client, AbmSyncStateStore store) {
        this.client = client;
        this.store = store;
    }

    public List<String> syncDevices(String tenantId, String pemPublicKey, String serverToken) {
        String fingerprint = AbmCredentialFingerprint.of(pemPublicKey, serverToken);
        AbmSyncState state = store.load(tenantId);
        String cursor = DepSyncPlanner.cursorAfterCredentialChange(state, fingerprint);
        DepSyncPlanner.Phase phase = DepSyncPlanner.initialPhase(state, fingerprint);

        if (DepSyncPlanner.credentialsChanged(state, fingerprint)) {
            log.warn("ABM credentials changed for tenant={}; discarding stored cursor and running a full fetch",
                    tenantId);
            store.save(new AbmSyncState(tenantId, null, fingerprint));
            cursor = null;
            phase = DepSyncPlanner.Phase.FETCH;
        }

        List<String> serials = new ArrayList<String>();
        boolean recoveredOnce = false;

        for (int page = 0; page < MAX_PAGES; page++) {
            String cursorUsed = cursor;
            try {
                DepDevicePage result = invoke(phase, cursorUsed);
                if (result.getSerialNumbers() != null) {
                    serials.addAll(result.getSerialNumbers());
                }
                cursor = result.getCursor();
                store.save(new AbmSyncState(tenantId, cursor, fingerprint));

                if (result.isMoreToFollow()) {
                    continue;
                }
                return serials;
            } catch (DepApiException e) {
                if (e.getErrorCode() == DepErrorCode.EXHAUSTED_CURSOR) {
                    log.info("DEP cursor exhausted for tenant={}; fetch already complete", tenantId);
                    return serials;
                }
                DepSyncPlanner.Phase recovery = DepSyncPlanner.recoverFrom(e, cursorUsed);
                if (recovery != DepSyncPlanner.Phase.FETCH || recoveredOnce) {
                    log.error("failed to fetch devices from ABM tenant={} cursor={} error={}",
                            tenantId, cursorUsed, e.getStatus(), e);
                    throw e;
                }
                log.warn("DEP {} for tenant={}; retrying full fetch with empty cursor",
                        e.getErrorCode(), tenantId);
                cursor = null;
                phase = DepSyncPlanner.Phase.FETCH;
                store.clearCursor(tenantId);
                recoveredOnce = true;
            }
        }
        throw new DepApiException(500, "ABM device sync exceeded page limit for tenant=" + tenantId);
    }

    private DepDevicePage invoke(DepSyncPlanner.Phase phase, String cursor) {
        if (phase == DepSyncPlanner.Phase.SYNC) {
            return client.syncDevices(cursor);
        }
        return client.fetchDevices(cursor);
    }
}
