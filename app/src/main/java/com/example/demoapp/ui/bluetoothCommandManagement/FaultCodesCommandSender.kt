package com.example.demoapp.ui.bluetoothCommandManagement

import android.bluetooth.BluetoothDevice
import android.content.ContentValues
import android.util.Log
import com.example.demoapp.ui.faultCodes.FaultCodesViewModel
import com.github.pires.obd.commands.control.TroubleCodesCommand
import com.github.pires.obd.commands.protocol.ObdResetCommand
import java.io.InputStream
import java.io.OutputStream

class FaultCodesCommandSender(device: BluetoothDevice, providedViewModel: FaultCodesViewModel) :
    AbstractCommandSender<FaultCodesViewModel>(device, providedViewModel) {

    override fun performCommand(inputStream: InputStream, outputStream: OutputStream) {
        val obdResetCommand = ObdResetCommand()
        obdResetCommand.run(inputStream, outputStream)

        try {
            sleep(500)
        } catch (e: InterruptedException) {
            Log.e(ContentValues.TAG,"Error with OBD reset:", e)
        }

        val troubleCodesCommand = TroubleCodesCommand()
        troubleCodesCommand.run(inputStream,outputStream)
        val troubleCodesResult = troubleCodesCommand.calculatedResult

        val fViewModel = viewModel
        fViewModel.faultCode.postValue(troubleCodesResult)
    }
}