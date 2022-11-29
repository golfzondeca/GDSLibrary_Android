package com.golfzondeca.gds.util

import android.location.Location
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
        private const val L_VALUE = 100000000
        private const val D_VALUE = 0.00000001
        private const val WATER_SLOPE = 48676
        private const val WATER_SLOPE_8 = -9999

        fun getAltitude(countryCode: Int, altitudeFileData: ByteBuffer, latitude: Double, longitude: Double): Int? {
            altitudeFileData.order(ByteOrder.LITTLE_ENDIAN)

            val x = (latitude * L_VALUE).toLong()
            val y = (longitude * L_VALUE).toLong()

            return when(countryCode) {
                4 ->
                    getAltitudeJapan(altitudeFileData, x, y)
                1, 2, 9,
                7, 8, 12 ,15 ,23 ,29 ,30 ,36 ,41 ,
                59 ,60 ,61 ,62 ,71 ,75 ,76 ,83 ,84 ,
                90 ,97 ,98 ,100 ,102 ,104 ,105 ,113 ,
                115 ,116 ,117 ,119 ,125 ,132 ,133 ,141 ,
                149 ,158 ,159 ,162 ,163 ,170 ,176 ,177 ,
                181 ,184 ,186 ,187 ,197 ,202 ,207 ,215 ,
                222 ,223 ,224 ,228 ,233 ,234 ,251 ,252 ->
                    getAltitudeUSA(altitudeFileData, x, y)
                else ->
                    null
            }
        }

        private fun getAltitudeJapan(altitudeFileData: ByteBuffer, x: Long, y: Long): Int? {
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

        private fun getAltitudeUSA(altitudeFileData: ByteBuffer, x: Long, y: Long): Int? {
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

        fun getAreaAltitudes(countryCode: Int, altitudeFileData: ByteBuffer, left: Double, top: Double, right: Double, bottom: Double): List<Triple<Double, Double, Int>>? {
            altitudeFileData.order(ByteOrder.LITTLE_ENDIAN)

            return when(countryCode) {
                4 ->
                    getAreaAltitudesJapan(altitudeFileData, left, top, right, bottom)
                1, 2, 9,
                7, 8, 12 ,15 ,23 ,29 ,30 ,36 ,41 ,
                59 ,60 ,61 ,62 ,71 ,75 ,76 ,83 ,84 ,
                90 ,97 ,98 ,100 ,102 ,104 ,105 ,113 ,
                115 ,116 ,117 ,119 ,125 ,132 ,133 ,141 ,
                149 ,158 ,159 ,162 ,163 ,170 ,176 ,177 ,
                181 ,184 ,186 ,187 ,197 ,202 ,207 ,215 ,
                222 ,223 ,224 ,228 ,233 ,234 ,251 ,252 ->
                    getAreaAltitudesUSA(altitudeFileData, left, top, right, bottom)
                else ->
                    null
            }
        }

        private fun getAreaAltitudesJapan(altitudeFileData: ByteBuffer, left: Double, top: Double, right: Double, bottom: Double): List<Triple<Double, Double, Int>> {
            val altitudeList = mutableListOf<Triple<Double, Double, Int>>()

            val leftX = (min(left, right) * L_VALUE).toLong()
            val topX = (min(top, bottom) * L_VALUE).toLong()

            val rightX = (max(left, right) * L_VALUE).toLong()
            val bottomX = (max(top, bottom) * L_VALUE).toLong()

            altitudeFileData.position(24)

            val unitY = altitudeFileData.int
            val unitX = altitudeFileData.int

            for(y in topX..bottomX step unitY.toLong()) {
                for (x in leftX..rightX step unitX.toLong()) {
                    getAltitudeJapan(altitudeFileData, x, y)?.let {
                        altitudeList.add(Triple(x * D_VALUE, y * D_VALUE, it))
                    }
                }
            }

            return altitudeList
        }

        private fun getAreaAltitudesUSA(altitudeFileData: ByteBuffer, left: Double, top: Double, right: Double, bottom: Double): List<Triple<Double, Double, Int>> {
            val altitudeList = mutableListOf<Triple<Double, Double, Int>>()

            val leftX = (min(left, right) * L_VALUE).toLong()
            val topX = (min(top, bottom) * L_VALUE).toLong()

            val rightX = (max(left, right) * L_VALUE).toLong()
            val bottomX = (max(top, bottom) * L_VALUE).toLong()

            altitudeFileData.position(24)

            val unitY = altitudeFileData.int
            val unitX = altitudeFileData.int

            for(y in topX..bottomX step unitY.toLong()) {
                for (x in leftX..rightX step unitX.toLong()) {
                    getAltitudeUSA(altitudeFileData, x, y)?.let {
                        altitudeList.add(Triple(x * D_VALUE, y * D_VALUE, it))
                    }
                }
            }

            return altitudeList
        }
    }
}