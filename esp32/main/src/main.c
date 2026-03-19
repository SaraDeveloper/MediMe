/**
 * MediMe ESP32 - Bluetooth Classic SPP Relay Control
 *
 * - SPP server with device name "MediMe". Pair from phone (PIN 1234), then connect from app.
 * - Relay 1 (GPIO 16) / Relay 2 (GPIO 17): send 0x01 = Relay 1 ON, 0x00 = Relay 1 OFF over SPP.
 * - Hall sensor: GPIO 34. Rotate(): relay 1 ON, continuously monitors sensor, stops immediately when sensor = 0.
 * - LED: GPIO 2 blinks continuously (500 ms period).
 * - TEST: rotate_test_task calls Rotate() every 10 s (remove for production).
 * - BLE code (ble_relay.*) kept in tree but not built.
 */

#include "relay.h"
#include "hall_rotate.h"
#include "bt_spp_relay.h"
#include "esp_log.h"
#include "nvs_flash.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "driver/gpio.h"

static const char *TAG = "main";

#define LED_GPIO GPIO_NUM_2
#define LED_BLINK_PERIOD_MS 500

static void led_blink_task(void *arg)
{
    (void)arg;
    bool led_state = false;
    for (;;) {
        gpio_set_level(LED_GPIO, led_state ? 1 : 0);
        led_state = !led_state;
        vTaskDelay(pdMS_TO_TICKS(LED_BLINK_PERIOD_MS));
    }
}

/** TEST: calls Rotate() every 10 s; remove or disable for production. */
static void rotate_test_task(void *arg)
{
    (void)arg;
    for (;;) {
        ESP_LOGI(TAG, "rotate test: calling Rotate()");
        Rotate();
        vTaskDelay(pdMS_TO_TICKS(10000));
    }
}

void app_main(void)
{
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    gpio_reset_pin(LED_GPIO);
    gpio_set_direction(LED_GPIO, GPIO_MODE_OUTPUT);
    xTaskCreate(led_blink_task, "led_blink", 2048, NULL, 1, NULL);

    RelayInit();
    HallRotateInit();
    ESP_LOGI(TAG, "MediMe SPP Relay starting");
    bt_spp_relay_init();

    xTaskCreate(rotate_test_task, "rotate_test", 3072, NULL, 3, NULL);
}
