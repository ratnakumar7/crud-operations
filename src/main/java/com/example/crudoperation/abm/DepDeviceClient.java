package com.example.crudoperation.abm;

/**
 * Apple DEP device list client.
 *
 * <p>Initial listing must use {@link #fetchDevices} with a null/empty cursor.
 * Incremental updates use {@link #syncDevices} with the last returned cursor.
 */
public interface DepDeviceClient {

    /**
     * {@code POST /server/devices} — full list (or continuation of a fetch).
     */
    DepDevicePage fetchDevices(String cursor);

    /**
     * {@code POST /devices/sync} — changes since {@code cursor}.
     */
    DepDevicePage syncDevices(String cursor);
}
