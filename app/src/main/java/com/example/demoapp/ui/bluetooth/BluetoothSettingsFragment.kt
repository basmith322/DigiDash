package com.example.demoapp.ui.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.demoapp.R
import com.example.demoapp.ui.performance.PerformanceFragment
import com.example.demoapp.utilities.CommandService
import com.example.demoapp.utilities.DeviceSingleton
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.system.exitProcess

const val REQUEST_ENABLE_BT = 1

class BluetoothSettingsFragment : Fragment() {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var btnPair: Button
    private lateinit var navBar: BottomNavigationView
    private val bluetoothSettingsViewModel: BluetoothSettingsViewModel by viewModels()
    private lateinit var progressBar: ProgressBar
    private lateinit var viewModel: BluetoothSettingsViewModel
    private lateinit var spinner: Spinner
    private lateinit var mainHandler: Handler
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = requireContext().getSharedPreferences("storedPrefs", Context.MODE_PRIVATE)

        //show a toast notification if the device does not support bluetooth
        if (bluetoothAdapter?.isEnabled != true) {
            showAlert()
        }
        //If there is no Bluetooth Adapter then do not show prompt and inform user the device does not support BT
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "This device does not support bluetooth", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.bluetooth_settings_fragment, container, false)
        mainHandler = Handler(Looper.getMainLooper())

        //Initialize view elements to variables and set functions
        progressBar = root.findViewById(R.id.progressBar_BTSettings)
        progressBar.visibility = View.INVISIBLE
        navBar = requireActivity().findViewById(R.id.bottom_nav_view)
        navBar.visibility = View.GONE
        btnPair = root.findViewById(R.id.btnSelectDevice)
        btnPair.setOnClickListener { tryConnect() }
        spinner = root.findViewById(R.id.spnDevices)
        pairedDevices(spinner)


        //Observer to monitor the value returned from the OBD for protocol
        val protocolObserver = Observer<String> { currentProtocolFromOBD ->
            //If the protocol returns OK then the connection is successful
            if (currentProtocolFromOBD.contains("OK", true)) {
                Toast.makeText(
                    context,
                    "Connection to " + DeviceSingleton.bluetoothDevice!!.name + " successful",
                    Toast.LENGTH_LONG
                ).show()
                with(sharedPref.edit()) {
                    putString("deviceAddress", DeviceSingleton.bluetoothDevice!!.address)
                    putBoolean("previouslyStarted", true)
                    apply()
                }
                //Replace the BT fragment with the performance fragment and remove BT fragment
                val perfFragment = PerformanceFragment()
                val fragmentManager = parentFragmentManager

                fragmentManager.beginTransaction().replace(R.id.nav_host_fragment, perfFragment)
                    .commit()
            }
        }
        bluetoothSettingsViewModel.returnedProtocol.observe(viewLifecycleOwner, protocolObserver)
        return root
    }

    private fun showAlert() {
        //Use an alert dialog to inform the user they must enable bluetooth
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enable Bluetooth")
        builder.setMessage("This app requires a Bluetooth connection to function.\nPlease allow the Bluetooth Permission to continue.")
        builder.setPositiveButton("Continue") { dialog, which ->
            startBluetooth()
        }
        builder.setNegativeButton("Exit") { dialog, which ->
            exitProcess(0)
        }
        if (bluetoothAdapter?.isEnabled != true) {
            builder.show()
        }
    }

    private fun startBluetooth() {
        //if the device supports bluetooth but adapter is not enabled, request it to be enabled
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(
                enableBtIntent,
                REQUEST_ENABLE_BT
            )
        }
    }

    private fun tryConnect() {
        //Try to connect to the bluetooth device for 10 seconds
        progressBar.visibility = View.VISIBLE
        try {
            CommandService().connectToServerBTSettings(
                bluetoothSettingsViewModel,
                DeviceSingleton.bluetoothDevice!!
            )
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, "Error Connecting to OBD Device", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.INVISIBLE
            Log.e(ContentValues.TAG, "Error Connecting to Server: ", e)
        }
        mainHandler.postDelayed({
            if (!bluetoothSettingsViewModel.hasConnected) {
                Toast.makeText(
                    context,
                    "Connection failed. Check your device is paired and connected to the vehicle and try again",
                    Toast.LENGTH_LONG
                ).show()
                progressBar.visibility = View.INVISIBLE
            }
        }, 10000)
    }

    private fun pairedDevices(spinner: Spinner) {
        val pairedList = bluetoothAdapter?.bondedDevices!! //Create a list of paired devices
        val deviceList = ArrayList<String>() //ArrayList of strings to show in the spinner
        //For each device in the pairedList, add the device name to the deviceList
        pairedList.forEach { device ->
            deviceList.add(device.name)

            val adapter = ArrayAdapter(
                this.requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                deviceList
            )
            spinner.adapter = adapter

            //If no device is selected then set the first device in the list as the bluetoothDevice
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.e("MainActivity", "" + pairedList.size)
                    if (pairedList.isNotEmpty()) {
                        val devices = pairedList.toTypedArray()
                        DeviceSingleton.bluetoothDevice = devices[0]
                        Log.e("MainActivity", "" + DeviceSingleton.bluetoothDevice)
                    }
                }

                //When an device is selected, set the device as the bluetoothDevice
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    Log.e("MainActivity", "" + pairedList.size)
                    if (pairedList.isNotEmpty()) {
                        val devices = pairedList.toTypedArray()
                        DeviceSingleton.bluetoothDevice = devices[position]
                        Log.e("MainActivity", "" + DeviceSingleton.bluetoothDevice)
                    }
                }
            }
        }
    }

    override fun onPause() {
        //Stop the handler to prevent it calling repeatedly when not needed
        mainHandler.removeCallbacksAndMessages(null)
        progressBar.visibility = View.INVISIBLE
        super.onPause()
    }

    override fun onDestroyView() {
        //Stop the handler to prevent it calling repeatedly when not needed
        mainHandler.removeCallbacksAndMessages(null)
        navBar.visibility = View.VISIBLE
        btnPair.visibility = View.INVISIBLE
        super.onDestroyView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(bluetoothSettingsViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        //Show alert dialog when the app resumes to ensure app doesn't display blank screen
        showAlert()
    }

}
