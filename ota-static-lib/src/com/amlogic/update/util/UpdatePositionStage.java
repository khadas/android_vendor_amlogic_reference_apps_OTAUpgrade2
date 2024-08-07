package com.amlogic.update.util;
/******************************************************************
*
*Copyright (C) 2012 Amlogic, Inc.
*
*Licensed under the Apache License, Version 2.0 (the "License");
*you may not use this file except in compliance with the License.
*You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing, software
*distributed under the License is distributed on an "AS IS" BASIS,
*WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*See the License for the specific language governing permissions and
*limitations under the License.
******************************************************************/
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.util.Log;

public class UpdatePositionStage {
    //for <n use data;
    public static final String KK_MEDIA="/cache";
    //private static final String N_MEDIA="/storage/emulated/0";
    private static final String N_MEDIA="/data/droidota";
        private static final String Q_MEDIA="/data/ota_package/";
    private static final String O_MEDIA="/data/cache";
    private static final String N_MEDIA_RECOVERY="/data/media/0";
    public final static int M400ZIE = 460 * 1024 * 1024;
    static public boolean isBigFile(File file){
        if (file == null || !file.exists()) {
            return false;
        } else {
            try {
                FileInputStream fin=new FileInputStream(file);
                long value = fin.available();
                fin.close();
                return value>M400ZIE;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        }
    }
    static public String getSizeDecidePosition(long size) {
        String sdkStr = UpgradeInfo.getString("ro.build.version.sdk");
        int sdkInt = Integer.parseInt(sdkStr);
        if (sdkInt < 29) return getDefaultPostion();
        File file = new File(KK_MEDIA);
        Log.d("OTA","getSizeDecidePosition: cache file freespace"+file.getFreeSpace()+" need size"+size);
        if (file.getFreeSpace()-size > 50*1024*1024) {
            return KK_MEDIA;
        }else {
            return Q_MEDIA;
        }
    }
    static public String getDefaultPostion(){
        String sdkStr = UpgradeInfo.getString("ro.build.version.sdk");
        int sdkInt = Integer.parseInt(sdkStr);
        if (sdkInt >= 29) { return Q_MEDIA;
        }else if (sdkInt >= 25 && sdkInt < 29 ) return O_MEDIA;
        else if (sdkInt >= 24/*for N*/)
            return N_MEDIA;
        else return KK_MEDIA;
    }
}
