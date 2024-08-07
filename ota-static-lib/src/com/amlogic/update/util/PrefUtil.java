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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.SystemProperties;
public class PrefUtil {
    public static final String         DEFAULT_UPDATE_FILENAME   = "update.zip";
    public static Boolean        DEBUG                     = true;
    public static Boolean        LOG_DEBUG                 = false;
    public static final String         TAG                       = "OTA";
    private Context             mContext;
    // access by download task
    static final String         PREFS_DOWNLOAD_SIZE       = "download_size";
    // access by download task
    static final String         PREFS_DOWNLOAD_POSITION   = "download_position";
    static final String         PREFS_DOWNLOAD_UPDATEFILE = "download_updatefile";
    // access by checking task
    static final String         PREFS_DOWNLOAD_TARGET     = "download_target";
    static final String         PREFS_DOWNLOAD_FILELIST   = "download_filelist";
    // access by checking task
    static final String         PREFS_DOWNLOAD_URL        = "download_URL";
    // access by checking task
    static final String         PREFS_PACKAGE_DESCRIPTOR  = "package_descriptor";
    // access by checking task
    static final String         PREFS_PACKAGE_MD5         = "package_md5";
    static final String         PREFS_CHECK_STRING        = "check_string";
    static final String         PREFS_UPDATE_STRING       = "update_string";
    // access by loader
    static final String         PREFS_CHECK_TIME          = "check_time";
    static final String         PREFS_SCRIPT_ASK          = "update_script";
    static final String         PREFS_NOTICE_TRUE         = "notice_true";
    //static final String         PREF_START_RESTORE        = "retore_start";
    //private static final String FlagFile                  = ".wipe_record";
    private SharedPreferences   mPrefs;

    public PrefUtil(Context context) {
        mPrefs = context.getSharedPreferences("SHARE", Context.MODE_PRIVATE);
        mContext = context;
        if ( SystemProperties.getInt("app.debug.ota.download",-1) != -1) {
            LOG_DEBUG = true;
        }
    }

    public void setLastCheckTime(String date) {
        setString(PREFS_CHECK_STRING, date);
    }

    String getLastCheckTime() {
        return mPrefs.getString(PREFS_CHECK_STRING, null);
    }

    void setLastUpdateTime(String date) {
        setString(PREFS_UPDATE_STRING, date);
    }

    String getLastUpdateTime() {
        return mPrefs.getString(PREFS_UPDATE_STRING, null);
    }

    private void setString(String key, String Str) {
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putString(key, Str);
        mEditor.commit();
    }

    private void setStringSet(String key, Set<String> downSet) {
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putStringSet(key, downSet);
        mEditor.commit();
    }

    private void setInt(String key, int Int) {
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putInt(key, Int);
        mEditor.commit();
    }

    private void setLong(String key, long Long) {
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putLong(key, Long);
        mEditor.commit();
    }

    public void setUpdateWithScript(boolean bool) {
        setBoolean(PREFS_SCRIPT_ASK, bool);
    }

    public boolean getScriptASK() {
        return mPrefs.getBoolean(PREFS_SCRIPT_ASK, false);
    }

    void setBoolean(String key, Boolean bool) {
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putBoolean(key, bool);
        mEditor.commit();
    }

    public void setDownloadSize(long size) {
        setLong(PREFS_DOWNLOAD_SIZE, size);
    }

    public void setDownFileList(HashMap<String, DownFilesInfo> downlist) {

        HashSet<String> filelist = new HashSet<String>();
        for (DownFilesInfo task :downlist.values()) {
            filelist.add(task.local);
            setString(task.local,task.md5);
        }
        if (filelist.size() > 0) {
            setStringSet(PREFS_DOWNLOAD_FILELIST, filelist);
        }
    }

    public void setDownloadPos(long position) {
        setLong(PREFS_DOWNLOAD_POSITION, position);
    }

    void setBreakpoint(long size, long position) {
        setLong(PREFS_DOWNLOAD_SIZE, size);
        setLong(PREFS_DOWNLOAD_POSITION, position);
    }

    void setDownloadInfo(String url, String targetFile) {
        setString(PREFS_DOWNLOAD_URL, url);
        setString(PREFS_DOWNLOAD_TARGET, targetFile);
        setBreakpoint(0, 0);
    }

    void setMd5(String md5) {
        setString(PREFS_PACKAGE_MD5, md5);
    }

