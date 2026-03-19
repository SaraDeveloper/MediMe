/**
 * MediMe Bluetooth Classic SPP Relay - SPP server and relay command parser.
 * Same protocol as BLE: 0x01 = Relay 1 ON, 0x00 = Relay 1 OFF.
 */

#include <stdint.h>
#include <string.h>
#include <stdio.h>
#include <inttypes.h>
#include <sys/unistd.h>

#include "esp_log.h"
#include "esp_bt.h"
#include "esp_bt_main.h"
#include "esp_gap_bt_api.h"
#include "esp_bt_device.h"
#include "esp_spp_api.h"

#include "spp_task.h"
#include "bt_spp_relay.h"
#include "relay.h"
#include "hall_rotate.h"

static const char *TAG = "bt_spp_relay";

#define SPP_SERVER_NAME "MediMe"
#define SPP_DATA_LEN 128

static const esp_spp_sec_t sec_mask = ESP_SPP_SEC_AUTHENTICATE;
static const esp_spp_role_t role_slave = ESP_SPP_ROLE_SLAVE;

static char *bda2str(uint8_t *bda, char *str, size_t size)
{
    if (bda == NULL || str == NULL || size < 18) {
        return NULL;
    }
    uint8_t *p = bda;
    sprintf(str, "%02x:%02x:%02x:%02x:%02x:%02x",
            p[0], p[1], p[2], p[3], p[4], p[5]);
    return str;
}

/**
 * Read SPP data and drive relay: 0x01 -> RelayOn(1), 0x00 -> RelayOff(1).
 */
static void spp_relay_read_handle(void *param)
{
    int size;
    int fd = (int)(intptr_t)param;
    uint8_t *spp_data = malloc(SPP_DATA_LEN);
    if (!spp_data) {
        ESP_LOGE(TAG, "malloc spp_data failed, fd:%d", fd);
        spp_wr_task_shut_down();
        return;
    }

    do {
        size = read(fd, spp_data, SPP_DATA_LEN);
        if (size < 0) {
            break;
        }
        if (size == 0) {
            vTaskDelay(500 / portTICK_PERIOD_MS);
            continue;
        }
        for (int i = 0; i < size; i++) {
            uint8_t b = spp_data[i];
            if (b == 0x01) {
                RelayOn(1);
                ESP_LOGI(TAG, "Relay 1 ON");
            } else if (b == 0x00) {
                RelayOff(1);
                ESP_LOGI(TAG, "Relay 1 OFF");
            } else if (b == 0x02) {
                Rotate();
                ESP_LOGI(TAG, "Rotate command received");
            }
        }
        vTaskDelay(10 / portTICK_PERIOD_MS);
    } while (1);

    free(spp_data);
    spp_wr_task_shut_down();
}

static void esp_spp_cb(uint16_t e, void *p)
{
    esp_spp_cb_event_t event = e;
    esp_spp_cb_param_t *param = p;
    char bda_str[18] = {0};

    switch (event) {
    case ESP_SPP_INIT_EVT:
        if (param->init.status == ESP_SPP_SUCCESS) {
            ESP_LOGI(TAG, "ESP_SPP_INIT_EVT");
            esp_spp_vfs_register();
        } else {
            ESP_LOGE(TAG, "ESP_SPP_INIT_EVT status:%d", param->init.status);
        }
        break;
    case ESP_SPP_DISCOVERY_COMP_EVT:
        ESP_LOGI(TAG, "ESP_SPP_DISCOVERY_COMP_EVT");
        break;
    case ESP_SPP_OPEN_EVT:
        ESP_LOGI(TAG, "ESP_SPP_OPEN_EVT");
        break;
    case ESP_SPP_CLOSE_EVT:
        ESP_LOGI(TAG, "ESP_SPP_CLOSE_EVT status:%d handle:%" PRIu32 " close_by_remote:%d",
                 param->close.status, param->close.handle, param->close.async);
        break;
    case ESP_SPP_START_EVT:
        if (param->start.status == ESP_SPP_SUCCESS) {
            ESP_LOGI(TAG, "ESP_SPP_START_EVT handle:%" PRIu32 " scn:%d", param->start.handle, param->start.scn);
            esp_bt_gap_set_device_name(CONFIG_MEDIME_BT_DEVICE_NAME);
            esp_bt_gap_set_scan_mode(ESP_BT_CONNECTABLE, ESP_BT_GENERAL_DISCOVERABLE);
        } else {
            ESP_LOGE(TAG, "ESP_SPP_START_EVT status:%d", param->start.status);
        }
        break;
    case ESP_SPP_CL_INIT_EVT:
        ESP_LOGI(TAG, "ESP_SPP_CL_INIT_EVT");
        break;
    case ESP_SPP_SRV_OPEN_EVT:
        ESP_LOGI(TAG, "ESP_SPP_SRV_OPEN_EVT status:%d handle:%" PRIu32 ", rem_bda:[%s]",
                 param->srv_open.status, param->srv_open.handle,
                 bda2str(param->srv_open.rem_bda, bda_str, sizeof(bda_str)));
        if (param->srv_open.status == ESP_SPP_SUCCESS) {
            spp_wr_task_start_up(spp_relay_read_handle, param->srv_open.fd);
        }
        break;
    case ESP_SPP_VFS_REGISTER_EVT:
        if (param->vfs_register.status == ESP_SPP_SUCCESS) {
            ESP_LOGI(TAG, "SPP server \"%s\" ready; pair from phone then connect from app", SPP_SERVER_NAME);
            esp_spp_start_srv(sec_mask, role_slave, 0, SPP_SERVER_NAME);
        } else {
            ESP_LOGE(TAG, "ESP_SPP_VFS_REGISTER_EVT status:%d", param->vfs_register.status);
        }
        break;
    default:
        break;
    }
}

