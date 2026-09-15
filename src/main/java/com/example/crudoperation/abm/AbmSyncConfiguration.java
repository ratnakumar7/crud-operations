package com.example.crudoperation.abm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AbmSyncConfiguration {

    @Bean
    public AbmSyncStateStore abmSyncStateStore() {
        return new InMemoryAbmSyncStateStore();
    }

    @Bean
    public DepDeviceClient depDeviceClient() {
        return new UnconfiguredDepDeviceClient();
    }

    @Bean
    public AbmDeviceSyncService abmDeviceSyncService(DepDeviceClient client, AbmSyncStateStore store) {
        return new AbmDeviceSyncService(client, store);
    }

    /**
     * Placeholder until a real Apple session client is wired. Sync calls fail
     * with a clear error instead of using a stale cursor.
     */
    static class UnconfiguredDepDeviceClient implements DepDeviceClient {
        @Override
        public DepDevicePage fetchDevices(String cursor) {
            throw new IllegalStateException(
                    "DEP client is not configured; reset the stored cursor after token rotation "
                            + "via PUT /abm/{tenantId}/credentials, then wire an Apple DEP client");
        }

        @Override
        public DepDevicePage syncDevices(String cursor) {
            return fetchDevices(cursor);
        }
    }
}
