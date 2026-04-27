package com.studymentor.chess

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnHost: Button
    private lateinit var btnJoin: Button
    private lateinit var btnSearch: Button
    private lateinit var devicesList: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardDevices: MaterialCardView

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val pairedDevices = mutableListOf<BluetoothDevice>()
    private lateinit var deviceArrayAdapter: ArrayAdapter<String>

    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

    companion object {
        private val MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private const val NAME = "ChessMultiplayer"
        var currentConnectedThread: ConnectedThread? = null
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)
        initViews()
        setupClickListeners()
        checkPermissions()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvBluetoothStatus)
        btnHost = findViewById(R.id.btnHostGame)
        btnJoin = findViewById(R.id.btnJoinGame)
        btnSearch = findViewById(R.id.btnSearchDevices)
        devicesList = findViewById(R.id.devicesList)
        progressBar = findViewById(R.id.progressBar)
        cardDevices = findViewById(R.id.cardDevices)
        deviceArrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        devicesList.adapter = deviceArrayAdapter
        
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        btnHost.setOnClickListener { if (checkBluetooth()) startHost() }
        btnJoin.setOnClickListener { if (checkBluetooth()) startJoin() }
        btnSearch.setOnClickListener { if (checkBluetooth()) searchDevices() }
        devicesList.setOnItemClickListener { _, _, position, _ ->
            if (position < pairedDevices.size) connectToDevice(pairedDevices[position])
        }
    }

    private fun checkBluetooth(): Boolean {
        if (bluetoothAdapter == null) return false
        if (!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
                    return false
                }
            }
            startActivityForResult(this, intent, 2, null)
            return false
        }
        return true
    }

    private fun checkPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        } else perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        
        val toRequest = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (toRequest.isNotEmpty()) ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 3)
    }

    private fun startHost() {
        tvStatus.text = "Интизори пайвастшавӣ..."
        progressBar.visibility = android.view.View.VISIBLE
        acceptThread?.cancel()
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    private fun startJoin() {
        tvStatus.text = "Дастгоҳро интихоб кунед"
        cardDevices.visibility = android.view.View.VISIBLE
        searchDevices()
    }

    private fun searchDevices() {
        deviceArrayAdapter.clear()
        pairedDevices.clear()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothAdapter?.bondedDevices?.forEach {
                pairedDevices.add(it)
                deviceArrayAdapter.add("${it.name}\n${it.address}")
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            tvStatus.text = "Пайвастшавӣ ба ${device.name}..."
            progressBar.visibility = android.view.View.VISIBLE
            connectThread?.cancel()
            connectThread = ConnectThread(device)
            connectThread?.start()
        }
    }

    private fun startGame(socket: BluetoothSocket, isHost: Boolean) {
        connectedThread = ConnectedThread(socket)
        currentConnectedThread = connectedThread
        connectedThread?.start()
        GameActivity.setBluetoothCallback(object : GameActivity.BluetoothCallback {
            override fun sendMessage(message: String) { currentConnectedThread?.write(message) }
        })
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("game_mode", "bluetooth")
            putExtra("is_bluetooth_host", isHost)
        }
        startActivity(intent)
        finish()
    }

    inner class AcceptThread : Thread() {
        private var mmServerSocket: BluetoothServerSocket? = null
        init {
            try {
                if (ActivityCompat.checkSelfPermission(this@BluetoothActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    mmServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
                }
            } catch (e: IOException) {}
        }
        override fun run() {
            while (true) {
                val socket = try { mmServerSocket?.accept() } catch (e: IOException) { null }
                if (socket != null) {
                    handler.post { startGame(socket, true) }
                    try { mmServerSocket?.close() } catch (e: IOException) {}
                    break
                }
            }
        }
        fun cancel() { try { mmServerSocket?.close() } catch (e: IOException) {} }
    }

    inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private var mmSocket: BluetoothSocket? = null
        init {
            try {
                if (ActivityCompat.checkSelfPermission(this@BluetoothActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                }
            } catch (e: IOException) {}
        }
        override fun run() {
            try {
                mmSocket?.connect()
                mmSocket?.let { s -> handler.post { startGame(s, false) } }
            } catch (e: IOException) {
                handler.post {
                    tvStatus.text = "Хатогӣ"
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }
        fun cancel() { try { mmSocket?.close() } catch (e: IOException) {} }
    }

    inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        fun write(msg: String) { try { mmOutStream.write(msg.toByteArray()) } catch (e: IOException) {} }
        override fun run() {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = mmInStream.read(buffer)
                    val msg = String(buffer, 0, bytes)
                    handler.post { GameActivity.receiveMove(msg) }
                } catch (e: IOException) {
                    handler.post { GameActivity.receiveMove("DISCONNECTED") }
                    break
                }
            }
        }
    }
}