static void esp_spp_stack_cb(esp_spp_cb_event_t event, esp_spp_cb_param_t *param)
{
    spp_task_work_dispatch(esp_spp_cb, event, param, sizeof(esp_spp_cb_param_t), NULL);
}

static void esp_bt_gap_cb(esp_bt_gap_cb_event_t event, esp_bt_gap_cb_param_t *param)
{
    switch (event) {
    case ESP_BT_GAP_AUTH_CMPL_EVT:
        if (param->auth_cmpl.stat == ESP_BT_STATUS_SUCCESS) {
            ESP_LOGI(TAG, "pairing success: %s", param->auth_cmpl.device_name);
        } else {
            ESP_LOGE(TAG, "pairing failed, status:%d", param->auth_cmpl.stat);
        }
        break;
    case ESP_BT_GAP_PIN_REQ_EVT:
        ESP_LOGI(TAG, "PIN_REQ (use 1234 on phone)");
        {
            esp_bt_pin_code_t pin_code;
            pin_code[0] = '1';
            pin_code[1] = '2';
            pin_code[2] = '3';
            pin_code[3] = '4';
            esp_bt_gap_pin_reply(param->pin_req.bda, true, 4, pin_code);
        }
        break;
    case ESP_BT_GAP_MODE_CHG_EVT:
        ESP_LOGI(TAG, "MODE_CHG mode:%d", param->mode_chg.mode);
        break;
    default:
        break;
    }
}

void bt_spp_relay_init(void)
{
    ESP_LOGI(TAG, "Initializing Bluetooth Classic SPP (device name: %s)", CONFIG_MEDIME_BT_DEVICE_NAME);

    ESP_ERROR_CHECK(esp_bt_controller_mem_release(ESP_BT_MODE_BLE));

    esp_bt_controller_config_t bt_cfg = BT_CONTROLLER_INIT_CONFIG_DEFAULT();
    if (esp_bt_controller_init(&bt_cfg) != ESP_OK) {
        ESP_LOGE(TAG, "bt_controller_init failed");
        return;
    }
    if (esp_bt_controller_enable(ESP_BT_MODE_CLASSIC_BT) != ESP_OK) {
        ESP_LOGE(TAG, "bt_controller_enable failed");
        return;
    }

    esp_bluedroid_config_t bluedroid_cfg = BT_BLUEDROID_INIT_CONFIG_DEFAULT();
    bluedroid_cfg.ssp_en = false; /* legacy PIN pairing (1234) */
    esp_err_t ret = esp_bluedroid_init_with_cfg(&bluedroid_cfg);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "bluedroid_init failed: %s", esp_err_to_name(ret));
        return;
    }
    if (esp_bluedroid_enable() != ESP_OK) {
        ESP_LOGE(TAG, "bluedroid_enable failed");
        return;
    }

    if (esp_bt_gap_register_callback(esp_bt_gap_cb) != ESP_OK) {
        ESP_LOGE(TAG, "gap register failed");
        return;
    }
    if (esp_spp_register_callback(esp_spp_stack_cb) != ESP_OK) {
        ESP_LOGE(TAG, "spp register failed");
        return;
    }

    spp_task_task_start_up();

    esp_spp_cfg_t bt_spp_cfg = BT_SPP_DEFAULT_CONFIG();
    if (esp_spp_enhanced_init(&bt_spp_cfg) != ESP_OK) {
        ESP_LOGE(TAG, "spp init failed");
        return;
    }

    esp_bt_pin_type_t pin_type = ESP_BT_PIN_TYPE_FIXED;
    esp_bt_pin_code_t pin_code;
    pin_code[0] = '1';
    pin_code[1] = '2';
    pin_code[2] = '3';
    pin_code[3] = '4';
    esp_bt_gap_set_pin(pin_type, 4, pin_code);

    char bda_str[18] = {0};
    ESP_LOGI(TAG, "SPP server init OK; address [%s]", bda2str((uint8_t *)esp_bt_dev_get_address(), bda_str, sizeof(bda_str)));
}
