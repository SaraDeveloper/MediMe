/**
 * Hall sensor on GPIO 34 + continuous monitoring for rotate.
 * Rotate(): turns on relay 1 (motor), continuously polls sensor, stops immediately when sensor = 0.
 * Note: GPIO 34-39 are input-only on ESP32 and may not support internal pull-up.
 * If pull-up doesn't work, use an external pull-up resistor (e.g., 10kΩ to 3.3V).
 */

#include "hall_rotate.h"
#include "relay.h"
#include "driver/gpio.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"

#define HALL_GPIO GPIO_NUM_34
#define SENSOR_POLL_INTERVAL_MS 10

static const char *TAG = "hall_rotate";

void HallRotateInit(void)
{
    gpio_reset_pin(HALL_GPIO);
    gpio_set_direction(HALL_GPIO, GPIO_MODE_INPUT);
    gpio_set_pull_mode(HALL_GPIO, GPIO_PULLUP_ONLY);
}

static void rotate_task(void *arg)
{
    (void)arg;

    RelayOn(1);
    ESP_LOGI(TAG, "Motor ON, monitoring hall sensor");

    for (;;) {
        int level = gpio_get_level(HALL_GPIO);
        if (level == 0) {
            RelayOff(1);
            ESP_LOGI(TAG, "Hall sensor = 0 -> motor stopped");
            break;
        }
        vTaskDelay(pdMS_TO_TICKS(SENSOR_POLL_INTERVAL_MS));
    }

    vTaskDelete(NULL);
}

void Rotate(void)
{
    BaseType_t ok = xTaskCreate(rotate_task, "rotate", 2048, NULL, 5, NULL);
    if (ok != pdPASS) {
        ESP_LOGE(TAG, "xTaskCreate(rotate) failed");
    }
}
