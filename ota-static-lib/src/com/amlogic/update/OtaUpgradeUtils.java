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
package com.amlogic.update;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.Locale;

import com.amlogic.update.util.MD5;
import com.amlogic.update.util.PrefUtil;
import com.amlogic.update.util.UpdatePositionStage;
import com.amlogic.update.util.UpgradeInfo;

public class OtaUpgradeUtils {
    public static final int ERROR_INVALID_UPGRADE_PACKAGE = 0;
    public static final int ERROR_FILE_DOES_NOT_EXIT = 1;
    public static final int ERROR_FILE_IO_ERROR = 2;
    public static final int FAIL_REASON_BATTERY = 1;
    public static final int FAIL_STOP_FORCE = 2;
    public static final int FAIL_STOP_COPYERROR = 3;
    public static final File CHCHE_PARTITION_DIRECTORY = Environment.getDownloadCacheDirectory();
    private boolean mCacheFileFlag = true;
    public static final String DEFAULT_PACKAGE_NAME = "update.zip";
    public static final int UPDATE_REBOOT = 0;
    public static final int UPDATE_RECOVERY = 1;
    public static final int UPDATE_UPDATE = 2;
    public static final int UPDATE_OTA = 3;
        public static final int RECOVERY_UPDATE = 4;
    private static File RECOVERY_DIR = new File("/cache/recovery");
    public static final File UNCRYPT_PACKAGE_FILE = new File(RECOVERY_DIR, "uncrypt_file");
    public static final File BLOCK_MAP_FILE = new File(RECOVERY_DIR, "block.map");
    private static File COMMAND_FILE = new File(RECOVERY_DIR, "command");
    //private static File COMMAND_FILE_BK = new File(RECOVERY_DIR,"wipe_flag");
    private static File LOG_FILE = new File(RECOVERY_DIR, "log");
    private Context mContext;
    private boolean mDeleteSource = false;
    private boolean mReceiverBattery = false;
    private boolean mFocusStop = false;
    private PrefUtil util = null;

    /**
     * Constructs a OtaUpgradeUtils object,which will perform an update
     *
     * @param mContext
     */
    public OtaUpgradeUtils(Context context) {
        mContext = context;
        util = new PrefUtil(mContext);
    }

    public void setDeleteSource(boolean setDel) {
        mDeleteSource = setDel;
    }

    /**
     * support Battery monitor during Update process
     *
     */
    public void unregistBattery() {
        if (mReceiverBattery) {
            mReceiverBattery = false;
            mContext.unregisterReceiver(batteryReceiver);
        }
    }

    /**
     * support Battery monitor during Update process
     *
     */
    public void registerBattery() {
        if (!mReceiverBattery) {
            mReceiverBattery = true;
            mContext.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra("level", 0);
            int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
            if (level < 20 && status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                mFocusStop = true;
            } else {
                mFocusStop = false;
            }
        }
    };

    public interface ProgressListener extends RecoverySystem.ProgressListener {
        @Override
        public void onProgress(int progress);

        public void onVerifyFailed(int errorCode, Object object);

        public void onCopyProgress(int progress);

        public void onCopyFailed(int errorCode, Object object);

        public void onStopProgress(int reason);
    }

    /**
     * Execute OTA update Function
     *
     * @param packageFile
     *            is Upgrade file path, progressListener is update process
     */
    public boolean upgredeFromOta(File packageFile, ProgressListener progressListener) {
        return upgrade(packageFile, progressListener, UPDATE_OTA);
    }

