package com.example.crudoperation.abm;

public interface AbmSyncStateStore {
    AbmSyncState load(String tenantId);

    void save(AbmSyncState state);

    void clearCursor(String tenantId);
}
