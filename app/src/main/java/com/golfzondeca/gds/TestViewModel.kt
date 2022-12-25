package com.golfzondeca.gds

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class TestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
): ViewModel(), GDSRepository.Callback {
    private val gdsRepository by lazy {
        GDSRepository(context, "golfzondeca", "")
    }

    val altitudeData = MutableLiveData<Int?>(null)
    val holeMapData = MutableLiveData<Bitmap?>(null)
    val undulationMapData = MutableLiveData<ArrayList<Bitmap>?>(null)

    val ccID = MutableStateFlow("65706")
    val countryCode = MutableStateFlow("1")
    val stateCode = MutableStateFlow("0")
    val courseCount = MutableStateFlow("4")
    val courseNum = MutableStateFlow("1")
    val latitude = MutableStateFlow("33.447024")
    val longitude = MutableStateFlow("126.511686")
    val holeNum = MutableStateFlow("1")

    init {
        gdsRepository.addCallback(this)
    }

    override fun onCleared() {
        super.onCleared()
        gdsRepository.removeCallback(this)
    }

    fun onCCIDChanged(ccID: String) {
        viewModelScope.launch {
            this@TestViewModel.ccID.emit(ccID)
        }
    }

    fun onCountryCodeChanged(countryCode: String) {
        viewModelScope.launch {
            this@TestViewModel.countryCode.emit(countryCode)
        }
    }

    fun onStateCodeChanged(stateCode: String) {
        viewModelScope.launch {
            this@TestViewModel.stateCode.emit(stateCode)
        }
    }

    fun onCourseCountChanged(courseCount: String) {
        viewModelScope.launch {
            this@TestViewModel.courseCount.emit(courseCount)
        }
    }

    fun onCourseNumChanged(courseNum: String) {
        viewModelScope.launch {
            this@TestViewModel.courseNum.emit(courseNum)
        }
    }

    fun onLatitudeChanged(latitude: String) {
        viewModelScope.launch {
            this@TestViewModel.latitude.emit(latitude)
        }
    }

    fun onLongitudeChanged(longitude: String) {
        viewModelScope.launch {
            this@TestViewModel.longitude.emit(longitude)
        }
    }

    fun onHoleNumChanged(holeNum: String) {
        viewModelScope.launch {
            this@TestViewModel.holeNum.emit(holeNum)
        }
    }

    fun loadRemoteData()
    {
        gdsRepository.loadCCData(
            ccID.value,
            countryCode.value.toInt(),
            stateCode.value.toInt(),
            courseCount.value.toInt(),
            useAltitude = true,
            useHoleMap = true,
            useUndulationMap = true,
            useCache = false
        )
    }

    fun loadAssetData()
    {
        if(gdsRepository.loadCCAssetData(
                ccID.value,
                countryCode.value.toInt(),
                courseCount.value.toInt(),
                useAltitude = true,
                useHoleMap = true,
                useUndulationMap = true,
            )
        ) {
            Timber.d("loadAssetData success")
            altitudeData.postValue(gdsRepository.getAltitude(ccID.value, latitude.value.toDouble(), longitude.value.toDouble()))
            holeMapData.postValue(gdsRepository.getHoleMap(ccID.value, courseNum.value.toInt(), holeNum.value.toInt()))
            undulationMapData.postValue(gdsRepository.getUndulationMap(ccID.value, courseNum.value.toInt(), holeNum.value.toInt()))
        } else {
            Timber.d("loadAssetData failed")
        }
    }

    fun loadFileData()
    {
        if(gdsRepository.loadCCFileData(
            ccID.value,
            countryCode.value.toInt(),
            courseCount.value.toInt(),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "65706/"),
            useAltitude = true,
            useHoleMap = true,
            useUndulationMap = true,
        )) {
                Timber.d("loadFileData success")
                altitudeData.postValue(gdsRepository.getAltitude(ccID.value, latitude.value.toDouble(), longitude.value.toDouble()))
                holeMapData.postValue(gdsRepository.getHoleMap(ccID.value, courseNum.value.toInt(), holeNum.value.toInt()))
                undulationMapData.postValue(gdsRepository.getUndulationMap(ccID.value, courseNum.value.toInt(), holeNum.value.toInt()))
        } else {
            Timber.d("loadFileData failed")
        }
    }

    override fun onCCDataReady(ccID: String) {
        Timber.d("onCCDataReady")
        //Timber.d("altitude %d", gdsRepository.getAltitude("66011", 37.094768, 127.335132))
        //Timber.d("altitude %d", gdsRepository.getAltitude("66011", 37.095045, 127.334654))

        if(ccID == this@TestViewModel.ccID.value) {
            altitudeData.postValue(gdsRepository.getAltitude(ccID, latitude.value.toDouble(), longitude.value.toDouble()))
            holeMapData.postValue(gdsRepository.getHoleMap(ccID, courseNum.value.toInt(), holeNum.value.toInt()))
            undulationMapData.postValue(gdsRepository.getUndulationMap(ccID, courseNum.value.toInt(), holeNum.value.toInt()))
        }
    }

    override fun onCCDataFailed(ccID: String, error: Int) {
        Timber.d("onCCDataFailed")
    }

}