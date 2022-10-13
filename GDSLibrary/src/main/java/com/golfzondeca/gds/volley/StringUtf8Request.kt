package com.golfzondeca.gds.volley

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

abstract class StringUtf8Request(
    url: String,
    errorListener: Response.ErrorListener,
    initialTimeoutMs: Int = 6000,
    maxNumRetries: Int = 5
) : BaseRequest<String>(url, errorListener, initialTimeoutMs, maxNumRetries) {

    override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
        return try {
            val utf8String = String(response.data, Charset.forName("UTF-8"))
            Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: UnsupportedEncodingException) {
            Response.error<String>(ParseError(e))
        } catch (e: Exception) {
            Response.error<String>(ParseError(e))
        }
    }

}