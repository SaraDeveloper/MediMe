/**
 * MediMe ESP32 - LED Blink + Relay
 *
 * - LED on GPIO 23 blinks every 500 ms.
 * - Relay 1 on GPIO 16 follows the LED (on when LED on, off when LED off).
 * - Relay 2 on GPIO 17 is available via RelayOn(2) / RelayOff(2).
 *
 * Same relay GPIOs as web-relay: Relay 1 = GPIO 16, Relay 2 = GPIO 17.
 */

#include "relay.h"
#include "driver/gpio.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

/* ----- Configuration ----- */
#define LED_GPIO       GPIO_NUM_23   /* Pin where the LED is connected */
#define BLINK_DELAY_MS 500           /* Time between each blink (milliseconds) */

static const char *TAG = "blink";

/* ----- Setup: configure the LED pin as output ----- */
static void setup_led(void)
{
    gpio_reset_pin(LED_GPIO);
    gpio_set_direction(LED_GPIO, GPIO_MODE_OUTPUT);
}

/* ----- Main entry point (called once at startup) ----- */
void app_main(void)
{
    setup_led();
    RelayInit();

    bool led_is_on = false;

    while (1)
    {
        /* Toggle LED: on -> off, off -> on */
        led_is_on = !led_is_on;
        gpio_set_level(LED_GPIO, led_is_on ? 1 : 0);

        /* Relay 1 follows the LED */
        if (led_is_on)
            RelayOn(1);
        else
            RelayOff(1);

        ESP_LOGI(TAG, "LED %s, Relay1 %s", led_is_on ? "ON" : "OFF", led_is_on ? "ON" : "OFF");

        /* Wait before next blink */
        vTaskDelay(pdMS_TO_TICKS(BLINK_DELAY_MS));
    }
}
