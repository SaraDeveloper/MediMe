/**
 * MediMe Bluetooth Classic SPP Relay - virtual COM port mode.
 * Device name "MediMe". Receives 1-byte commands: 0x01 = Relay 1 ON, 0x00 = Relay 1 OFF.
 */

#ifndef BT_SPP_RELAY_H
#define BT_SPP_RELAY_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Initialize and start Bluetooth Classic SPP server (device name from Kconfig, relay control).
 * Call after NVS and RelayInit() from app_main.
 */
void bt_spp_relay_init(void);

#ifdef __cplusplus
}
#endif

#endif /* BT_SPP_RELAY_H */
