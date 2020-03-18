package com.example.demoapp.ui.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.demoapp.R
import com.example.demoapp.ui.performance.PerformanceFragment
import com.example.demoapp.ui.performance.PerformanceViewModel
import com.example.demoapp.utilities.MyServerBluetoothService
import com.example.demoapp.utilities.REQUEST_ENABLE_BT
import com.google.android.material.bottomnavigation.BottomNavigationView

class BluetoothFragment : Fragment() {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var btnPair: Button
    private lateinit var navBar: BottomNavigationView
    private lateinit var currentDevice: BluetoothDevice
    private lateinit var data: Bundle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_bluetooth, container, false)
        navBar = activity!!.findViewById(R.id.bottom_nav_view)
        navBar.visibility = View.GONE

        val spinner: Spinner = root.findViewById(R.id.spnDevices)
        pairedDevices(spinner, performanceViewModel = PerformanceViewModel())

        //region bluetooth adapter code
        //show a toast notification if the device does not support bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "This device does not support bluetooth", Toast.LENGTH_LONG)
                .show()
        }

        //if the device supports bluetooth but adapter is not enabled, request it to be enabled
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(
                enableBtIntent,
                REQUEST_ENABLE_BT
            )
        }
        //endregion

        btnPair = root.findViewById(R.id.btnSelectDevice)
        btnPair.setOnClickListener { sendFragment(data) }

        return root
    }

    private fun sendFragment(data:Bundle) {
        Toast.makeText(context, "Device Selected: ${currentDevice.name}", Toast.LENGTH_LONG).show()
        val perfFragment = PerformanceFragment()
        val fragmentManager = parentFragmentManager
        perfFragment.arguments = data
        fragmentManager.beginTransaction().replace(R.id.fragmentContainer, perfFragment).commit()
    }

    private fun pairedDevices(spinner: Spinner, performanceViewModel: PerformanceViewModel) {
        val pairedList = bluetoothAdapter?.bondedDevices
        val deviceList = java.util.ArrayList<String>()
        pairedList?.forEach { device ->
            deviceList.add(device.name)

            val adapter = ArrayAdapter(
                this.context!!,
                android.R.layout.simple_spinner_dropdown_item,
                deviceList
            )
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.e("MainActivity", "" + pairedList.size)
                    if (pairedList.size > 0) {
                        val devices: Array<Any> = pairedList.toTypedArray()
                        currentDevice = devices[0] as BluetoothDevice
                        Log.e("MainActivity", "" + currentDevice)
                        data = Bundle()
                        data.putParcelable("currentDevice", currentDevice)
                    }
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    Log.e("MainActivity", "" + pairedList.size)
                    if (pairedList.size > 0) {
                        val devices: Array<Any> = pairedList.toTypedArray()
                        currentDevice = devices[position] as BluetoothDevice
                        Log.e("MainActivity", "" + currentDevice)
                        data = Bundle()
                        data.putParcelable("currentDevice", currentDevice)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navBar.visibility = View.VISIBLE
        btnPair.visibility = View.INVISIBLE
        MyServerBluetoothService(handler).AcceptThread().cancel()
    }
}