    String getMd5() {
        return mPrefs.getString(PREFS_PACKAGE_MD5, null);
    }

    public HashMap<String,String>  getFileListWithMd5() {
        Set<String> downfiles = mPrefs.getStringSet(PREFS_DOWNLOAD_FILELIST,null);
        HashMap<String,String> map = new HashMap();
        if (downfiles == null) return map;
        for (String local:downfiles) {
            String md5 = mPrefs.getString(local,"");
            map.put(local,md5);
        }
        return map;
    }

    public void setUpdateFile(String filepath) {
        setString(PREFS_DOWNLOAD_UPDATEFILE, filepath);
    }

    public String getUpdatFile() {
        return mPrefs.getString(PREFS_DOWNLOAD_UPDATEFILE, null);
    }

    void setCheckTime(long time) {
        setLong(PREFS_CHECK_TIME, time);
    }

    public void setPackageDescriptor(String str) {
        setString(PREFS_PACKAGE_DESCRIPTOR, str);
    }

    void setDownloadTarget(String target) {
        setString(PREFS_DOWNLOAD_TARGET, target);
    }

    void setDownloadURL(String URL) {
        setString(PREFS_DOWNLOAD_URL, URL);
    }

    void setNotice(Boolean bool) {
        setBoolean(PREFS_NOTICE_TRUE, bool);
    }

    public String getPackageDescriptor() {
        return mPrefs.getString(PREFS_PACKAGE_DESCRIPTOR, null);
    }

    public synchronized long getDownloadSize() {
        return mPrefs.getLong(PREFS_DOWNLOAD_SIZE, 0);
    }

    public long getDownloadPos() {
        return mPrefs.getLong(PREFS_DOWNLOAD_POSITION, 0);
    }

    String getDownloadTarget() {
        return mPrefs.getString(PREFS_DOWNLOAD_TARGET, null);
    }

    String getDownloadURL() {
        return mPrefs.getString(PREFS_DOWNLOAD_URL, null);
    }

    long getCheckTime() {
        return mPrefs.getLong(PREFS_CHECK_TIME, 0);
    }

    boolean getNotice() {
        return mPrefs.getBoolean(PREFS_NOTICE_TRUE, true);
    }

    public Set<String> getDownFileSet() {
        return mPrefs.getStringSet(PREFS_DOWNLOAD_FILELIST, null);
    }

    void setDownloadInfo(long size, long position, String target, String URL) {
        setDownloadSize(size);
        setDownloadPos(position);
        setDownloadTarget(target);
        setDownloadURL(URL);
    }

    public void resetAll() {
        setDownloadURL("");
        setMd5("");
        setUpdateWithScript(false);
        setString(PREFS_DOWNLOAD_URL, "");
        setString(PREFS_DOWNLOAD_TARGET, "");
        setBreakpoint(0, 0);
        setStringSet(PREFS_DOWNLOAD_FILELIST, null);
    }

    /**
     * @return
     */
    public int getID() {
        if (mPrefs.getInt("ID", 1001) == 1001) {
            int random = (int) (Math.random() * 1000);
            setInt("ID", random);
        }
        if (DEBUG)
            return 1000;
        else
            return mPrefs.getInt("ID", 0);
    }

    boolean getBooleanVal(String key, boolean def) {
        return mPrefs.getBoolean(key, def);
    }

 /*   void write2File() {
        String Mounted = Environment.getExternalStorage2State();
        if (!Mounted.equals(Environment.MEDIA_MOUNTED)) {
            return;
        }
        File flagFile = new File(Environment.getExternalStorage2Directory(),
                FlagFile);
        if (!flagFile.exists()) {
            try {
                flagFile.createNewFile();
            } catch (IOException excep) {
            }
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(flagFile);
        } catch (IOException excep) {
        }
        BufferedWriter output = new BufferedWriter(fw);
        Set<String> downfiles = mPrefs.getStringSet(PREFS_DOWNLOAD_FILELIST,
                null);
        if (downfiles != null && downfiles.size() > 0) {
            String[] downlist = downfiles.toArray(new String[0]);
            for (int i = 0; i < downlist.length; i++) {
                try {
                    output.write(downlist[i]);
                    output.newLine();
                } catch (IOException ex) {
                }
            }
        }
        try {
            output.close();
        } catch (IOException e) {
        }
    }*/

}
