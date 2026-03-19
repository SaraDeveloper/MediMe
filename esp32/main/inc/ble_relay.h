/**
 * MediMe BLE Relay - GATT server for relay control over BLE.
 * Device name: "MediMe". One writeable characteristic: 0x01 = Relay 1 ON, 0x00 = Relay 1 OFF.
 */

#ifndef BLE_RELAY_H
#define BLE_RELAY_H

#ifdef __cplusplus
extern "C" {
#endif

/** UUIDs (128-bit) - use the same in the Android app. */
#define BLE_RELAY_SVC_UUID_128  "a1b2c3d4-e5f6-4a5b-8c7d-9e0f1a2b3c4d"
#define BLE_RELAY_CHR_UUID_128  "a1b2c3d5-e5f6-4a5b-8c7d-9e0f1a2b3c4d"

/**
 * @brief Initialize and start BLE (NimBLE GATT server, device name "MediMe", advertising).
 * Call after NVS and RelayInit(). Does not return; BLE runs in host task.
 */
void ble_relay_init(void);

#ifdef __cplusplus
}
#endif

#endif /* BLE_RELAY_H */
