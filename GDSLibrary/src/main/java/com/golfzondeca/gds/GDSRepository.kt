package com.golfzondeca.gds

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.golfzondeca.gds.data.realm.CCFileInfo
import com.golfzondeca.gds.data.realm.Undulation
import com.golfzondeca.gds.util.AltitudeUtil
import com.golfzondeca.gds.volley.*
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
import timber.log.Timber
import java.io.FileInputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@SuppressLint("MissingPermission")
class GDSRepository (
    private val context: Context,
    private val id: String,
    private val password: String,
) {

    companion object {
        // 그린 맵 파일
        private const val URL_UPDATE_DEC = "https://d1iaurndxudtwi.cloudfront.net/G3Manager/AutoUpdate/w10/dec/"

        // 고도 파일 (.bin)
        private const val URL_UPDATE_BIN =
            "https://d1iaurndxudtwi.cloudfront.net/G3Manager/AutoUpdate/slope/slope_mobile/"

        // 고도 파일 (.bin)
        private const val URL_SEARCH_CC =
            "https://www.gpsgolfbuddy.com/app/cc/search.asp"

        const val ERROR_NETWORK = 1
        const val ERROR_CC_INFO = 2
        const val ERROR_DOWNLOAD = 3
    }

    interface Callback {
        fun onCCDataReady(
            ccID: String
        )

        fun onCCDataFailed(
            ccID: String,
            error: Int
        )
    }


    private val config =
        RealmConfiguration
            .Builder(schema = setOf(CCFileInfo::class, Undulation::class))
            .schemaVersion(1L)
            .encryptionKey(getSHA512("DECA_$id"))
            .name("gdcfi")
            .build()

    private fun getSHA512(str: String): ByteArray {
        try {
            val md = MessageDigest.getInstance("SHA-512")
            md.update(str.toByteArray())
            val hash = md.digest()

            Timber.e(String.format("%128X", BigInteger(1, hash)))
            return hash
        }
        catch (e: CloneNotSupportedException) {

        }
        return byteArrayOf()
    }

    private val realm = Realm.open(config)

    private val searchCCQueue by lazy { Volley.newRequestQueue(context) }
    private val decQueue by lazy { Volley.newRequestQueue(context) }
    private val binQueue by lazy { Volley.newRequestQueue(context) }

    private class CCRequestStatus (
        val ccID: String,
        var isInfoCheck: Boolean = false,
        var isDecCheck: Boolean = false,
        var isBinCheck: Boolean = false
    )

    private val ccRequestStatusQueue = ConcurrentLinkedQueue<CCRequestStatus>()

    private class CCData (
        var countryCode: String,
        var binFileData: ByteBuffer? = null,
    ) {
        val decFileDatas = mutableMapOf<Int, ByteBuffer>()
    }

    private val ccDataMap = mutableMapOf<String, CCData>()

    private val callbacks = mutableSetOf<Callback>()

    private val languageType by lazy {
        val country =
            context.resources.configuration.locales[0].language

        if(country == "ko") "1" else "2"
    }

    fun addCallback(callback: Callback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    fun getAltitude(
        ccID: String,
        latitude: Double,
        longitude: Double,
    ): Int? {
        ccDataMap[ccID]?.let {
            it.binFileData?.let { fileData ->
                return AltitudeUtil.getAltitude(
                    it.countryCode,
                    fileData,
                    latitude,
                    longitude
                )

            } ?: run {
                return null
            }
        } ?: run {
            return null
        }
    }

    fun getUndulationMap(
        ccID: String,
        courseNum: Int,
        holeNum: Int,
    ): Array<Bitmap>? {
        ccDataMap[ccID]?.decFileDatas?.get(courseNum)?.let {
            return null
        } ?: run {
            return null
        }
    }

    fun loadCCData(
        ccID: String,
    ) {
        val url = Uri
            .parse(URL_SEARCH_CC)
            .buildUpon()
            .appendQueryParameter("UserID", "YmFlaw==")
            .appendQueryParameter("SID", "V1g5RDBELTAxMDAtMDE2NQ==")
            .appendQueryParameter("Lang", languageType)
            .appendQueryParameter("Type", "search")
            .appendQueryParameter("SearchCCID", ccID)
            .build().toString()

        Timber.d("url : $url")

        val request = CCSearchRequest(
            url,
            searchListener,
            searchErrorListener,
            5000,
            0
        )

        ccRequestStatusQueue.offer(
            CCRequestStatus(ccID)
        )

        searchCCQueue.add(request)
    }

    private fun broadcastReady() {
        ccRequestStatusQueue.poll()?.let { status ->
            callbacks.forEach {
                it.onCCDataReady(status.ccID)
            }
        }
    }

    private fun broadcastError(errorCode: Int) {
        ccRequestStatusQueue.poll()?.let { status ->
            ccDataMap.remove(status.ccID)
            callbacks.forEach {
                it.onCCDataFailed(status.ccID, errorCode)
            }
        }
    }

    private val searchListener = Response.Listener<CCSearchResponse> {
        ccRequestStatusQueue.peek()?.isInfoCheck = true

        it?.let {
            if(it.searchCC.ccList.isNotEmpty()) {
                val searchCC = it.searchCC.ccList[0]

                val ccID = searchCC.ccid!!
                val courseCount = searchCC.csCount!!
                val country = searchCC.countryCode!!
                val state = searchCC.stateCode!!

                realm.writeBlocking {
                    val ccFileInfo =
                        query<CCFileInfo>("ccID == $0", ccID).first().find() ?:
                        copyToRealm(CCFileInfo().apply { this.ccID = ccID })

                    ccFileInfo.countryCode = country
                    ccFileInfo.stateCode = state
                    ccFileInfo.downloadDate = RealmInstant.from(Date().time / 1000, 0)
                }

                if(!ccDataMap.containsKey(ccID)) {
                    ccDataMap[ccID] = CCData(country)
                }

                val downloadPath = "${context.dataDir}/map"

                for(courseNum in 1..courseCount.toInt()) {
                    val decUrl = Uri
                        .parse(URL_UPDATE_DEC)
                        .buildUpon()
                        .appendPath("${ccID}_${courseNum}.dec")
                        .build().toString()

                    Timber.d("url : $decUrl")

                    val decRequest = UndulationFileRequest(
                        decUrl,
                        ccID,
                        courseNum,
                        downloadPath,
                        decListener,
                        decErrorListener,
                        10000,
                        0
                    )

                    decQueue.add(decRequest)
                }
                
                val locationPath =
                    when (country) {
                        "1" -> "L${country}_v2" // 한국
                        "2" -> "L${country}_${state}" // 미국
                        else -> "L${country}"
                    }

                val binUrl = Uri
                    .parse(URL_UPDATE_BIN)
                    .buildUpon()
                    .appendPath(locationPath)
                    .appendPath("${ccID}_cb_4M.bin")
                    .build().toString()

                Timber.d("url : $binUrl")

                val binRequest = AltitudeFileRequest(
                    binUrl,
                    ccID,
                    downloadPath,
                    binListener,
                    binErrorListener,
                    10000,
                    0
                )

                binQueue.add(binRequest)
            } else {
                broadcastError(ERROR_CC_INFO)
            }
        } ?: let {
            broadcastError(ERROR_CC_INFO)
        }
    }

    private val searchErrorListener = Response.ErrorListener {
        ccRequestStatusQueue.peek()?.isInfoCheck = true
        broadcastError(ERROR_NETWORK)
    }

    private val decListener = Response.Listener<UndulationFileResponse> {
        it.downloadFile?.let { file ->
            realm.writeBlocking {
                query<CCFileInfo>("ccID == $0", it.ccID).first().find()?.undulations?.let { list ->
                    list.firstOrNull{ undulation -> undulation.courseNum == it.courseNum }?.let {
                        delete(it)
                    }

                    list.add(copyToRealm(Undulation().apply {
                        this.courseNum = it.courseNum
                        this.file = file.name
                    }))
                }
            }

            val decFileData = ByteArray(file.length().toInt())

            FileInputStream(file).apply {
                read(decFileData)
                close()
            }

            val ccData = ccDataMap[it.ccID]

            ccData!!.decFileDatas[it.courseNum] = ByteBuffer.wrap(decFileData)

            ccRequestStatusQueue.peek()?.isDecCheck = true

            if(ccRequestStatusQueue.peek()?.isBinCheck == true) {
                broadcastReady()
            }

        } ?: run {
            if(ccRequestStatusQueue.peek()?.isBinCheck == true) {
                broadcastError(ERROR_DOWNLOAD)
            }
        }
    }

    private val binListener = Response.Listener<AltitudeFileResponse> {
        it.downloadFile?.let { file ->
            realm.writeBlocking {
                query<CCFileInfo>("ccID == $0", it.ccID).first().find()?.altitude = file.name
            }

            val binFileData = ByteArray(file.length().toInt())

            FileInputStream(file).let {
                it.read(binFileData)
                it.close()
            }

            val ccData = ccDataMap[it.ccID]

            ccData!!.binFileData = ByteBuffer.wrap(binFileData)

            ccRequestStatusQueue.peek()?.isBinCheck = true

            if(ccRequestStatusQueue.peek()?.isDecCheck == true) {
                broadcastReady()
            }
        } ?: run {
            if(ccRequestStatusQueue.peek()?.isDecCheck == true) {
                broadcastError(ERROR_DOWNLOAD)
            }
        }
    }

    private val decErrorListener = Response.ErrorListener {
        ccRequestStatusQueue.peek()?.isDecCheck = true

        if(it.networkResponse.statusCode in arrayOf(403, 404)) {
            if(ccRequestStatusQueue.peek()?.isBinCheck == true) {
                broadcastReady()
            }
        } else {
            if (ccRequestStatusQueue.peek()?.isBinCheck == true) {
                broadcastError(ERROR_NETWORK)
            }
        }
    }

    private val binErrorListener = Response.ErrorListener {
        ccRequestStatusQueue.peek()?.isBinCheck = true

        if(it.networkResponse.statusCode in arrayOf(403, 404)) {
            if(ccRequestStatusQueue.peek()?.isDecCheck == true) {
                broadcastReady()
            }
        } else {
            if (ccRequestStatusQueue.peek()?.isDecCheck == true) {
                broadcastError(ERROR_NETWORK)
            }
        }
    }
}