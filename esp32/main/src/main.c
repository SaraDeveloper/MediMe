/**
 * MediMe ESP32 - Bluetooth Classic SPP Relay Control
 *
 * - SPP server with device name "MediMe". Pair from phone (PIN 1234), then connect from app.
 * - Relay 1 (GPIO 16) / Relay 2 (GPIO 17): send 0x01 = Relay 1 ON, 0x00 = Relay 1 OFF over SPP.
 * - BLE code (ble_relay.*) kept in tree but not built.
 */

#include "relay.h"
#include "bt_spp_relay.h"
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
    ESP_LOGI(TAG, "MediMe SPP Relay starting");
    bt_spp_relay_init();
}
