/**
 * Hall sensor on GPIO 34 + rotate control.
 * Rotate(): relay 1 ON, then after an ignore window uses GPIO interrupt (falling edge)
 * to stop as soon as the sensor goes low — no slow polling, so short pulses are not missed.
 * Note: GPIO 34-39 are input-only on ESP32; internal pull-up may not be available — use external pull-up if needed.
 */

#include "hall_rotate.h"
#include "relay.h"
#include "driver/gpio.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h"
#include "esp_log.h"
#include "esp_err.h"

#define HALL_GPIO GPIO_NUM_34

/** Ignore hall transitions for this long after motor start (mechanical settle / gearbox slack). */
#define HALL_ROTATE_IGNORE_MS 150

/** Stop motor if no hall edge is seen (sensor fault / stalled). */
#define HALL_ROTATE_MAX_RUN_MS (2000)

static const char *TAG = "hall_rotate";

static SemaphoreHandle_t s_hall_stop_sem = NULL;
static volatile bool s_hall_armed = false;
static portMUX_TYPE s_hall_isr_mux = portMUX_INITIALIZER_UNLOCKED;

static void IRAM_ATTR hall_gpio_isr(void *arg)
{
    (void)arg;
    BaseType_t hp_task = pdFALSE;

    portENTER_CRITICAL_ISR(&s_hall_isr_mux);
    const bool armed = s_hall_armed;
    SemaphoreHandle_t sem = s_hall_stop_sem;
    if (armed && sem != NULL) {
        s_hall_armed = false;
    }
    portEXIT_CRITICAL_ISR(&s_hall_isr_mux);

    if (armed && sem != NULL) {
        xSemaphoreGiveFromISR(sem, &hp_task);
    }
    if (hp_task) {
        portYIELD_FROM_ISR();
    }
}

void HallRotateInit(void)
{
    gpio_reset_pin(HALL_GPIO);
    gpio_set_direction(HALL_GPIO, GPIO_MODE_INPUT);
    gpio_set_pull_mode(HALL_GPIO, GPIO_PULLUP_ONLY);

    esp_err_t err = gpio_install_isr_service(0);
    if (err != ESP_OK && err != ESP_ERR_INVALID_STATE) {
        ESP_LOGE(TAG, "gpio_install_isr_service: %s", esp_err_to_name(err));
    }

    gpio_set_intr_type(HALL_GPIO, GPIO_INTR_NEGEDGE);
    err = gpio_isr_handler_add(HALL_GPIO, hall_gpio_isr, NULL);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "gpio_isr_handler_add: %s", esp_err_to_name(err));
    }
    gpio_intr_disable(HALL_GPIO);
}

static void rotate_task(void *arg)
{
    (void)arg;

    SemaphoreHandle_t stop_sem = xSemaphoreCreateBinary();
    if (stop_sem == NULL) {
        ESP_LOGE(TAG, "stop sem create failed");
        vTaskDelete(NULL);
        return;
    }

    RelayOn(1);
    ESP_LOGI(TAG, "Motor ON; hall IRQ armed after %d ms", (int)HALL_ROTATE_IGNORE_MS);

    s_hall_stop_sem = stop_sem;
    s_hall_armed = false;

    vTaskDelay(pdMS_TO_TICKS(100));

    s_hall_armed = true;

    if (gpio_get_level(HALL_GPIO) == 0) {
        ESP_LOGI(TAG, "Hall already low after ignore window -> stop");
        goto cleanup;
    }

    gpio_intr_enable(HALL_GPIO);

    const TickType_t timeout_ticks = pdMS_TO_TICKS(HALL_ROTATE_MAX_RUN_MS);
    if (xSemaphoreTake(stop_sem, timeout_ticks) != pdTRUE) {
        ESP_LOGW(TAG, "Rotate timeout (%d ms) -> forcing motor off", (int)HALL_ROTATE_MAX_RUN_MS);
    } else {
        ESP_LOGI(TAG, "Hall falling edge -> motor stopping");
    }

cleanup:
    s_hall_armed = false;
    gpio_intr_disable(HALL_GPIO);
    s_hall_stop_sem = NULL;
    RelayOff(1);
    vSemaphoreDelete(stop_sem);
    vTaskDelete(NULL);
}

void Rotate(void)
{
    BaseType_t ok = xTaskCreate(rotate_task, "rotate", 3072, NULL, 5, NULL);
    if (ok != pdPASS) {
        ESP_LOGE(TAG, "xTaskCreate(rotate) failed");
    }
}
