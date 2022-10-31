package com.golfzondeca.gds.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

fun ByteBuffer.getUByte() : Int {
    return this.get().toInt() and 0xFF
}

fun ByteBuffer.getUShort() : Int {
    return this.short.toInt() and 0xFFFF
}

fun ByteBuffer.getUInt() : Long {
    return this.int.toLong() and 0xFFFFFFFF
}

class AltitudeUtil {
    companion object
    {
        private const val D_VALUE = 100000000
        private const val WATER_SLOPE = 48676
        private const val WATER_SLOPE_8 = -9999

        fun getAltitude(countryCode: Int, altitudeFileData: ByteBuffer, latitude: Double, longitude: Double): Int? {
            altitudeFileData.order(ByteOrder.LITTLE_ENDIAN)

            return when(countryCode) {
                4 ->
                    getAltitudeJapan(altitudeFileData, latitude, longitude)
                1, 2, 9,
                7, 8, 12 ,15 ,23 ,29 ,30 ,36 ,41 ,
                59 ,60 ,61 ,62 ,71 ,75 ,76 ,83 ,84 ,
                90 ,97 ,98 ,100 ,102 ,104 ,105 ,113 ,
                115 ,116 ,117 ,119 ,125 ,132 ,133 ,141 ,
                149 ,158 ,159 ,162 ,163 ,170 ,176 ,177 ,
                181 ,184 ,186 ,187 ,197 ,202 ,207 ,215 ,
                222 ,223 ,224 ,228 ,233 ,234 ,251 ,252 ->
                    getAltitudeUSA(altitudeFileData, latitude, longitude)
                else ->
                    null
            }
        }

        private fun getAltitudeJapan(altitudeFileData: ByteBuffer, latitude: Double, longitude: Double): Int? {
            val x = (latitude * D_VALUE).toLong()
            val y = (longitude * D_VALUE).toLong()

            altitudeFileData.position(7)

            val unit = altitudeFileData.get()

            val startY = altitudeFileData.long
            val startX = altitudeFileData.long

            val unitY = altitudeFileData.int
            val unitX = altitudeFileData.int

            if((unitX != 5555 || unitY != 5555) &&
                (unitX != 11111 || unitY != 11111))
                return null

            val rowSize = altitudeFileData.short
            val colSize = altitudeFileData.short

            val xOffset = (startX - x).toInt()
            var col = xOffset / unitX

            if(abs(xOffset - (unitX * col)) > abs(xOffset - (unitX * (col + 1))))
                col++

            val yOffset = (y - startY).toInt()
            var row = yOffset / unitY

            if(abs(yOffset - (unitY * row)) > abs(yOffset - (unitY * (row + 1))))
                row++

            if(col > colSize || row > rowSize) return null

            val pos = (col * rowSize + row) * 2

            if(pos < 0 || pos + 2 > altitudeFileData.capacity()) return null

            altitudeFileData.position(pos + 7)

            var slope = altitudeFileData.short.toInt()

            if(unit < 8) {
                if(slope == WATER_SLOPE) {
                    return null
                } else {
                    slope = (slope / 100)
                }
            }
            else {
                if(slope == WATER_SLOPE_8) {
                    return null
                }
            }

            return slope
        }

        private fun getAltitudeUSA(altitudeFileData: ByteBuffer, latitude: Double, longitude: Double): Int? {
            val x = (latitude * D_VALUE).toLong()
            val y = (longitude * D_VALUE).toLong()

            altitudeFileData.position(7)

            val unit = altitudeFileData.get()

            val startY = altitudeFileData.long
            val startX = altitudeFileData.long

            val unitY = altitudeFileData.int
            val unitX = altitudeFileData.int

            //Timber.d("C1 $unit, $startY, $startX, $unitX, $unitY")

            if((unitX != 9259 || unitY != 9259) &&
                (unitX != 4630 || unitY != 4630) &&
                (unitX != 4545 || unitY != 4545) &&
                (unitX != 5000 || unitY != 5000))
                return null

            val rowSize = altitudeFileData.short
            val colSize = altitudeFileData.short

            val xOffset = (startX - x).toInt()
            var col = xOffset / unitX

            if(abs(xOffset - (unitX * col)) > abs(xOffset - (unitX * (col + 1))))
                col++

            val yOffset = (y - startY).toInt()
            var row = yOffset / unitY

            if(abs(yOffset - (unitY * row)) > abs(yOffset - (unitY * (row + 1))))
                row++

            if(col > colSize || row > rowSize) return null

            val pos = (col * rowSize + row) * 2

            if(pos < 0 || pos + 2 > altitudeFileData.capacity()) return null

            altitudeFileData.position(pos + 38)

            var slope = altitudeFileData.short.toInt()

            if(unit < 8) {
                if(slope == WATER_SLOPE) {
                    return null
                } else {
                    slope = (slope / 100)
                }
            }
            else {
                if(slope == WATER_SLOPE_8) {
                    return null
                }
            }

            return slope
        }
    }
}