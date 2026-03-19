/**
 * MediMe BLE Relay - NimBLE GATT server for relay control.
 * Device name "MediMe". One characteristic: write 0x01 = Relay 1 ON, 0x00 = Relay 1 OFF.
 */

#include <assert.h>
#include <string.h>
#include "ble_relay.h"
#include "relay.h"
#include "esp_log.h"
#include "nimble/nimble_port.h"
#include "nimble/nimble_port_freertos.h"
#include "host/ble_hs.h"
#include "host/ble_uuid.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"

static const char *TAG = "ble_relay";

/* 128-bit UUIDs: Service a1b2c3d4-e5f6-4a5b-8c7d-9e0f1a2b3c4d, Characteristic a1b2c3d5-... */
static const ble_uuid128_t ble_relay_svc_uuid =
    BLE_UUID128_INIT(0xa1, 0xb2, 0xc3, 0xd4, 0xe5, 0xf6, 0x4a, 0x5b,
                     0x8c, 0x7d, 0x9e, 0x0f, 0x1a, 0x2b, 0x3c, 0x4d);
static const ble_uuid128_t ble_relay_chr_uuid =
    BLE_UUID128_INIT(0xa1, 0xb2, 0xc3, 0xd5, 0xe5, 0xf6, 0x4a, 0x5b,
                     0x8c, 0x7d, 0x9e, 0x0f, 0x1a, 0x2b, 0x3c, 0x4d);

static uint8_t ble_relay_chr_val;
static uint16_t ble_relay_chr_val_handle;

static int ble_relay_gap_event(struct ble_gap_event *event, void *arg);
static int ble_relay_gatt_access(uint16_t conn_handle, uint16_t attr_handle,
                                struct ble_gatt_access_ctxt *ctxt, void *arg);
static void ble_relay_on_sync(void);
static void ble_relay_on_reset(int reason);
static void ble_relay_host_task(void *param);

static const struct ble_gatt_svc_def ble_relay_gatt_svcs[] = {
    {
        .type = BLE_GATT_SVC_TYPE_PRIMARY,
        .uuid = &ble_relay_svc_uuid.u,
        .characteristics = (struct ble_gatt_chr_def[]){
            {
                .uuid = &ble_relay_chr_uuid.u,
                .access_cb = ble_relay_gatt_access,
                .flags = BLE_GATT_CHR_F_READ | BLE_GATT_CHR_F_WRITE,
                .val_handle = &ble_relay_chr_val_handle,
            },
            { 0 },
        },
    },
    { 0 },
};

static void ble_relay_advertise(void)
{
    struct ble_gap_adv_params adv_params;
    struct ble_hs_adv_fields fields;
    int rc;

    memset(&fields, 0, sizeof(fields));
    fields.flags = BLE_HS_ADV_F_DISC_GEN | BLE_HS_ADV_F_BREDR_UNSUP;
    fields.tx_pwr_lvl_is_present = 1;
    fields.tx_pwr_lvl = BLE_HS_ADV_TX_PWR_LVL_AUTO;

    const char *name = ble_svc_gap_device_name();
    fields.name = (uint8_t *)name;
    fields.name_len = strlen(name);
    fields.name_is_complete = 1;

    /* Do not add 128-bit UUID here: with flags + tx_power + name it would exceed 31 bytes (BLE_HS_EMSGSIZE). App finds us by name "MediMe". */

    rc = ble_gap_adv_set_fields(&fields);
    if (rc != 0) {
        ESP_LOGE(TAG, "adv_set_fields rc=%d (4=EMSGSIZE: adv data >31 bytes)", rc);
        return;
    }

    memset(&adv_params, 0, sizeof(adv_params));
    adv_params.conn_mode = BLE_GAP_CONN_MODE_UND;
    adv_params.disc_mode = BLE_GAP_DISC_MODE_GEN;
    rc = ble_gap_adv_start(BLE_OWN_ADDR_PUBLIC, NULL, BLE_HS_FOREVER,
                           &adv_params, ble_relay_gap_event, NULL);
    if (rc != 0) {
        ESP_LOGE(TAG, "adv_start rc=%d", rc);
    } else {
        ESP_LOGI(TAG, "Advertising as \"%s\" (connect with app or nRF Connect)", name);
    }
}

