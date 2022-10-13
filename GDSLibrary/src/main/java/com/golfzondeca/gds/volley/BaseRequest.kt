package com.golfzondeca.gds.volley

import com.android.volley.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Volley의 기본 StringRequest는 UTF-8 charset을 지원하지 않아
 * parseNetworkResponse를 override 하여 해당 기능을 구현함
 * 2022.03.23
 *
 * @author lookjoon
 *
 * @param url
 * @param listener
 * @param errorListener
 */

abstract class BaseRequest<T>(
    url: String,
    errorListener: Response.ErrorListener,
    initialTimeoutMs: Int = 6000,
    maxNumRetries: Int = 5,
) : Request<T>(Method.GET, url, errorListener) {

    companion object {
        private var sharedMessageNumber = AtomicInteger(0)
    }

    val requestUrl = url
    val messageNumber = (sharedMessageNumber.getAndIncrement()) % 256

    init {
        retryPolicy = DefaultRetryPolicy(
            initialTimeoutMs,
            maxNumRetries,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
    }

    abstract override fun deliverResponse(response: T)
    abstract override fun parseNetworkResponse(response: NetworkResponse): Response<T>
}