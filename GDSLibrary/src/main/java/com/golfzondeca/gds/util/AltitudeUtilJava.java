package com.golfzondeca.gds.util;

import android.util.Log;

import timber.log.Timber;

public class AltitudeUtilJava {

    private static long D_VALUE = 100000000;
    private static int WATER_SLOPE = 48676;
    private static int WATER_SLOPE_8 = -9999;//0xFFFF;
    public static int NO_SLOPE_DATA = -9999;//-0xFFFF;

    public static int getAltitudeDataUSA(byte[] altitudeBuffer,double lat,double lng)
    {
        long x = (long)(lat * D_VALUE);
        long y = (long)(lng * D_VALUE);

        int offset = 7; // Unit 단위 저장 위치: 2m or 4m
        int unit = altitudeBuffer[offset++];

        long startY = (((long)altitudeBuffer[offset++] & 0xFF) | (((long)altitudeBuffer[offset++] & 0xFF) << 8) | (((long)altitudeBuffer[offset++] & 0xFF) << 16) | (((long)altitudeBuffer[offset++] & 0xFF) << 24)
                | (((long)altitudeBuffer[offset++] & 0xFF) << 32) | (((long)altitudeBuffer[offset++] & 0xFF) << 40) | (((long)altitudeBuffer[offset++] & 0xFF) << 48) | (((long)altitudeBuffer[offset++] & 0xFF) << 56));

        long startX = (((long)altitudeBuffer[offset++] & 0xFF) | (((long)altitudeBuffer[offset++] & 0xFF) << 8) | (((long)altitudeBuffer[offset++] & 0xFF) << 16) | (((long)altitudeBuffer[offset++] & 0xFF) << 24)
                | (((long)altitudeBuffer[offset++] & 0xFF) << 32) | (((long)altitudeBuffer[offset++] & 0xFF) << 40) | (((long)altitudeBuffer[offset++] & 0xFF) << 48) | (((long)altitudeBuffer[offset++] & 0xFF) << 56));

        int unitY = ((altitudeBuffer[offset++] & 0xFF) | ((altitudeBuffer[offset++] & 0xFF) << 8) | ((altitudeBuffer[offset++] & 0xFF) << 16) | ((altitudeBuffer[offset++] & 0xFF) << 24));
        int unitX = ((altitudeBuffer[offset++] & 0xFF) | ((altitudeBuffer[offset++] & 0xFF) << 8) | ((altitudeBuffer[offset++] & 0xFF) << 16) | ((altitudeBuffer[offset++] & 0xFF) << 24));

        int rowSize = ((altitudeBuffer[offset++] & 0xFF) | ((altitudeBuffer[offset++] & 0xFF) << 8));
        int colSize = ((altitudeBuffer[offset++] & 0xFF) | ((altitudeBuffer[offset++] & 0xFF) << 8));

        int col = (int)((startX - x) / unitX);
        int x_offset = (int)((startX - x));
        if(Math.abs(x_offset - (unitX * col)) > Math.abs(x_offset - (unitX * (col + 1))))
            col++;

        int row = (int)((y - startY) / unitY);
        int y_offset= (int)(y - (startY));
        if(Math.abs(y_offset- (unitY * row)) > Math.abs(y_offset- (unitY * (row + 1))))
            row++;

        int pos = (col * (rowSize) + row) * 2;
        //9250,japan/ 4630mus, 4545, eu
        if(((unitX != 9259) || (unitY != 9259)) && ((unitX != 4630) || (unitY != 4630)) && ((unitX != 4545) || (unitY != 4545)) && ((unitX != 5000) || (unitY != 5000))){
            return NO_SLOPE_DATA;
        }

        if((col > colSize) || (row > rowSize)){
            return NO_SLOPE_DATA;
        }

        if(pos < 0 || ((pos + 2) > altitudeBuffer.length)) {
            return NO_SLOPE_DATA;
        }

//        int slope = ((altitudeBuffer[pos + offset++] & 0xFF) | ((altitudeBuffer[pos + offset++] & 0xFF) << 8));
//        if(unit < 8){
//            if(slope == WATER_SLOPE){
//                return -1;
//            }
//            else{
//                slope = slope / 100;
//            }
//        }
//        else{
//            if(slope == WATER_SLOPE_8){
//                return -1;
//            }
//        }

        short slope = (short)((altitudeBuffer[pos + offset++] & 0xFF) | ((altitudeBuffer[pos + offset++] & 0xFF) << 8));
        if(unit < 8){
            if(slope == WATER_SLOPE){
                return NO_SLOPE_DATA;
            }
            else{
                slope = (short)(slope / 100);
            }
        }
        else{
            if(slope == WATER_SLOPE_8){
                return NO_SLOPE_DATA;
            }
        }
        return slope;
    }
}
