package sara.app.medimeapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import sara.app.medimeapp.databinding.ActivitySppRelayBinding
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Bluetooth Classic SPP Relay: connect to bonded "MediMe", send 0x01 = Relay 1 ON, 0x00 = Relay 1 OFF.
 * User must pair MediMe in Settings → Bluetooth first (PIN 1234).
 */
class SppRelayActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySppRelayBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val deviceNameFilter = "MediMe"
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySppRelayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        binding.buttonConnect.setOnClickListener {
            if (hasBlePermissions()) {
                connectToMediMe()
            } else {
                requestPermissions()
            }
        }
        binding.buttonRelay1On.setOnClickListener { writeRelay(0x01) }
        binding.buttonRelay1Off.setOnClickListener { writeRelay(0x00) }
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    private fun hasBlePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BT_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BT_PERMISSIONS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectToMediMe()
        } else {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToMediMe() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Turn on Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val bonded = bluetoothAdapter!!.bondedDevices
        val mediMe = bonded?.firstOrNull { it.name == deviceNameFilter }
        if (mediMe == null) {
            Toast.makeText(this, getString(R.string.spp_relay_pair_hint), Toast.LENGTH_LONG).show()
            return
        }
        binding.statusText.text = getString(R.string.spp_relay_status_connecting)
        binding.buttonConnect.isEnabled = false
        Thread {
            try {
                var s: BluetoothSocket? = null
                try {
                    s = mediMe.createRfcommSocketToServiceRecord(sppUuid)
                    s.connect()
                } catch (e: IOException) {
                    Log.w(TAG, "createRfcommSocketToServiceRecord failed, trying fallback", e)
                    try {
                        val m = mediMe.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        s = m.invoke(mediMe, 1) as BluetoothSocket
                        s.connect()
                    } catch (e2: Exception) {
                        throw IOException("SPP connect failed", e2)
                    }
                }
                socket = s
                outputStream = s.outputStream
                runOnUiThread {
                    setConnected(true)
                    binding.statusText.text = getString(R.string.spp_relay_status_connected)
                }
            } catch (e: IOException) {
                Log.e(TAG, "connect failed", e)
                runOnUiThread {
                    setConnected(false)
                    binding.statusText.text = getString(R.string.spp_relay_status_disconnected)
                    binding.buttonConnect.isEnabled = true
                    Toast.makeText(this@SppRelayActivity, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.w(TAG, "close error", e)
        }
        outputStream = null
        socket = null
        runOnUiThread { setConnected(false) }
    }

    private fun setConnected(connected: Boolean) {
        binding.buttonRelay1On.isEnabled = connected
        binding.buttonRelay1Off.isEnabled = connected
        binding.buttonConnect.isEnabled = !connected
        if (!connected) {
            binding.statusText.text = getString(R.string.spp_relay_status_disconnected)
        }
    }

    private fun writeRelay(value: Byte) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            outputStream?.write(value.toInt())
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "write failed", e)
            runOnUiThread {
                Toast.makeText(this, "Write failed", Toast.LENGTH_SHORT).show()
                disconnect()
                setConnected(false)
                binding.buttonConnect.isEnabled = true
            }
        }
    }

    companion object {
        private const val TAG = "SppRelay"
        private const val REQUEST_BT_PERMISSIONS = 200
    }
}
