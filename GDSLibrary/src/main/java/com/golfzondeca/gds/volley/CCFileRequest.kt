package com.golfzondeca.gds.volley

import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

class CCFileRequest(
    url: String,
    private val ccID: String,
    private val downloadFolderPath: String,
    private val listener: Response.Listener<CCFileResponse>,
    errorListener: Response.ErrorListener,
    initialTimeoutMs: Int = 6000,
    maxNumRetries: Int = 5,
) : BaseRequest<ByteArray>(url, errorListener, initialTimeoutMs, maxNumRetries) {

    init {
        setShouldCache(false)
    }

    var responseHeaders: Map<String, String>? = null

    @Throws(AuthFailureError::class)
    override fun getParams(): Map<String, String>? {
        return params
    }

    override fun deliverResponse(response: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            var downloadFile = File(downloadFolderPath)

            kotlin.runCatching {
                do {
                    downloadFile = File(downloadFolderPath, getSHA256("${Random(Date().time)}"))
                } while (downloadFile.exists())
                File(downloadFolderPath).mkdirs()

                val outputStream: FileOutputStream = downloadFile.outputStream()
                outputStream.write(response)
                outputStream.close()
            }.onSuccess {
                listener.onResponse(CCFileResponse(ccID, downloadFile))
            }.onFailure {
                listener.onResponse(CCFileResponse(ccID, null))
            }

        }
    }

    private fun getSHA256(str: String): String {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(str.toByteArray())
            val hash = md.digest()
            return String.format("%64x", BigInteger(1, hash))
        }
        catch (e: CloneNotSupportedException) {

        }
        return ""
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray> {
        //Initialise local responseHeaders map with response headers received
        responseHeaders = response.headers

        //Pass the response data here
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response))
    }

}