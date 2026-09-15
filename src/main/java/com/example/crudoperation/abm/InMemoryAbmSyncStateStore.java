package com.example.crudoperation.abm;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAbmSyncStateStore implements AbmSyncStateStore {
    private final ConcurrentHashMap<String, AbmSyncState> states = new ConcurrentHashMap<String, AbmSyncState>();

    @Override
    public AbmSyncState load(String tenantId) {
        return states.get(tenantId);
    }

    @Override
    public void save(AbmSyncState state) {
        states.put(state.getTenantId(), state);
    }

    @Override
    public void clearCursor(String tenantId) {
        AbmSyncState existing = states.get(tenantId);
        if (existing == null) {
            return;
        }
        states.put(tenantId, new AbmSyncState(tenantId, null, existing.getCredentialFingerprint()));
    }
}
