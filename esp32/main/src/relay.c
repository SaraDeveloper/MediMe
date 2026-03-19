/**
 * Relay control - same GPIOs as web-relay project.
 * Relay 1: GPIO 16, Relay 2: GPIO 17.
 */

#include "relay.h"
#include "driver/gpio.h"
#include "esp_log.h"

#define RELAY_1_GPIO GPIO_NUM_16
#define RELAY_2_GPIO GPIO_NUM_17

static const char *TAG = "relay";

void RelayInit(void)
{
    gpio_reset_pin(RELAY_1_GPIO);
    gpio_reset_pin(RELAY_2_GPIO);
    gpio_set_direction(RELAY_1_GPIO, GPIO_MODE_OUTPUT);
    gpio_set_direction(RELAY_2_GPIO, GPIO_MODE_OUTPUT);
}

void RelayOn(int relayNumber)
{
    if (relayNumber != 1 && relayNumber != 2)
    {
        ESP_LOGE(TAG, "Invalid relay number: %d", relayNumber);
        return;
    }
    gpio_num_t gpio = (relayNumber == 1) ? RELAY_1_GPIO : RELAY_2_GPIO;
    gpio_set_level(gpio, 1);
}

void RelayOff(int relayNumber)
{
    if (relayNumber != 1 && relayNumber != 2)
    {
        ESP_LOGE(TAG, "Invalid relay number: %d", relayNumber);
        return;
    }
    gpio_num_t gpio = (relayNumber == 1) ? RELAY_1_GPIO : RELAY_2_GPIO;
    gpio_set_level(gpio, 0);
}
