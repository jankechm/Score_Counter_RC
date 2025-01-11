package com.mj.scorecounterrc

class Constants {
    companion object {
        const val BLE_DISPLAY_NAME = "Score-counter-BLE"

        const val BT_PERMISSIONS_REQUEST_CODE = 1
        const val NOTIFICATIONS_PERMISSIONS_REQUEST_CODE = 2

        const val SCAN_PERIOD: Long = 7000

        const val MAX_CONNECT_ATTEMPTS = 4

        /** UUID of the Client Characteristic Configuration Descriptor (0x2902). */
        const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805F9B34FB"
        const val DISPLAY_WRITABLE_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"

        const val GATT_MIN_MTU_SIZE = 23
        const val GATT_MAX_MTU_SIZE = 517
        const val GATT_CUSTOM_MTU_SIZE = GATT_MAX_MTU_SIZE

        const val SET_SCORE_CMD_PREFIX = "SET_SCORE="
        const val GET_SCORE_CMD = "GET_SCORE"
        const val SCORE_CMD_PREFIX = "SCORE="
        const val SET_TIME_CMD_PREFIX = "SET_TIME="
        const val SET_ALL_LEDS_ON_CMD_PREFIX = "SET_ALL_LEDS_ON="
        const val SET_BRIGHTNESS_CMD_PREFIX = "SET_BRIGHT="
        const val SET_SHOW_SCORE_CMD_PREFIX = "SET_SHOW_SCORE="
        const val SET_SHOW_DATE_CMD_PREFIX = "SET_SHOW_DATE="
        const val SET_SHOW_TIME_CMD_PREFIX = "SET_SHOW_TIME="
        const val SET_SCROLL_CMD_PREFIX = "SET_SCROLL="
        const val PERSIST_CONFIG_CMD_PREFIX = "PERSIST_CONFIG="
        const val GET_CONFIG_CMD = "GET_CONFIG"
        const val CONFIG_CMD_PREFIX = "CONFIG="
        const val CFG_PERSIST_ACK_CMD = "CFG_PERSIST_ACK"
        const val CRLF = "\r\n"

        const val MIN_SCORE = 0
        const val MAX_SCORE = 999

        const val MAX_OPS_QUEUE_SIZE = 20

        const val PEBBLE_APP_UUID = "eb31d3a1-b305-4b8c-a806-fbf995567f85"

        const val FROM_PEBBLE_CMD_KEY = 10
        const val FROM_PEBBLE_SCORE_1_KEY = 11
        const val FROM_PEBBLE_SCORE_2_KEY = 12
        const val FROM_PEBBLE_TIMESTAMP_KEY = 13

        const val FROM_PEBBLE_CMD_SET_SCORE_VAL = 1
        const val FROM_PEBBLE_CMD_SYNC_SCORE_VAL = 2

        const val TO_PEBBLE_CMD_KEY = 10
        const val TO_PEBBLE_SCORE_1_KEY = 11
        const val TO_PEBBLE_SCORE_2_KEY = 12
        const val TO_PEBBLE_TIMESTAMP_KEY = 13

        const val TO_PEBBLE_CMD_SET_SCORE_VAL = 1
        const val TO_PEBBLE_CMD_SYNC_SCORE_VAL = 2
    }
}