    /**
     * Execute update Function
     *
     * @param packageFile
     *            is Upgrade file path, progressListener is update
     *            process,classfy is Update Category
     * @see UPDATE_REBOOT
     * @see UPDATE_OTA
     * @see UPDATE_RECOVERY
     * @see UPDATE_UPDATE
     */
    public boolean upgrade(File packageFile, ProgressListener progressListener, int classfy) {
        util.resetAll();

        if (mFocusStop) {
            progressListener.onStopProgress(FAIL_STOP_FORCE);
            return false;
        }
        if (PrefUtil.LOG_DEBUG) Log.d("OTA","upgrade"+classfy+"mFocusStop"+mFocusStop+"packageFile"+packageFile);
        //classfy = UPDATE_OTA;
        String sdkStr = UpgradeInfo.getString("ro.build.version.sdk");
            int sdkInt = Integer.parseInt(sdkStr);
            if (sdkInt >= 28) {
                classfy = UPDATE_OTA;
            }
        if (classfy == UPDATE_OTA && !mFocusStop) {
            try {
                if (sdkInt >= 24) {
                    String filePath = packageFile.getAbsolutePath();
                    File cacheFile = new File(UpdatePositionStage.KK_MEDIA,DEFAULT_PACKAGE_NAME);
                    if (cacheFile.exists()) {
                        cacheFile.delete();
                    }
                    long fileSize = packageFile.length() > 0?packageFile.length():getFileSize(packageFile);
                    if (PrefUtil.LOG_DEBUG) Log.d("OTA","fileSize:"+packageFile.length() +"-"+fileSize);
                    String  defaultPath = UpdatePositionStage.getSizeDecidePosition(fileSize);
                    if (PrefUtil.LOG_DEBUG) Log.d("OTA","filePath:"+filePath +"-"+defaultPath);
                    File updateFile;
                    if (!filePath.contains(defaultPath)) {
                        if (UpdatePositionStage.isBigFile(packageFile) || sdkInt >= 28 ) {
                            updateFile = new File(defaultPath, DEFAULT_PACKAGE_NAME);
                        } else {
                            updateFile = new File(CHCHE_PARTITION_DIRECTORY, DEFAULT_PACKAGE_NAME);
                        }
                        if (packageFile.getParent() == updateFile.getParent()) {
                            installPackage(mContext, new File(filePath));
                            return true;
                        }
                        boolean b = copyFile(packageFile, updateFile, progressListener);
                                                if (!b) {
                                                     updateFile = new File(CHCHE_PARTITION_DIRECTORY, DEFAULT_PACKAGE_NAME);
                                                     b = copyFile(packageFile, updateFile, progressListener);
                                                }
                        if (PrefUtil.LOG_DEBUG) Log.d("OTA","copyFile"+packageFile.getAbsolutePath()+"to"+updateFile.getAbsolutePath());
                        if (b && mDeleteSource) {
                            packageFile.delete();
                        }
                        filePath = updateFile.getCanonicalPath();
                    }
                    String cmd = "--update_package=" + filePath;
                    if (!changeCommand(cmd, "--update_package")) {
                        writeCommand("--update_package=" + filePath);
                    }
                    // bootCommand(mContext, UPDATE_RECOVERY);
                    installPackage(mContext, new File(filePath));
                    return true;
                } else {
                    File updateFile = new File(CHCHE_PARTITION_DIRECTORY, DEFAULT_PACKAGE_NAME);
                    boolean b = copyFile(packageFile, updateFile, progressListener);
                    if (PrefUtil.LOG_DEBUG) Log.d("OTA","copyFile"+packageFile.getAbsolutePath()+"->"+updateFile.getAbsolutePath());
                    if (b && mDeleteSource) {
                        packageFile.delete();
                    }
                    if (b) {
                        installPackage(mContext, updateFile);
                        return true;
                    }
                }
            } catch (IOException e) {
                progressListener.onStopProgress(FAIL_STOP_FORCE);
            }
        } else {
            try {
                if (!mFocusStop) {
                    //writeCommand("--update_package=" + filePath);
                    bootCommand(mContext, classfy);
                    return true;
                } else {
                    progressListener.onStopProgress(FAIL_STOP_FORCE);
                }
            } catch (IOException e) {
            }
        }
        return false;
    }

