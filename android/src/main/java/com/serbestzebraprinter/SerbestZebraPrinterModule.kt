package com.serbestzebraprinter

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.util.Log
import android.widget.Toast
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.printer.PrinterStatus
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException
import java.nio.charset.Charset
import com.facebook.react.bridge.ReactMethod

class SerbestZebraPrinterModule(reactContext: ReactApplicationContext) :
    NativeSerbestZebraPrinterSpec(reactContext) {

    private var connection: Connection? = null
    private var printer: ZebraPrinter? = null

    companion object {
        const val NAME = "SerbestZebraPrinter"
    }
    private fun sendEvent(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun sendEvent(eventName: String, params: WritableArray) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    @ReactMethod
    override fun discoverPrinters(promise: Promise) {
        Log.d("SerbestZebraPrinterModule", "discoverPrinters called")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(reactApplicationContext, "Zebra: Starting Scan...", Toast.LENGTH_SHORT).show()
        }
        val bluetoothManager = reactApplicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null) {  
            Log.e("SerbestZebraPrinterModule", "Bluetooth adapter is null")
            promise.reject("NO_BLUETOOTH", "Bluetooth not supported")
            return
        }

        if (!adapter.isEnabled) {
            Log.e("SerbestZebraPrinterModule", "Bluetooth is disabled")
            promise.reject("BLUETOOTH_DISABLED", "Bluetooth is disabled")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(reactApplicationContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                 promise.reject("PERMISSION_DENIED", "Bluetooth scan permission denied")
                 return
            }
            if (ActivityCompat.checkSelfPermission(reactApplicationContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                 promise.reject("PERMISSION_DENIED", "Bluetooth connect permission denied")
                 return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(reactApplicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                 promise.reject("PERMISSION_DENIED", "Location permission denied (required for Bluetooth scan)")
                 return
            }
        }

        val pairedDevices = adapter.bondedDevices
        val printerList = Arguments.createArray()
        for (device in pairedDevices) {
            val map = Arguments.createMap()
            map.putString("name", device.name ?: "Unknown Device")
            map.putString("serialNumber", device.address)
            map.putString("connectionType", "BLUETOOTH")
            printerList.pushMap(map)
        }
        Log.d("SerbestZebraPrinterModule", "Sending ${printerList.size()} paired devices")
        sendEvent("PrinterFound", printerList)

        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }
        try {
             Log.d("SerbestZebraPrinterModule", "Starting discovery")
             val filter = android.content.IntentFilter(BluetoothDevice.ACTION_FOUND)
             reactApplicationContext.registerReceiver(discoveryReceiver, filter)
             val started = adapter.startDiscovery()
             Log.d("SerbestZebraPrinterModule", "Discovery started: $started")
             promise.resolve(null)
             android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                 if (adapter.isDiscovering) {
                     adapter.cancelDiscovery()
                 }
                 try {
                    reactApplicationContext.unregisterReceiver(discoveryReceiver)
                 } catch (e: Exception) {
                 }
             }, 10000)
        } catch (e: Exception) {
            promise.reject("DISCOVERY_ERROR", "Failed to start discovery: ${e.message}")
        }
    }

    private val discoveryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d("SerbestZebraPrinterModule", "Broadcast received: $action")
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    Log.d("SerbestZebraPrinterModule", "Device found: ${it.name} - ${it.address}")
                    if (it.name != null) {
                         val printerList = Arguments.createArray()
                         val map = Arguments.createMap()
                         map.putString("name", it.name)
                         map.putString("serialNumber", it.address)
                         map.putString("connectionType", "BLUETOOTH")
                         printerList.pushMap(map)
                         sendEvent("PrinterFound", printerList)
                    }
                }
            }
        }
    }
    @ReactMethod
    override fun connect(macAddress: String, promise: Promise) {
        Thread {
            try {
                val bluetoothManager = reactApplicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                if (adapter != null && adapter.isDiscovering) {
                    Log.d("SerbestZebraPrinterModule", "Cancelling discovery before connect")
                    adapter.cancelDiscovery()
                }

                try {
                    if (connection != null) {
                        connection!!.close()
                    }
                } catch (e: Exception) {
                    Log.w("SerbestZebraPrinterModule", "Error closing previous connection: ${e.message}")
                }
                connection = null
                printer = null

                connection = BluetoothConnection(macAddress)
                connection!!.open()

                try {
                    printer = ZebraPrinterFactory.getInstance(connection)
                    val map = Arguments.createMap()
                    map.putString("status", "CONNECTED")
                    sendEvent("PrinterStatusChanged", map)
                    promise.resolve(true)
                } catch (e: Exception) {
                    Log.e("SerbestZebraPrinterModule", "Failed to get printer instance", e)
                    try {
                         connection?.close()
                    } catch (closeErr: Exception) {}
                    connection = null
                    promise.reject("CONNECTION_ERROR", "Failed to get printer instance: ${e.message}")
                }

            } catch (e: Exception) {
                Log.e("SerbestZebraPrinterModule", "Connection failed", e)
                try {
                     connection?.close()
                } catch (closeErr: Exception) {}
                connection = null
                promise.reject("CONNECTION_FAILED", "Could not connect to printer: ${e.message}")
            }
        }.start()
    }
    @ReactMethod
    override fun print(data: String, promise: Promise) {
        Thread {
            try {
                if (connection == null || !connection!!.isConnected) {
                    promise.reject("NOT_CONNECTED", "Printer is not connected")
                    return@Thread
                }

                val bytes = data.toByteArray(Charset.forName("UTF-8"))
                connection!!.write(bytes)
                promise.resolve(bytes.size)

            } catch (e: Exception) {
                promise.reject("PRINT_ERROR", "Failed to print: ${e.message}")
            }
        }.start()
    }
    @ReactMethod
    override fun checkStatus(promise: Promise) {
         Thread {
             try {
                 if (connection == null || !connection!!.isConnected) {
                     val map = Arguments.createMap()
                     map.putString("status", "DISCONNECTED")
                     promise.resolve(map)
                     return@Thread
                 }

                 if (printer != null) {
                     val status = printer!!.currentStatus
                     val map = Arguments.createMap()
                     if (status.isReadyToPrint) {
                         map.putString("status", "READY")
                         map.putBoolean("isReady", true)
                     } else {
                         map.putString("status", "BUSY")
                         map.putBoolean("isReady", false)
                     }
                     promise.resolve(map)
                 } else {
                     val map = Arguments.createMap()
                     map.putString("status", "CONNECTED")
                     promise.resolve(map)
                 }
             } catch (e: Exception) {
                 val map = Arguments.createMap()
                 map.putString("status", "CONNECTED_UNKNOWN")
                 promise.resolve(map)
             }
         }.start()
    }
    @ReactMethod
    override fun disconnect(promise: Promise) {
        Thread {
            try {
                if (connection != null) {
                    connection!!.close()
                    connection = null
                    printer = null
                }
                val map = Arguments.createMap()
                map.putString("status", "DISCONNECTED")
                sendEvent("PrinterStatusChanged", map)
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("DISCONNECT_ERROR", "Failed to disconnect: ${e.message}")
            }
        }.start()
    }

    // NativeEventEmitter'ın zorunlu tuttuğu metodlar - event dinleyici sayısını
    // takip etmek istemiyorsak boş bırakmak yeterli
    @ReactMethod
    override fun addListener(eventName: String) {
        // no-op
    }
    @ReactMethod
    override fun removeListeners(count: Double) {
        // no-op
    }
}