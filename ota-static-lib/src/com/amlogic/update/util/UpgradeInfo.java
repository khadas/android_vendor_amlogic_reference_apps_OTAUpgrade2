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
package com.amlogic.update.util;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.net.InterfaceConfiguration;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

/**
 * @ClassName UpgradeInfo
 * @Description TODO
 * @Date 2013-7-15
 * @Email
 * @Author
 * @Version V1.0
 */
public class UpgradeInfo {
    /*
     * 1---ro.product.otaupdateurl should be defined for different server target
     * url ex:http://10.18.19.10:8080/otaupdate/update 1---ro.product.firmware
     * should auto-increasing according to specified specification
     */
    static final String UNKNOWN = "unknown";
    public static String       postUrl;                     // ro.product.otaupdateurl
    public static String       updating_apk_version;        // = "1";
    public static String       brand;                       // = "MID";
    public static String       device;                      // = "g24ref";
    public static String       board;                       // = "g24ref";
    public static String       mac;                         // =
                                                            // "00.11.22.33.44.55";
    public static String       firmware /* = "00421001" */;
    public static String       android;                     // = "4.2.1";
    public static String       time;                        // =
                                                            // "20120301.092251";
    public static String       builder;
    public static String       fingerprint;
    public static String       country;
    private Context            mContext;

    public UpgradeInfo(Context mContext) {
        this.mContext = mContext;
        onInit();
    }

    private void getcountry() {
        try {
            country = ActivityManagerNative.getDefault().getConfiguration().locale
                    .getCountry();
        } catch (RemoteException e) {
        }
    }

    static String makePostString() {
        return null;
    }

    public static String getString(String property) {
        return SystemProperties.get(property, UNKNOWN);
    }
    public static boolean isDebugAble(){
       String debug = getString("rw.update.debug");
       if (debug.equals(true)) {
           return true;
       }
       return false;
    }
    private String getMacAddr() {
        IBinder b = ServiceManager
                .getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService networkManagement = INetworkManagementService.Stub
                .asInterface(b);
        if (networkManagement != null) {
            InterfaceConfiguration iconfig = null;
            try {
                iconfig = networkManagement.getInterfaceConfig("eth0");
            } catch (Exception e) {
                // e.printStackTrace();
            } finally {
                return "";
            }
        } else {
            return "";
        }
    }

    private String getVersionCode() {
        String packageName = mContext.getPackageName();
        int versionCode = 0;
        try {
            versionCode = mContext.getPackageManager().getPackageInfo(
                    packageName, 0).versionCode;
        } catch (Exception e) {
        }
        return String.valueOf(versionCode);
    }

    private void onInit() {
        //Log.d("OTA","onInit()"+Log.getStackTraceString(new Throwable()));
        if (postUrl != null) return;
        getcountry();
        updating_apk_version = getVersionCode();
        brand = getString("ro.product.brand");
        device = getString("ro.product.device");
        board = getString("ro.product.board");
        mac = getMacAddr();
        postUrl = getString("ro.product.otaupdateurl");
        firmware = getString("ro.product.firmware");
        android = getString("ro.build.version.release");
        time = getString("ro.build.date.utc");
        builder = getString("ro.build.user");
        fingerprint = getString("ro.build.fingerprint");
    }
    public void setCustomValue(String version,String url){
        //Log.d("OTA","onInit()"+Log.getStackTraceString(new Throwable()));
        firmware = version;
        postUrl = url;
    }
}
