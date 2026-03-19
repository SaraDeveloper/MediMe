package sara.app.medimeapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import sara.app.medimeapp.databinding.ActivityBleRelayBinding
import java.util.UUID

/**
 * BLE Relay control: scan for "MediMe", connect, and send Relay 1 ON (0x01) / OFF (0x00).
 * UUIDs must match ESP32 ble_relay.h.
 */
class BleRelayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBleRelayBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var relayCharacteristic: BluetoothGattCharacteristic? = null
    private val handler = Handler(Looper.getMainLooper())

    private val serviceUuid: UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-8c7d-9e0f1a2b3c4d")
    private val characteristicUuid: UUID = UUID.fromString("a1b2c3d5-e5f6-4a5b-8c7d-9e0f1a2b3c4d")
    private val deviceNameFilter = "MediMe"

    private var scanResultCount = 0

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (ActivityCompat.checkSelfPermission(this@BleRelayActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
            scanResultCount++
            val name = result.scanRecord?.deviceName ?: result.device?.name
            val hasOurService = result.scanRecord?.serviceUuids?.any { it.uuid == serviceUuid } == true
            Log.d(TAG, "Scan #$scanResultCount: name=\"$name\" addr=${result.device.address} hasOurService=$hasOurService")
            if (name == deviceNameFilter || hasOurService) {
                if (ActivityCompat.checkSelfPermission(this@BleRelayActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                stopScan()
                connectToDevice(result.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                binding.statusText.text = getString(R.string.ble_relay_status_disconnected)
                Toast.makeText(this@BleRelayActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val gattCallback = object : android.bluetooth.BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            runOnUiThread {
                if (status != BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_DISCONNECTED) {
                    binding.statusText.text = getString(R.string.ble_relay_status_disconnected)
                    setConnected(false)
                    // 22 = local host terminated (timeout/SMP); 133 = common when using system Bluetooth
                    val msg = when (status) {
                        22 -> "Connection lost (22). Try again; keep phone close to MediMe."
                        133 -> "Connection failed (133). Use \"Scan and connect\" here, not system Bluetooth."
                        else -> "Connection failed (status=$status). Try again or move closer."
                    }
                    Toast.makeText(this@BleRelayActivity, msg, Toast.LENGTH_LONG).show()
                    Log.w(TAG, "onConnectionStateChange status=$status newState=$newState")
                }
            }
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    if (ActivityCompat.checkSelfPermission(this@BleRelayActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                    // Brief delay before discoverServices() to avoid status 22 (local host terminate) on some devices
                    handler.postDelayed({
                        if (ActivityCompat.checkSelfPermission(this@BleRelayActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return@postDelayed
                        gatt?.discoverServices()
                    }, 300)
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    runOnUiThread {
                        setConnected(false)
                        binding.statusText.text = getString(R.string.ble_relay_status_disconnected)
                    }
                    gatt?.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS || gatt == null) return
            val service: BluetoothGattService? = gatt.getService(serviceUuid)
            val chr = service?.getCharacteristic(characteristicUuid)
            if (chr != null) {
                relayCharacteristic = chr
                runOnUiThread {
                    setConnected(true)
                    binding.statusText.text = getString(R.string.ble_relay_status_connected)
                }
            } else {
                runOnUiThread {
                    setConnected(false)
                    binding.statusText.text = getString(R.string.ble_relay_status_disconnected)
                    Toast.makeText(this@BleRelayActivity, "MediMe relay service not found", Toast.LENGTH_SHORT).show()
                }
                if (ActivityCompat.checkSelfPermission(this@BleRelayActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                gatt.disconnect()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread {
                    Toast.makeText(this@BleRelayActivity, "Write failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleRelayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = manager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        binding.buttonScanConnect.setOnClickListener {
            if (hasBlePermissions()) {
                startScanAndConnect()
            } else {
                requestBlePermissions()
            }
        }
        binding.buttonRelay1On.setOnClickListener { writeRelay(0x01) }
        binding.buttonRelay1Off.setOnClickListener { writeRelay(0x00) }
    }

    override fun onDestroy() {
        stopScan()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        }
        bluetoothGatt = null
        relayCharacteristic = null
        super.onDestroy()
    }

    private fun hasBlePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLE_PERMISSIONS
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH),
                REQUEST_BLE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLE_PERMISSIONS && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startScanAndConnect()
        } else {
            Toast.makeText(this, "BLE permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startScanAndConnect() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Turn on Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }
        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "BLE not available", Toast.LENGTH_SHORT).show()
            return
        }
        binding.statusText.text = getString(R.string.ble_relay_status_scanning)
        binding.buttonScanConnect.isEnabled = false
        // No filter: match by name (from scan record) or by our service UUID in advertisement
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        bluetoothLeScanner?.startScan(emptyList(), settings, scanCallback)
        scanResultCount = 0
        handler.postDelayed({
            if (bluetoothGatt == null) {
                stopScan()
                runOnUiThread {
                    binding.statusText.text = getString(R.string.ble_relay_status_disconnected)
                    binding.buttonScanConnect.isEnabled = true
                    val msg = "MediMe not found ($scanResultCount devices scanned). Check logcat tag \"BleRelay\" for list."
                    Toast.makeText(this@BleRelayActivity, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Scan finished: $scanResultCount devices seen. MediMe not matched.")
                }
            }
        }, SCAN_TIMEOUT_MS)
    }

    private fun stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        bluetoothLeScanner?.stopScan(scanCallback)
        runOnUiThread { binding.buttonScanConnect.isEnabled = true }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        runOnUiThread { binding.statusText.text = getString(R.string.ble_relay_status_connecting) }
        // Use BLE transport explicitly so Android doesn't try classic Bluetooth (MediMe is BLE-only)
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, gattCallback)
        }
    }

    private fun setConnected(connected: Boolean) {
        binding.buttonRelay1On.isEnabled = connected
        binding.buttonRelay1Off.isEnabled = connected
        binding.buttonScanConnect.isEnabled = !connected
    }

    private fun writeRelay(value: Byte) {
        val chr = relayCharacteristic ?: return
        chr.value = byteArrayOf(value)
        chr.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        bluetoothGatt?.writeCharacteristic(chr)
    }

    companion object {
        private const val TAG = "BleRelay"
        private const val REQUEST_BLE_PERMISSIONS = 100
        private const val SCAN_TIMEOUT_MS = 15_000L
    }
}
