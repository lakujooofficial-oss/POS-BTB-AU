package com.posbtbau

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.util.*

class BluetoothPrinterHelper(private val context: Context) {
    private var socket: BluetoothSocket? = null
    private val printerUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun connect(device: BluetoothDevice): Boolean {
        disconnect()
        return try {
            val printerSocket = device.createRfcommSocketToServiceRecord(printerUuid)
            printerSocket.connect()
            socket = printerSocket
            true
        } catch (exception: IOException) {
            exception.printStackTrace()
            false
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
        socket = null
    }

    fun printText(text: String) {
        if (socket?.isConnected != true) {
            return
        }
        try {
            val output = socket?.outputStream ?: return
            val init = byteArrayOf(0x1B, 0x40)
            val center = byteArrayOf(0x1B, 0x61, 0x01)
            val left = byteArrayOf(0x1B, 0x61, 0x00)
            val lineFeed = byteArrayOf(0x0A)

            output.write(init)
            output.write(center)
            output.write(text.toByteArray(Charsets.UTF_8))
            output.write(lineFeed)
            output.write(left)
            output.flush()
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
    }
}
