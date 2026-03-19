# MediMe ESP32 BLE Relay

BLE GATT server with device name **MediMe**. One writeable characteristic: send `0x01` for Relay 1 ON, `0x00` for Relay 1 OFF.

## Build and flash

From the ESP-IDF environment (run `export.ps1` or `export.bat` from your IDF path first):

```bash
cd esp32
idf.py set-target esp32
idf.py build
idf.py -p COMx flash monitor
```

Replace `COMx` with your ESP32 serial port.

## Test with Android app

1. Flash the firmware to the ESP32.
2. Open the MediMe Android app, tap the menu (three dots) and choose **BLE Relay**.
3. Tap **Scan and connect to MediMe**. Ensure Bluetooth is on and grant permissions if asked.
4. When status shows **Connected to MediMe**, use **Relay 1 ON** and **Relay 1 OFF** to control the relay on GPIO 16.

## Debugging: "MediMe not found" on Android

### 1. Confirm ESP32 is advertising (serial monitor)

After flashing, open the serial monitor (`idf.py monitor` or your IDE). You should see:

```
I (xxx) main: MediMe BLE Relay starting
I (xxx) ble_relay: Initializing NimBLE...
I (xxx) ble_relay: NimBLE OK, device name will be "MediMe"
I (xxx) ble_relay: BLE stack synced, starting advertising
I (xxx) ble_relay: Advertising as "MediMe" (connect with app or nRF Connect)
```

If you see errors or never see "Advertising as \"MediMe\"", BLE did not start correctly (e.g. wrong sdkconfig, crash).

### 2. Confirm ESP32 is visible (nRF Connect or similar)

On your phone, install **nRF Connect for Mobile** (Nordic) or any BLE scanner. Start a scan. You should see a device named **MediMe**. If it does not appear, the ESP32 is not advertising (check step 1) or the phone is out of range.

### 3. See what the Android app sees (logcat)

With the phone connected via USB (USB debugging on), run:

```bash
adb logcat -s BleRelay:D
```

Then in the app tap **Scan and connect to MediMe**. You will see lines like:

- `Scan #1: name="MediMe" addr=... hasOurService=true` — app should then connect.
- `Scan #N: name="..." addr=...` — all BLE devices the phone sees.
- `Scan finished: N devices seen` — when the 15 s scan ends.

If you see `name="MediMe"` or `hasOurService=true` but the app does not connect, there is an app bug. If you never see MediMe in the list, the phone is not receiving the ESP32’s advertisements (distance, interference, or ESP32 not advertising).

### 4. Checklist

- [ ] Serial monitor shows "Advertising as \"MediMe\"".
- [ ] nRF Connect (or similar) shows a device named "MediMe".
- [ ] Phone Bluetooth is on; app has Bluetooth (and location if prompted) permission.
- [ ] ESP32 and phone are within a few metres.

## UUIDs (for custom clients)

- Service: `a1b2c3d4-e5f6-4a5b-8c7d-9e0f1a2b3c4d`
- Characteristic (relay command): `a1b2c3d5-e5f6-4a5b-8c7d-9e0f1a2b3c4d`
- Write 1 byte: `0x01` = Relay 1 ON, `0x00` = Relay 1 OFF.
