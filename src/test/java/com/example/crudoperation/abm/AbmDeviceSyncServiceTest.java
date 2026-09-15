package com.example.crudoperation.abm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbmDeviceSyncServiceTest {

    private static final String TENANT = "553151";
    private static final String PEM = "-----BEGIN CERTIFICATE-----\nOLD\n-----END CERTIFICATE-----";
    private static final String TOKEN = "old-token";
    private static final String STALE_CURSOR =
            "MTAwOjEwMToxNzg2Njk3MTgxODg5OjE3ODk0NTY0MzUxNjM6ZmFsc2U6MTc4OTQ1NjQzNTE2Mw";

    private InMemoryAbmSyncStateStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryAbmSyncStateStore();
    }

    @Test
    void parsesHaloInvalidCursorMessageEvenWhenWrappedAs500() {
        DepApiException parsed = DepApiException.parse(500,
                "unexpected dep response. status=400 DEP API Error=INVALID_CURSOR");
        assertEquals(400, parsed.getStatus());
        assertEquals(DepErrorCode.INVALID_CURSOR, parsed.getErrorCode());
        assertTrue(parsed.getErrorCode().requiresFullFetch());
    }

    @Test
    void newPemAndTokenForcesFullFetchAndDropsStaleCursor() {
        String oldFp = AbmCredentialFingerprint.of(PEM, TOKEN);
        store.save(new AbmSyncState(TENANT, STALE_CURSOR, oldFp));

        RecordingClient client = new RecordingClient();
        client.fetchPages.add(new DepDevicePage("new-cursor", false, Arrays.asList("SN1", "SN2")));

        AbmDeviceSyncService service = new AbmDeviceSyncService(client, store);
        List<String> serials = service.syncDevices(TENANT, "-----BEGIN CERTIFICATE-----\nNEW\n-----END CERTIFICATE-----",
                "new-token");

        assertEquals(Arrays.asList("SN1", "SN2"), serials);
        assertEquals(Collections.singletonList(null), client.fetchCursors);
        assertTrue(client.syncCursors.isEmpty());
        assertEquals("new-cursor", store.load(TENANT).getCursor());
        assertFalse(AbmCredentialFingerprint.of(PEM, TOKEN)
                .equals(store.load(TENANT).getCredentialFingerprint()));
    }

    @Test
    void invalidCursorOnSyncRetriesFetchWithEmptyCursor() {
        String fp = AbmCredentialFingerprint.of(PEM, TOKEN);
        store.save(new AbmSyncState(TENANT, STALE_CURSOR, fp));

        RecordingClient client = new RecordingClient();
        client.syncErrors.add(DepApiException.parse(400, "DEP API Error=INVALID_CURSOR"));
        client.fetchPages.add(new DepDevicePage("fresh", false, Collections.singletonList("SN9")));

        List<String> serials = new AbmDeviceSyncService(client, store).syncDevices(TENANT, PEM, TOKEN);

        assertEquals(Collections.singletonList("SN9"), serials);
        assertEquals(Collections.singletonList(STALE_CURSOR), client.syncCursors);
        assertEquals(Collections.singletonList(null), client.fetchCursors);
        assertEquals("fresh", store.load(TENANT).getCursor());
    }

    @Test
    void invalidCursorOnFetchWithStoredCursorRetriesEmpty() {
        String fp = AbmCredentialFingerprint.of(PEM, TOKEN);
        store.save(new AbmSyncState(TENANT, STALE_CURSOR, fp));

        // credentials unchanged but planner starts at SYNC because a cursor exists.
        // Force fetch path: empty fingerprint match with FETCH by clearing fp equality...
        // Instead invoke fetch by storing cursor but using recover from fetch:
        RecordingClient client = new RecordingClient();
        client.syncErrors.add(new DepApiException(500,
                "failed to fetch devices from ABM tenant=" + TENANT + " cursor=" + STALE_CURSOR
                        + " error=500 unexpected dep response. status=400 DEP API Error=INVALID_CURSOR"));
        client.fetchPages.add(new DepDevicePage("ok", false, Collections.singletonList("ABC")));

        List<String> serials = new AbmDeviceSyncService(client, store).syncDevices(TENANT, PEM, TOKEN);
        assertEquals(Collections.singletonList("ABC"), serials);
        assertEquals(Collections.singletonList(null), client.fetchCursors);
    }

    @Test
    void emptyCursorInvalidDoesNotLoop() {
        RecordingClient client = new RecordingClient();
        client.fetchErrors.add(DepApiException.parse(400, "INVALID_CURSOR"));

        AbmDeviceSyncService service = new AbmDeviceSyncService(client, store);
        DepApiException thrown = assertThrows(DepApiException.class,
                () -> service.syncDevices(TENANT, PEM, TOKEN));
        assertEquals(DepErrorCode.INVALID_CURSOR, thrown.getErrorCode());
        assertEquals(1, client.fetchAttempts.get());
    }

    @Test
    void incrementalSyncUsesStoredCursorWhenCredentialsUnchanged() {
        String fp = AbmCredentialFingerprint.of(PEM, TOKEN);
        store.save(new AbmSyncState(TENANT, "cursor-1", fp));

        RecordingClient client = new RecordingClient();
        client.syncPages.add(new DepDevicePage("cursor-2", false, Collections.singletonList("SN3")));

        List<String> serials = new AbmDeviceSyncService(client, store).syncDevices(TENANT, PEM, TOKEN);
        assertEquals(Collections.singletonList("SN3"), serials);
        assertEquals(Collections.singletonList("cursor-1"), client.syncCursors);
        assertTrue(client.fetchCursors.isEmpty());
        assertEquals("cursor-2", store.load(TENANT).getCursor());
    }

    @Test
    void plannerTreatsCredentialChangeAsFullFetch() {
        AbmSyncState state = new AbmSyncState(TENANT, STALE_CURSOR, "old-fp");
        assertEquals(DepSyncPlanner.Phase.FETCH, DepSyncPlanner.initialPhase(state, "new-fp"));
        assertNull(DepSyncPlanner.cursorAfterCredentialChange(state, "new-fp"));
    }

    @Test
    void replaceCredentialsClearsCursor() {
        store.save(new AbmSyncState(TENANT, STALE_CURSOR, "old"));
        AbmDeviceController controller = new AbmDeviceController(
                new AbmDeviceSyncService(new RecordingClient(), store), store);
        AbmDeviceController.CredentialsRequest req = new AbmDeviceController.CredentialsRequest();
        req.setPemPublicKey(PEM);
        req.setServerToken("brand-new");
        controller.replaceCredentials(TENANT, req);
        assertNull(store.load(TENANT).getCursor());
        assertEquals(AbmCredentialFingerprint.of(PEM, "brand-new"),
                store.load(TENANT).getCredentialFingerprint());
    }

    static class RecordingClient implements DepDeviceClient {
        final List<DepDevicePage> fetchPages = new ArrayList<DepDevicePage>();
        final List<DepDevicePage> syncPages = new ArrayList<DepDevicePage>();
        final List<DepApiException> fetchErrors = new ArrayList<DepApiException>();
        final List<DepApiException> syncErrors = new ArrayList<DepApiException>();
        final List<String> fetchCursors = new ArrayList<String>();
        final List<String> syncCursors = new ArrayList<String>();
        final AtomicInteger fetchAttempts = new AtomicInteger();
        final AtomicInteger syncAttempts = new AtomicInteger();

        @Override
        public DepDevicePage fetchDevices(String cursor) {
            fetchAttempts.incrementAndGet();
            fetchCursors.add(cursor);
            if (!fetchErrors.isEmpty()) {
                throw fetchErrors.remove(0);
            }
            if (fetchPages.isEmpty()) {
                throw new DepApiException(500, "no fetch page configured");
            }
            return fetchPages.remove(0);
        }

        @Override
        public DepDevicePage syncDevices(String cursor) {
            syncAttempts.incrementAndGet();
            syncCursors.add(cursor);
            if (!syncErrors.isEmpty()) {
                throw syncErrors.remove(0);
            }
            if (syncPages.isEmpty()) {
                throw new DepApiException(500, "no sync page configured");
            }
            return syncPages.remove(0);
        }
    }
}
