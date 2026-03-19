# Build with ESP-IDF extension (Cursor / VS Code)

1. **Open this folder** as the workspace root: `MediMe/esp32` (must contain the project `CMakeLists.txt`).
2. **Configure the extension** once: `Ctrl+Shift+P` → **ESP-IDF: Configure ESP-IDF extension** (use Express and your Espressif install path).
3. **Use the ESP-IDF terminal** (has Python + tools on PATH):
   - `Ctrl+Shift+P` → **ESP-IDF: Open ESP-IDF Terminal**
   - In that terminal run:
     ```bash
     idf.py build
     ```
   - Or after changing Bluetooth mode in `sdkconfig.defaults`:
     ```bash
     idf.py fullclean
     idf.py set-target esp32
     idf.py build
     ```
   - If you see **`esp_bt_main.h: No such file`**, your `sdkconfig` is still on **NimBLE** (old BLE). Delete `sdkconfig` in the `esp32` folder (or run `idf.py fullclean` after deleting it), then build again so `sdkconfig.defaults` (Classic BT + Bluedroid + SPP) is applied.

## Build without typing `idf.py`

- `Ctrl+Shift+B` — runs the default **ESP-IDF: Build** task (uses the extension environment).
- Or **Terminal → Run Build Task…** → **ESP-IDF: Build**.

Tasks are defined in [`.vscode/tasks.json`](.vscode/tasks.json).
