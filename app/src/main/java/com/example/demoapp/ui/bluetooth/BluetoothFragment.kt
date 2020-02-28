package com.example.demoapp.ui.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.demoapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList

private const val REQUEST_ENABLE_BT = 1
lateinit var send_data: String
lateinit var view_data: TextView
var MY_UUID_INSECURE: UUID =  UUID.randomUUID()

class BluetoothTestFragment : Fragment() {
    private val NAME: String? = "TEST"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var btnPaired: Button
    private lateinit var btnServer: Button
    private lateinit var btnPair: Button
    private lateinit var btnSend: Button
    private lateinit var navBar: BottomNavigationView
    private val testData: String = "Test"

    private lateinit var mmDevice: BluetoothDevice
    private lateinit var deviceUUID: UUID
    var mConnectedThread: ConnectedThread? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_bluetooth_test, container, false)
        navBar = activity!!.findViewById(R.id.bottom_nav_view)
        navBar.visibility = View.GONE

        send_data = testData
        view_data = root.findViewById(R.id.textView2)

        //region bluetooth adapter code
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "This device does not support bluetooth", Toast.LENGTH_LONG)
                .show()
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        //endregion

        //region button on click listeners
        btnPaired = root.findViewById(R.id.btnPaired)
        btnPaired.setOnClickListener { pairedDevices() }
        btnServer = root.findViewById(R.id.btnServerStart)
        btnServer.setOnClickListener { startServer() }
        btnPair = root.findViewById(R.id.btnPair)
        btnPair.setOnClickListener{pairDevice()}
        btnSend = root.findViewById(R.id.btnSend)
        btnSend.setOnClickListener{sendMessage()}
        //endregion
        return root
    }

    private fun pairDevice() {
        val pairedDevices =
            bluetoothAdapter?.bondedDevices
        if (pairedDevices != null) {
            Log.e("MAinActivity", "" + pairedDevices.size)
            if (pairedDevices.size > 0) {
                val devices: Array<Any> = pairedDevices.toTypedArray()
                val device = devices[0] as BluetoothDevice
                var list = device.uuids
                var deviceUUID = UUID.fromString(list[0].toString())
                Log.e("MainActivity", "" + device)
                Log.e("MAinActivity", "" + deviceUUID)
                val connect = ConnectThread(device, MY_UUID_INSECURE)
                connect.start()
            }
        }
    }

    inner class ConnectThread(device: BluetoothDevice, uuid: UUID) : Thread() {
        private var mmSocket: BluetoothSocket? = null

        init {
            Log.d(TAG, "ConnectThread: started.")
            mmDevice = device
            deviceUUID = uuid
        }

        override fun run() {
            var tmp: BluetoothSocket? = null
            Log.i(TAG, "RUN mConnectThread ")

            try {
                Log.d(
                    TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                            + MY_UUID_INSECURE
                )
               tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE)
            } catch (e: IOException) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.message)
            }


                mmSocket = tmp


            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket?.connect()

            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket?.close()
                    Log.d(TAG, "run: Closed Socket.")
                } catch (e1: IOException) {
                    Log.e(
                        TAG,
                        "mConnectThread: run: Unable to close connection in socket " + e1.message
                    )
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: $MY_UUID_INSECURE")
            }
            //will talk about this in the 3rd video
            mmSocket?.let { connected(it) }
        }

        fun cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket")
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "cancel: close() of mmSocket in connectThread failed " + e.message)
            }
        }
    }

    private fun connected(mmSocket: BluetoothSocket) {
        Log.d(TAG, "connected: Starting.")

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(mmSocket)
        mConnectedThread!!.start()
    }

    inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private var mmSocket: BluetoothSocket? = null
        private var mmInStream: InputStream? = null
        private var mmOutStream: OutputStream? = null

        init {
            Log.d(TAG, "ConnectedThread: Starting.")
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = mmSocket!!.inputStream
                tmpOut = mmSocket!!.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            val buffer = ByteArray(1024) // buffer store for the stream
            var bytes: Int // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) { // Read from the InputStream
                try {
                    bytes = mmInStream!!.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    Log.d(TAG, "InputStream: $incomingMessage")
                    activity?.runOnUiThread { view_data.text = incomingMessage }
                } catch (e: IOException) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.message)
                    break
                }
            }
        }

        fun write(bytes: ByteArray?) {
            val text = String(bytes!!, Charset.defaultCharset())
            Log.d(TAG, "write: Writing to outputstream: $text")
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "write: Error writing to output stream. " + e.message)
            }
        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
            }
        }
    }

    private fun sendMessage() {
        val bytes = send_data.toByteArray(Charset.defaultCharset())
        mConnectedThread?.write(bytes)
    }



    private fun startServer() {
        val accept = AcceptThread()
        accept.start()
    }

    inner class AcceptThread : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID_INSECURE)
        }

        override fun run() {
            Log.d(TAG, "run: AcceptThread Running")

            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                                    } catch (e: IOException) {
                    Log.e(TAG, "AcceptThread: IOException: " + e.message)
                    shouldLoop = false
                    null
                }
                if (socket != null) {
                    connected(socket)
                }
                Log.i(TAG, "End mAcceptThread ")
            }
        }

        fun cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.")
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.message)
            }
        }
    }

    /*function to list all paired devices*/
    private fun pairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val deviceList = ArrayList<String>()
        pairedDevices?.forEach { device ->
            deviceList.add(device.name)

            val data = Bundle()
            data.putStringArrayList("deviceName", deviceList)

            val fragment: Fragment = ControlFragment()
            val fragmentManager: FragmentManager? = parentFragmentManager
            fragment.arguments = data
            fragmentManager!!.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navBar.visibility = View.VISIBLE
        AcceptThread().cancel()
    }

}