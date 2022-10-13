package com.golfzondeca.gds.volley

import com.android.volley.Response
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister

class CCSearchRequest(
    url: String,
    private val listener: Response.Listener<CCSearchResponse>,
    errorListener: Response.ErrorListener,
    initialTimeoutMs: Int = 6000,
    maxNumRetries: Int = 5,
) : StringUtf8Request(url, errorListener, initialTimeoutMs, maxNumRetries) {

    companion object {
        private val serializer: Serializer = Persister()
    }

    override fun deliverResponse(response: String) {
        try {
            val document = serializer.read(CCSearchResponse::class.java, response)
            listener.onResponse(document)
        }
        catch (e: Exception) {

        }
    }
}