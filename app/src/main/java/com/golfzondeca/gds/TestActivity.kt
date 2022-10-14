package com.golfzondeca.gds

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class TestActivity: ComponentActivity(), GDSRepository.Callback {
    private val gdsRepository by lazy {
        GDSRepository(this, "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        gdsRepository.addCallback(this)
        gdsRepository.loadCCData(
            "66011",
            1,
            0,
            2,
            useAltitude = true,
            useHoleMap = true,
            useUndulationMap = false
        )
    }

    override fun onCCDataReady(ccID: String) {
        Timber.d("onCCDataReady")
        Timber.d("altitude %d", gdsRepository.getAltitude("66011", 37.094768, 127.335132))
        Timber.d("altitude %d", gdsRepository.getAltitude("66011", 37.095045, 127.334654))
    }

    override fun onCCDataFailed(ccID: String, error: Int) {
        Timber.d("onCCDataFailed")
    }
}