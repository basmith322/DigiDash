package com.example.demoapp.ui.performance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PerformanceViewModel : ViewModel() {
    private val randomMph = (0..130).random()

    /**Mutable properties that can be changed. Titles are static but values are set to returned
     * values from OBD commands */
    //Current Speed Title
    private val _textCurrentSpeedTitle:MutableLiveData<String> = MutableLiveData<String>().apply {
        value = "Current Speed"
    }
    val textCurrentSpeedTitle: LiveData<String> = _textCurrentSpeedTitle

    //Current speed value returned from ODB
    val currentSpeed by lazy {
        MutableLiveData<Int>()
    }

    //RPM Title
    private val _textRPMTitle = MutableLiveData<String>().apply {
        value = "Current RPM"
    }
    val textRPMTitle: LiveData<String> = _textRPMTitle

    //RPM value returned from ODB
    val currentRPM by lazy {
        MutableLiveData<Int>()
    }

    //Boost Pressure Title
    private val _textPSITitle = MutableLiveData<String>().apply {
        value = "Current PSI"
    }
    val textPSITitle: LiveData<String> = _textPSITitle

    //Boost value returned from ODB
    val currentBoost by lazy {
        MutableLiveData<Int>()
    }

    //Avg Speed
    private val _textAvgSpeedTitle = MutableLiveData<String>().apply {
        value = "Average Speed"
    }
    val textAvgSpeedTitle: LiveData<String> = _textAvgSpeedTitle

    private val _textAvgSpeed = MutableLiveData<String>().apply {
        value = randomMph.toString() + "Mph"
    }
    val textAvgSpeed: LiveData<String> = _textAvgSpeed

    //Max Speed
    private val _textMaxSpeedTitle = MutableLiveData<String>().apply {
        value = "Max Speed"
    }
    val textMaxSpeedTitle: LiveData<String> = _textMaxSpeedTitle

}