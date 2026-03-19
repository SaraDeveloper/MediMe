/**
 * MediMe ESP32 - BLE Relay Control
 *
 * - BLE GATT server with device name "MediMe".
 * - Relay 1 (GPIO 16) / Relay 2 (GPIO 17) controlled via BLE writes:
 *   write 0x01 to relay characteristic -> Relay 1 ON, 0x00 -> Relay 1 OFF.
 */

#include "relay.h"
#include "ble_relay.h"
#include "esp_log.h"
#include "nvs_flash.h"

static const char *TAG = "main";

void app_main(void)
{
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    RelayInit();
    ESP_LOGI(TAG, "MediMe BLE Relay starting");
    ble_relay_init();
}
