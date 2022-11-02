package com.golfzondeca.gds.volley

import java.io.File

open class MapFileResponse (
    val ccID: String,
    val courseNum: Int,
    val downloadFile: File?
)