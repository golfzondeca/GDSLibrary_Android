package com.golfzondeca.gds.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HoleFileUtil {
    companion object {
        fun getMapBitmap(mapFileData: ByteBuffer, holeNum : Int): Bitmap? {
            mapFileData.order(ByteOrder.LITTLE_ENDIAN)
            mapFileData.position(4)

            val header1Len = mapFileData.int

            val infoOffset = holeNum * 12

            if(infoOffset <= header1Len + 12) {

                mapFileData.position(infoOffset)

                Timber.d("infoOffset $infoOffset")

                val dataIndex = mapFileData.int

                Timber.d("dataIndex $dataIndex")

                return if(dataIndex > 0) {
                    val dataOffset = mapFileData.int
                    val dataLength = mapFileData.int

                    Timber.d("dataOffset $dataOffset")

                    Timber.d("dataLength $dataLength")

                    if(dataLength > 0) {
                        BitmapFactory.decodeByteArray(
                            mapFileData.array(),
                            dataOffset + 12,
                            dataLength
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                return null
            }
        }
    }
}