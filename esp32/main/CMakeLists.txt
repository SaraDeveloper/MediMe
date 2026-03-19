idf_component_register(SRCS "src/main.c" "src/relay.c" "src/bt_spp_relay.c" "src/spp_task.c"
                       INCLUDE_DIRS "inc" "."
                       REQUIRES bt
                       PRIV_REQUIRES esp_driver_gpio nvs_flash vfs)
