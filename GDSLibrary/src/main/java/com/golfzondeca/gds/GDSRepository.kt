package com.golfzondeca.gds

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.golfzondeca.gds.data.realm.CCFileInfo
import com.golfzondeca.gds.data.realm.HoleMap
import com.golfzondeca.gds.data.realm.UndulationMap
import com.golfzondeca.gds.util.AltitudeUtil
import com.golfzondeca.gds.util.HoleFileUtil
import com.golfzondeca.gds.util.UndulationFileUtil
import com.golfzondeca.gds.volley.*
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("MissingPermission")
class GDSRepository (
    private val context: Context,
    private val gdsID: String,
) {

    companion object {
        // 그린 파일
        private const val URL_UPDATE_UNDULATION_MAP_DEC =
            //"w10/ccid_green/L1/ccid/"
            "dzEwL2NjaWRfZ3JlZW4vTDEvY2NpZC8="

        // 맵 파일
        private const val URL_UPDATE_HOLE_MAP_DEC =
            //"w10/dec/"
            "dzEwL2RlYy8="

        // 고도 파일 (.bin)
        private const val URL_UPDATE_BIN =
            //"slope/slope_mobile/"
            "c2xvcGUvc2xvcGVfbW9iaWxlLw=="

        // 공통 서버 path
        private const val URL_PREFIX_4 =
            //"toUpdate/"
            "dG9VcGRhdGUv"

        private const val URL_PREFIX_2 =
            //"front.net/G3M"
            "ZnJvbnQubmV0L0czTQ=="

        private const val URL_PREFIX_A =
            //"anagerControl/Au"
            "YW5hZ2VyQ29udHJvbC9BdQ=="

        private const val URL_PREFIX_1 =
            //"d1iaurndxudtwi.cloud"
            "ZDFpYXVybmR4dWR0d2kuY2xvdWQ="

        private const val URL_PREFIX_0 =
            //"https://"
            "aHR0cHM6Ly8="

        private const val URL_PREFIX_B =
            //"toMap/"
            "dG9NYXAv"

        private const val URL_PREFIX_3 =
            //"anager/Au"
            "YW5hZ2VyL0F1"

        private const val URL_DOWNLOAD =
            //"gds.golfbuddy.com/cffile/download.asp"
            "Z2RzLmdvbGZidWRkeS5jb20vY2ZmaWxlL2Rvd25sb2FkLmFzcA=="

        const val ERROR_NETWORK = 1
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

    private fun getBase64decode(content: String): String {
        return String(Base64.decode(content, Base64.NO_WRAP))
    }

    private val config =
        RealmConfiguration
            .Builder(schema = setOf(CCFileInfo::class, HoleMap::class, UndulationMap::class))
            .schemaVersion(1L)
            .encryptionKey(getSHA512("DECA_$gdsID"))
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

    private val undulationMapQueue by lazy { Volley.newRequestQueue(context) }
    private val holeMapQueue by lazy { Volley.newRequestQueue(context) }
    private val binQueue by lazy { Volley.newRequestQueue(context) }

    private val countDownContext = newSingleThreadContext("CountDown")

    private class CCRequestStatus (
        val ccID: String,
        var binFileCheck: Boolean,
    ) {
        val holeMapCheckMap: MutableMap<Int, Boolean> = mutableMapOf()
        val undulationMapCheckMap: MutableMap<Int, Boolean> = mutableMapOf()
    }

    private val ccRequestStatusQueue = ConcurrentLinkedQueue<CCRequestStatus>()
    private val mutex = Mutex()

    private class CCData (
        var countryCode: Int,
        var binFileData: ByteBuffer? = null,
    ) {
        val holeMapFileDatas = mutableMapOf<Int, ByteBuffer>()
        val undulationMapFileDatas = mutableMapOf<Int, ByteBuffer>()
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

    fun getHoleMap(
        ccID: String,
        courseNum: Int,
        holeNum: Int,
    ): Bitmap? {
        if(courseNum < 1 || holeNum < 1) return null

        ccDataMap[ccID]?.holeMapFileDatas?.get(courseNum)?.let {
            return HoleFileUtil.getMapBitmap(it, holeNum)
        } ?: run {
            return null
        }
    }

    fun getUndulationMap(
        ccID: String,
        courseNum: Int,
        holeNum: Int,
    ): ArrayList<Bitmap>? {
        if(courseNum < 1 || holeNum < 1) return null

        ccDataMap[ccID]?.undulationMapFileDatas?.get(courseNum)?.let {
            return UndulationFileUtil.getUndulationBitmap(it, holeNum)
        } ?: run {
            return null
        }
    }

    /*fun loadCCData(
        ccID: String,
        courseCount: Int,
        useAltitude: Boolean,
        useHoleMap: Boolean,
        useUndulationMap: Boolean,
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
    }*/

    private fun getSHA256(str: String): String {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(str.toByteArray())
            val hash = md.digest()
            return String.format("%64X", BigInteger(1, hash))
        }
        catch (e: CloneNotSupportedException) {

        }
        return ""
    }

    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat("yyyyMMdd").also {
        it.timeZone = TimeZone.getTimeZone("GMT")
    }

    fun loadCCData(
        ccID: String,
        countryCode: Int,
        stateCode: Int,
        courseCount: Int,
        useAltitude: Boolean,
        useHoleMap: Boolean,
        useUndulationMap: Boolean,
    ) {
        val headers = mutableMapOf<String, String>()

        headers["BpId"] = gdsID
        headers["User-Agent"] = "Dalvik/2.1.0 (Linux; U; Android 12) GOLFBUDDY"
        headers["Auth"] = getSHA256(dateFormat.format(Date()))

        Timber.d(headers.toString())

        var isNewCCAltitudeData = false
        var isNewCCHoleMapData = false
        var isNewCCUndulationMapData = false

        val downloadPath = "${context.dataDir}/map"

        val ccFileInfo = realm.writeBlocking {
            var ccFileInfo =
                query<CCFileInfo>("ccID == $0", ccID).first().find()

            if(ccFileInfo == null) {
                isNewCCAltitudeData = true
                isNewCCHoleMapData = true
                isNewCCUndulationMapData = true
                ccFileInfo = copyToRealm(CCFileInfo().apply { this.ccID = ccID })
            }
            else {
                if(useAltitude && ccFileInfo.altitude.isBlank()) isNewCCAltitudeData = true
                if(useHoleMap && ccFileInfo.holeMaps.size < 1) isNewCCHoleMapData = true
                if(useUndulationMap && ccFileInfo.undulationMaps.size < 1) isNewCCUndulationMapData = true
            }

            ccFileInfo.countryCode = countryCode
            ccFileInfo.stateCode = stateCode
            ccFileInfo.downloadDate = RealmInstant.from(Date().time / 1000, 0)

            ccFileInfo
        }

        if(!ccDataMap.containsKey(ccID)) {
            ccDataMap[ccID] = CCData(countryCode)
        }

        ccRequestStatusQueue.offer(
            CCRequestStatus(
                ccID,
                !isNewCCAltitudeData,
            ).apply {
                repeat(courseCount) {
                    if(isNewCCHoleMapData) holeMapCheckMap[it + 1] = false
                    if(isNewCCUndulationMapData) undulationMapCheckMap[it + 1] = false
                }
            }
        )

        if(isNewCCAltitudeData) {
            val locationPath =
                when (countryCode) {
                    1 -> "L${countryCode}_v2" // 한국
                    2 -> "L${countryCode}_${stateCode}" // 미국
                    else -> "L${countryCode}"
                }

            val binUrl = Uri
                .parse("${getBase64decode(URL_PREFIX_0)}${getBase64decode(URL_DOWNLOAD)}")
                .buildUpon()
                .appendQueryParameter("Type", "bin")
                .appendQueryParameter("CCID", ccID)
                .build().toString()

            Timber.d("url : $binUrl")

            val binRequest = AltitudeFileRequest(
                binUrl,
                headers,
                ccID,
                downloadPath,
                binListener,
                binErrorListener,
                10000,
                0
            )

            binQueue.add(binRequest)
        } else {
            val file = File(downloadPath, ccFileInfo.altitude)

            val binFileData = ByteArray(file.length().toInt())

            FileInputStream(file).let {
                it.read(binFileData)
                it.close()
            }

            ccDataMap[ccID]!!.binFileData = ByteBuffer.wrap(binFileData)
        }

        if(isNewCCHoleMapData) {
            for (courseNum in 1..courseCount) {
                val holeMapUrl = Uri
                    .parse("${getBase64decode(URL_PREFIX_0)}${getBase64decode(URL_DOWNLOAD)}")
                    .buildUpon()
                    .appendQueryParameter("Type", "hole")
                    .appendQueryParameter("CSID", String.format("%s%02d", ccID, courseNum))
                    .build().toString()

                Timber.d("url : $holeMapUrl")

                val holeMapRequest = HoleFileRequest(
                    holeMapUrl,
                    headers,
                    ccID,
                    courseNum,
                    downloadPath,
                    holeMapListener,
                    holeMapErrorListener,
                    10000,
                    0
                )

                holeMapQueue.add(holeMapRequest)
            }
        } else {
            ccFileInfo.holeMaps.forEach {
                val file = File(downloadPath, it.file)

                val mapFileData = ByteArray(file.length().toInt())

                FileInputStream(file).let {
                    it.read(mapFileData)
                    it.close()
                }

                ccDataMap[ccID]!!.holeMapFileDatas[it.courseNum] = ByteBuffer.wrap(mapFileData)
            }
        }

        if(isNewCCUndulationMapData) {
            for (courseNum in 1..courseCount) {
                val holeMapUrl = Uri
                    .parse("${getBase64decode(URL_PREFIX_0)}${getBase64decode(URL_DOWNLOAD)}")
                    .buildUpon()
                    .appendQueryParameter("Type", "green")
                    .appendQueryParameter("CSID", String.format("%s%02d", ccID, courseNum))
                    .build().toString()

                Timber.d("url : $holeMapUrl")

                val holeMapRequest = UndulationFileRequest(
                    holeMapUrl,
                    headers,
                    ccID,
                    courseNum,
                    downloadPath,
                    undulationMapListener,
                    undulationMapErrorListener,
                    10000,
                    0
                )

                undulationMapQueue.add(holeMapRequest)
            }
        } else {
            ccFileInfo.undulationMaps.forEach {
                val file = File(downloadPath, it.file)

                val mapFileData = ByteArray(file.length().toInt())

                FileInputStream(file).let {
                    it.read(mapFileData)
                    it.close()
                }

                ccDataMap[ccID]!!.undulationMapFileDatas[it.courseNum] = ByteBuffer.wrap(mapFileData)
            }
        }

        if(!isNewCCAltitudeData && !isNewCCHoleMapData && !isNewCCUndulationMapData) {
            broadcastReady()
        }
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

    /*private val searchListener = Response.Listener<CCSearchResponse> {
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
                    val holeMapUrl = Uri
                        .parse(URL_UPDATE_HOLE_MAP_DEC)
                        .buildUpon()
                        .appendPath("${ccID}_${courseNum}.dec")
                        .build().toString()

                    Timber.d("url : $holeMapUrl")

                    val holeMapRequest = DecFileRequest(
                        holeMapUrl,
                        ccID,
                        courseNum,
                        downloadPath,
                        holeMapListener,
                        holeMapErrorListener,
                        10000,
                        0
                    )

                    holeMapQueue.add(holeMapRequest)
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
    }*/

    private val holeMapListener = Response.Listener<HoleFileResponse> {
        Timber.d("holeMapListener ${it.downloadFile?.length()}")
        it.downloadFile?.let { file ->
            realm.writeBlocking {
                query<CCFileInfo>("ccID == $0", it.ccID).first().find()?.holeMaps?.let { list ->
                    list.firstOrNull{ holeMap -> holeMap.courseNum == it.courseNum }?.let {
                        delete(it)
                    }

                    list.add(copyToRealm(HoleMap().apply {
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

            ccData!!.holeMapFileDatas[it.courseNum] = ByteBuffer.wrap(decFileData)

            CoroutineScope(countDownContext).launch {
                ccRequestStatusQueue.peek()?.let { status ->
                    status.holeMapCheckMap[it.courseNum] = true

                    Timber.d("hole")
                    if (
                        status.ccID == it.ccID &&
                        status.binFileCheck &&
                        status.holeMapCheckMap.all { it.value } &&
                        status.undulationMapCheckMap.all { it.value }
                    ) {
                        broadcastReady()
                    }
                }
            }

        } ?: run {
            CoroutineScope(countDownContext).launch {
                ccRequestStatusQueue.peek()?.let { status ->
                    status.holeMapCheckMap[it.courseNum] = true

                    if (
                        status.ccID == it.ccID &&
                        status.binFileCheck &&
                        status.holeMapCheckMap.all { it.value } &&
                        status.undulationMapCheckMap.all { it.value }
                    ) {
                        broadcastError(ERROR_DOWNLOAD)
                    }
                }
            }
        }
    }

    private val undulationMapListener = Response.Listener<UndulationFileResponse> {
        Timber.d("undulationMapListener ${it.downloadFile?.length()}")
        it.downloadFile?.let { file ->
            realm.writeBlocking {
                query<CCFileInfo>("ccID == $0", it.ccID).first().find()?.undulationMaps?.let { list ->
                    list.firstOrNull{ undulationMap -> undulationMap.courseNum == it.courseNum }?.let {
                        delete(it)
                    }

                    list.add(copyToRealm(UndulationMap().apply {
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

            ccData!!.undulationMapFileDatas[it.courseNum] = ByteBuffer.wrap(decFileData)

            CoroutineScope(countDownContext).launch {
                ccRequestStatusQueue.peek()?.let { status ->
                    status.undulationMapCheckMap[it.courseNum] = true

                    Timber.d("undulation")
                    if (
                        status.ccID == it.ccID &&
                        status.binFileCheck &&
                        status.holeMapCheckMap.all { it.value } &&
                        status.undulationMapCheckMap.all { it.value }
                    ) {
                        broadcastReady()
                    }
                }
            }

        } ?: run {
            CoroutineScope(countDownContext).launch {
                ccRequestStatusQueue.peek()?.let { status ->
                    status.undulationMapCheckMap[it.courseNum] = true

                    if (
                        status.ccID == it.ccID &&
                        status.binFileCheck &&
                        status.holeMapCheckMap.all { it.value } &&
                        status.undulationMapCheckMap.all { it.value }
                    ) {
                        broadcastError(ERROR_DOWNLOAD)
                    }
                }
            }
        }
    }

    private val binListener = Response.Listener<AltitudeFileResponse> {
        Timber.d("binListener  ${it.downloadFile?.length()}")
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

            CoroutineScope(countDownContext).launch {
                ccRequestStatusQueue.peek()?.let { status ->
                    status.binFileCheck = true

                    Timber.d("bin")
                    if (
                        status.ccID == it.ccID &&
                        status.binFileCheck &&
                        status.holeMapCheckMap.all { it.value } &&
                        status.undulationMapCheckMap.all { it.value }
                    ) {
                        broadcastReady()
                    }
                }
            }
        } ?: run {
            CoroutineScope(countDownContext).launch {
                ccRequestStatusQueue.peek()?.let { status ->
                    status.binFileCheck = true

                    if (
                        status.ccID == it.ccID &&
                        status.binFileCheck &&
                        status.holeMapCheckMap.all { it.value } &&
                        status.undulationMapCheckMap.all { it.value }
                    ) {
                        broadcastError(ERROR_DOWNLOAD)
                    }
                }
            }
        }
    }

    private val undulationMapErrorListener = Response.ErrorListener {
        CoroutineScope(countDownContext).launch {
            ccRequestStatusQueue.peek()?.let { status ->
                status.undulationMapCheckMap.clear()
                if (
                    status.binFileCheck &&
                    status.holeMapCheckMap.all { it.value } &&
                    status.undulationMapCheckMap.all { it.value }
                ) {
                    if (it.networkResponse.statusCode in arrayOf(403, 404)) {
                        broadcastError(ERROR_DOWNLOAD)
                    } else {
                        broadcastError(ERROR_NETWORK)
                    }
                }
            }
        }
    }

    private val holeMapErrorListener = Response.ErrorListener {
        CoroutineScope(countDownContext).launch {
            ccRequestStatusQueue.peek()?.let { status ->
                status.holeMapCheckMap.clear()
                if (
                    status.binFileCheck &&
                    status.holeMapCheckMap.all { it.value } &&
                    status.undulationMapCheckMap.all { it.value }
                ) {
                    if (it.networkResponse.statusCode in arrayOf(403, 404)) {
                        broadcastError(ERROR_DOWNLOAD)
                    } else {
                        broadcastError(ERROR_NETWORK)
                    }
                }
            }
        }
    }

    private val binErrorListener = Response.ErrorListener {
        CoroutineScope(countDownContext).launch {
            ccRequestStatusQueue.peek()?.let { status ->
                status.binFileCheck = true
                if (
                    status.binFileCheck &&
                    status.holeMapCheckMap.all { it.value } &&
                    status.undulationMapCheckMap.all { it.value }
                ) {
                    if (it.networkResponse.statusCode in arrayOf(403, 404)) {
                        broadcastError(ERROR_DOWNLOAD)
                    } else {
                        broadcastError(ERROR_NETWORK)
                    }
                }
            }
        }
    }
}