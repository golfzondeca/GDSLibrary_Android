package com.golfzondeca.gds.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UndulationFileUtil {
    companion object {
        fun getUndulationBitmap(mapFileData: ByteBuffer, holeNum : Int): ArrayList<Bitmap>? {
            mapFileData.order(ByteOrder.LITTLE_ENDIAN)
            mapFileData.position(4)

            val header1Len = mapFileData.int

            val infoOffset = 12 + (holeNum - 1) * 24

            if(infoOffset <= header1Len + 12) {

                mapFileData.position(infoOffset)

                Timber.d("infoOffset $infoOffset")

                var dataIndex = mapFileData.int

                Timber.d("dataIndex $dataIndex")

                val leftGreenBitmap = if(dataIndex > 0) {
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

                dataIndex = mapFileData.int

                val rightGreenBitmap = if(dataIndex > 0) {
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

                return if(leftGreenBitmap != null) {
                    arrayListOf<Bitmap>().apply {
                        add(leftGreenBitmap)
                        if(rightGreenBitmap != null) add(rightGreenBitmap)
                    }
                }
                else {
                    null
                }
            } else {
                return null
            }
        }
    }
}