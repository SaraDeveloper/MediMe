idf_component_register(SRCS "src/main.c" "src/relay.c" "src/ble_relay.c" INCLUDE_DIRS "inc" "."
                       REQUIRES bt
                       PRIV_REQUIRES esp_driver_gpio nvs_flash)

# Ensure NimBLE headers are found (bt component; IDF_PATH is set by idf.py)
set(nimble_bt_dir "$ENV{IDF_PATH}/components/bt")
target_include_directories(${COMPONENT_LIB} PRIVATE
    ${nimble_bt_dir}/host/nimble/port/include
    ${nimble_bt_dir}/host/nimble/nimble/porting/nimble/include
    ${nimble_bt_dir}/host/nimble/nimble/porting/npl/freertos/include
    ${nimble_bt_dir}/host/nimble/nimble/nimble/host/include
    ${nimble_bt_dir}/host/nimble/nimble/nimble/include
    ${nimble_bt_dir}/host/nimble/nimble/nimble/transport/include
    ${nimble_bt_dir}/host/nimble/nimble/nimble/host/services/gap/include
    ${nimble_bt_dir}/host/nimble/nimble/nimble/host/services/gatt/include
    ${nimble_bt_dir}/host/nimble/nimble/nimble/host/util/include
)