    private  static long getFileSize(File f) {
        long filesize = 2*1024*1024l;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                filesize = fis.available();
                fis.close();
                if (PrefUtil.LOG_DEBUG) Log.d("OTA", "fileSize"+filesize);
                return filesize;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return filesize;


    }
    private boolean updateWithBCB(File packageFile) {
        Log.d("OTA","enter updateWithBCB");
        RecoverySystem rs = (RecoverySystem) mContext.getSystemService("recovery");
        if (rs == null)
            return false;
        LOG_FILE.delete();

        // Must delete the file in case it was created by system server.
        UNCRYPT_PACKAGE_FILE.delete();

        String filename;
        try {
            filename = packageFile.getCanonicalPath();
            // If the package name ends with "_s.zip", it's a security update.
            boolean securityUpdate = filename.endsWith("_s.zip");

            // If the package is on the /data partition, the package needs to
            // be processed (i.e. uncrypt'd). The caller specifies if that has
            // been done in 'processed' parameter.
            if (filename.startsWith("/data/")) {
                FileWriter uncryptFile = new FileWriter(UNCRYPT_PACKAGE_FILE);
                try {
                    uncryptFile.write(filename + "\n");
                } finally {
                    uncryptFile.close();
                }
                // UNCRYPT_PACKAGE_FILE needs to be readable and writable
                // by system server.
                if (!UNCRYPT_PACKAGE_FILE.setReadable(true, false) || !UNCRYPT_PACKAGE_FILE.setWritable(true, false)) {
                    if (PrefUtil.LOG_DEBUG) Log.e("UPDATE", "Error setting permission for " + UNCRYPT_PACKAGE_FILE);
                }
                if (PrefUtil.LOG_DEBUG) Log.d("OTA","updateWithBCB");
                BLOCK_MAP_FILE.delete();

                // If the package is on the /data partition, use the block map
                // file as the package name instead.
                filename = "@/cache/recovery/block.map";
            }
        } catch (IOException e) {
            return false;
        }

        final String filenameArg = "--update_package=" + filename + "\n";
        final String localeArg = "--locale=" + Locale.getDefault().toLanguageTag() + "\n";
        String command = filenameArg + localeArg;
        //command +="--wipe_data --wipe_cache";
        Method setupBcb;
        try {
            Method[] mes= RecoverySystem.class.getDeclaredMethods();
            for (Method m:mes) {
                //Log.d("TAG","m:"+m);
                if (m.getName().contains("setupBcb")) {
                    m.setAccessible(true);
                    boolean obj = (boolean) m.invoke(rs, command);
                    if (!obj) {
                        return false;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        // Having set up the BCB (bootloader control block), go ahead and reboot
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        //String reason = "recovery-update,quiescent";
                String reason="recovery-update";
            try {
            changeCommand("", "--update_package");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        pm.reboot(reason);
        return true;
    }
    private boolean changeCommand(String args, String head) throws IOException {
        if (!COMMAND_FILE.exists())
            return false;
        else {
            String newcmd = "";
            FileReader fr = new FileReader(COMMAND_FILE);
            BufferedReader bf = new BufferedReader(fr);
            String cmd = "";
            while ((cmd = bf.readLine()) != null) {
                if (cmd.startsWith(head)) {
                    cmd = args;
                }
                newcmd += cmd;
            }
            bf.close();
            writeCommand(newcmd);
            return true;
        }

    }

    private void writeCommand(String... args) throws IOException {
        RECOVERY_DIR.mkdirs(); // In case we need it
        COMMAND_FILE.delete(); // In case it's not writable
        LOG_FILE.delete();

        FileWriter command = new FileWriter(COMMAND_FILE);
        try {
            for (String arg : args) {
                command.write(arg);
                command.write("\n");
            }
        } finally {
            command.close();
        }

    }

    /**
     * Execute boot command
     *
     * @param classfy
     *            is update Category
     * @see UPDATE_REBOOT
     * @see UPDATE_OTA
     * @see UPDATE_RECOVERY
     * @see UPDATE_UPDATE
     */
    public static void bootCommand(Context context, int classfy) throws IOException {
        if (PrefUtil.LOG_DEBUG) Log.d("OTA","bootCommand"+classfy);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (classfy == UPDATE_RECOVERY) {
            pm.reboot("recovery");
        }
        if (classfy == RECOVERY_UPDATE) {
            pm.reboot("recovery-update");
        }
        if (classfy == UPDATE_REBOOT) {
            pm.reboot("normal_reboot");
        } else if (classfy == UPDATE_UPDATE) {
            pm.reboot("update");
        }
        throw new IOException("Reboot failed (no permissions?)");
    }

    /**
     * Execute OTA update
     *
     * @param packagePath
     *            is update file path
     * @see UPDATE_OTA
     */
    public boolean upgradeFromOta(String packagePath, ProgressListener progressListener) {
        return upgredeFromOta(new File(packagePath), progressListener);
    }

    // public void deleteSource(boolean b) {}

    private boolean copyFile(File src, File dst, ProgressListener listener) {

        long inSize = src.length();
        long outSize = 0;
        int progress = 0;
        listener.onCopyProgress(progress);
        try {
            if (dst.exists()) {
                dst.delete();
                dst.createNewFile();
            }
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            int length = -1;
            byte[] buf = new byte[1024];
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
                outSize += length;
                int temp = (int) (((float) outSize) / inSize * 100);
                if (temp != progress) {
                    progress = temp;
                    listener.onCopyProgress(progress);
                }
                if (mFocusStop) {
                    listener.onStopProgress(FAIL_STOP_FORCE);
                    out.flush();
                    in.close();
                    out.close();
                    return false;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (outSize != in.available() || MD5.checkMd5Files(src, dst)) {
                listener.onStopProgress(FAIL_STOP_COPYERROR);
            }
            out.flush();
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean copyFile(String src, String dst, ProgressListener listener) {
        return copyFile(new File(src), new File(dst), listener);
    }

    /**
     * Execute installPackage Update
     *
     * @param packagePath
     *            is update file path
     *
     */
    public void installPackage(Context context, File packageFile) {
        String sdkStr = UpgradeInfo.getString("ro.build.version.sdk");
        int sdkInt = Integer.parseInt(sdkStr);
        /*if ( sdkInt > 29 ) {
            try {
              bootCommand(context,RECOVERY_UPDATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } */if(sdkInt >= 26 && !updateWithBCB(packageFile)){
            try {
                RecoverySystem.installPackage(context, packageFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if (sdkInt < 26) {
            rebootRecovery(context);
        }
    }


    /**
     * Execute recovery command
     *
     * @param
     * @see UPDATE_REBOOT
     */
    public static void rebootRecovery(Context context) {
        try {
            if (PrefUtil.LOG_DEBUG) Log.d("OTA","rebootrecovery");

            bootCommand(context, UPDATE_RECOVERY);
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    /**
     * Execute reboot normal command
     *
     * @param
     * @see UPDATE_REBOOT
     */
    public static void rebootNormal(Context context) {
        try {
            bootCommand(context, UPDATE_REBOOT);
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    /*
     * public static boolean checkVersion(long newVersion, String product) {
     * return (Build.TIME <= newVersion * 1000 && (Build.DEVICE .equals(product)
     * || Build.PRODUCT.equals(product))); }
     */

    /*
     * public static boolean checkIncVersion(String fingerprinter, String
     * product) { return (Build.FINGERPRINT.equals(fingerprinter) &&
     * (Build.DEVICE .equals(product) || Build.PRODUCT.equals(product))); }
     */

    /**
     * setMode to Debug
     *
     * @param debug
     *            true:debug mode false: update mode
     */
    public static void setDebugMode(boolean debug) {
        PrefUtil.DEBUG = debug;
    }

    public void forceStop(boolean val) {
        mFocusStop = val;
    }

}