static int ble_relay_gap_event(struct ble_gap_event *event, void *arg)
{
    switch (event->type) {
    case BLE_GAP_EVENT_CONNECT:
        if (event->connect.status == 0) {
            ESP_LOGI(TAG, "connected");
        } else {
            ESP_LOGI(TAG, "connect failed; status=%d", event->connect.status);
            ble_relay_advertise();
        }
        return 0;

    case BLE_GAP_EVENT_DISCONNECT:
        ESP_LOGI(TAG, "disconnect; reason=%d", event->disconnect.reason);
        ble_relay_advertise();
        return 0;

    case BLE_GAP_EVENT_ADV_COMPLETE:
        ble_relay_advertise();
        return 0;

    default:
        return 0;
    }
}

static int ble_relay_gatt_access(uint16_t conn_handle, uint16_t attr_handle,
                                 struct ble_gatt_access_ctxt *ctxt, void *arg)
{
    int rc;

    switch (ctxt->op) {
    case BLE_GATT_ACCESS_OP_READ_CHR:
        if (attr_handle == ble_relay_chr_val_handle) {
            rc = os_mbuf_append(ctxt->om, &ble_relay_chr_val, sizeof(ble_relay_chr_val));
            return (rc == 0) ? 0 : BLE_ATT_ERR_INSUFFICIENT_RES;
        }
        return BLE_ATT_ERR_UNLIKELY;

    case BLE_GATT_ACCESS_OP_WRITE_CHR: {
        uint16_t om_len;
        if (attr_handle != ble_relay_chr_val_handle) {
            return BLE_ATT_ERR_UNLIKELY;
        }
        om_len = OS_MBUF_PKTLEN(ctxt->om);
        if (om_len < 1) {
            return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
        }
        rc = ble_hs_mbuf_to_flat(ctxt->om, &ble_relay_chr_val, 1, NULL);
        if (rc != 0) {
            return BLE_ATT_ERR_UNLIKELY;
        }
        if (ble_relay_chr_val == 0x01) {
            RelayOn(1);
            ESP_LOGI(TAG, "Relay 1 ON");
        } else if (ble_relay_chr_val == 0x00) {
            RelayOff(1);
            ESP_LOGI(TAG, "Relay 1 OFF");
        }
        return 0;
    }

    default:
        return BLE_ATT_ERR_UNLIKELY;
    }
}

static void gatt_svr_register_cb(struct ble_gatt_register_ctxt *ctxt, void *arg)
{
    (void)arg;
    switch (ctxt->op) {
    case BLE_GATT_REGISTER_OP_SVC:
        ESP_LOGD(TAG, "registered service");
        break;
    case BLE_GATT_REGISTER_OP_CHR:
        ESP_LOGD(TAG, "registered characteristic; val_handle=%u", ctxt->chr.val_handle);
        break;
    default:
        break;
    }
}

static int gatt_svr_init(void)
{
    int rc = ble_gatts_count_cfg(ble_relay_gatt_svcs);
    if (rc != 0) {
        return rc;
    }
    rc = ble_gatts_add_svcs(ble_relay_gatt_svcs);
    if (rc != 0) {
        return rc;
    }
    return 0;
}

static void ble_relay_on_sync(void)
{
    ESP_LOGI(TAG, "BLE stack synced, starting advertising");
    ble_relay_advertise();
}

static void ble_relay_on_reset(int reason)
{
    ESP_LOGE(TAG, "NimBLE reset; reason=%d", reason);
}

static void ble_relay_host_task(void *param)
{
    (void)param;
    nimble_port_run();
    nimble_port_freertos_deinit();
}

void ble_relay_init(void)
{
    ESP_LOGI(TAG, "Initializing NimBLE...");
    esp_err_t ret = nimble_port_init();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "nimble_port_init failed %d", ret);
        return;
    }
    ESP_LOGI(TAG, "NimBLE OK, device name will be \"MediMe\"");

    ble_hs_cfg.reset_cb = ble_relay_on_reset;
    ble_hs_cfg.sync_cb = ble_relay_on_sync;
    ble_hs_cfg.gatts_register_cb = gatt_svr_register_cb;

    ble_svc_gap_init();
    ble_svc_gatt_init();

    int rc = gatt_svr_init();
    assert(rc == 0);

    rc = ble_svc_gap_device_name_set("MediMe");
    assert(rc == 0);

    nimble_port_freertos_init(ble_relay_host_task);
}
