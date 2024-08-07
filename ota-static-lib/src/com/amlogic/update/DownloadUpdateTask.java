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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import com.amlogic.update.util.DownFileDao;
import com.amlogic.update.util.DownFilesInfo;
import com.amlogic.update.util.DownloadUtil;
import com.amlogic.update.util.PrefUtil;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;

/**
 * @ClassName DownloadUpdateTask
 * @Description Download Update files from Server
 * @see UpdateTasks
 * @Date 2012/12/03
 * @Email
 * @Author
 * @Version V1.0
 */
public class DownloadUpdateTask extends UpdateTasks {
    private static final String TAG = PrefUtil.TAG;
    public static final int ERROR_NETWORK_UNAVAIBLE = 1;
    public static final int ERROR_UNKNOWN = 2;
    public static final int ERROR_IOERROR = 3;
    private Notifier notifier;
    private Context mContext;
    private PrefUtil mPrefs;
    private DownFileDao dao;
    private DownFilesInfo curTask = null;
    private DownloadUtil downloader = null;
    private String targetString = "";
    private DownloadSize mDownSize = null;
    private CheckPathCallBack mcheckTaskCallback;

    /**
     * Constructs a object of DownloadUpdateTask for Download Update Files
     *
     * @param mContext
     */
    public DownloadUpdateTask(Context mContext) {
        this.mContext = mContext;
        mPrefs = new PrefUtil(mContext);
        mDownSize = new DownloadSize();
    }

    @Override
    protected void onStart() {
        super.onStart();
        dao = new DownFileDao(mContext);
    }

    public void setErrorCode(int err) {
        mErrorCode = err;
    }

    public void setCallbacks(CheckPathCallBack callback) {
        mcheckTaskCallback = callback;
    }

    @Override
    protected void onRunning() {
        super.onRunning();
        List<DownFilesInfo> tasks = dao.getDowntasks();
        if (mcheckTaskCallback != null) {
            for (DownFilesInfo t : tasks) {
                t.setLocal(mcheckTaskCallback.onExternalPathSwitch(t.getLocal()));
            }
        }
        boolean errOccur = false;
        if (tasks.size() > 0 && mRunningStatus == RUNNING_STATUS_RUNNING) {
            curTask = tasks.get(0);
            downloader = new DownloadUtil(mDownSize, mContext, this);
            if (PrefUtil.LOG_DEBUG) Log.d(TAG, "CurTask:" + curTask.toString());
            do {
                if (mRunningStatus == RUNNING_STATUS_UNSTART) {
                    break;
                } else if (mRunningStatus == RUNNING_STATUS_PAUSE) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    if (doDownload(downloader)) {
                        tasks.remove(0);
                        errOccur = false;
                        if (tasks.size() > 0 && mRunningStatus == RUNNING_STATUS_RUNNING) {
                            curTask = tasks.get(0);
                            downloader = new DownloadUtil(mDownSize, mContext, this);
                        }
                    } else {
                        errOccur = true;
                        continue;
                    }
                }
            } while (tasks.size() > 0
                    && (mRunningStatus == RUNNING_STATUS_RUNNING || mRunningStatus == RUNNING_STATUS_PAUSE));
        }
        if (mRunningStatus == RUNNING_STATUS_RUNNING || mRunningStatus == RUNNING_STATUS_FINISH) {
            if (tasks.size() == 0 && !errOccur) {
                mRunningStatus = RUNNING_STATUS_FINISH;
                mErrorCode = NO_ERROR;
                mResult = new DownloadResult();
                if (notifier != null) {
                    notifier.Successnotify();
                }
            } else {
                mRunningStatus = RUNNING_STATUS_FINISH;
                mErrorCode = ERROR_UNKNOWN;
                if (notifier != null) {
                    notifier.Failednotify();
                }
            }
            mPrefs.setDownloadPos(0);
            mProgress = 0;
        }
    }

    private boolean doDownload(DownloadUtil download) {
        int ret = -1;
        while (mRunningStatus == RUNNING_STATUS_PAUSE) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            ret = download.Save2File(curTask.url, curTask.local, curTask.name, curTask.md5);
            if (PrefUtil.LOG_DEBUG) Log.d(TAG, "ret:" + ret);
        } catch (Exception e) {
           // mRunningStatus = RUNNING_STATUS_FINISH;
          //  e.printStackTrace();
          Log.d(TAG,"download exception here"+e);
        } finally {
            if (PrefUtil.LOG_DEBUG)
                Log.d(TAG, "ret is " + ret + " mRunningStatus" + mRunningStatus);
            if (ret == 0) {
                dao.delFile(curTask.local);
                download.delete(curTask.url);
                return true;
            } else if (ret == -1) {
                // for test
                //if(downloader != null && downloader.isdownloading()) {
               //   downloader.pause();
                //}
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
               return false;
            } else if (ret == -2){
                 //xxxxxxx----->for test
                 return true;
            }else {
                if (downloader != null && downloader.isdownloading()) {
                    downloader.pause();
                }
                return false;
            }
        }
    }

    /**
     * return path of update file for execute update
     *
     * @return set Notifier object to Download work thread.
     * @param null
     */
    public void setNotify(Notifier notifier) {
        this.notifier = notifier;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (downloader != null && downloader.isdownloading()) {
            downloader.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (downloader != null) {
            downloader.resume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (downloader != null && downloader.isdownloading()) {
            downloader.stop();
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public class DownloadSize {
        /*
         * private int length; private int TimeoutException = 0;
         */

        void DownloadSize() {
            getSavedSize();
        }

        void getSavedSize() {
            mProgress = (int) mPrefs.getDownloadPos();
        }
        /*
         * void saveInitSize() { mPrefs.setDownloadPos(length); }
         */

        public void updateSize(int downSize) {
            synchronized (this) {
                mProgress += downSize;
                mPrefs.setDownloadPos(mProgress);
            }
        }

        public void deleteSize(long urlTotalSize) {
            synchronized (this) {
                mProgress -= urlTotalSize;
                mPrefs.setDownloadPos(mProgress);
            }
        }
        /*
         * int getDownloadSize() { synchronized (this) { return length; } }
         */
    }

    /**
     * @ClassName CheckUpdateResult
     * @Description inner class of CheckUpdateTask to return Check Result by
     *              getResult()
     * @see UpdateTasks
     * @see CheckUpdateResult
     * @Date 2012/12/03
     * @Email
     * @Author
     * @Version V1.0
     */
    public class DownloadResult {

        /**
         * return Set of Update Download Files
         *
         * @return Set<String> update Files set
         * @param null
         */
        public Set<String> getFilesList() {
            if (mPrefs != null) {
                return mPrefs.getDownFileSet();
            }
            return null;
        }
        public HashMap<String,String> getFileListWithMd5() {
            if (mPrefs != null) {
                return mPrefs.getFileListWithMd5();
            }
            return null;
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        dao.cleardata();
        mPrefs.resetAll();
    }

    @Override
    public long getProgress() {
        return  mPrefs.getDownloadPos();
    };

    /* interface for file path switch */
    public interface CheckPathCallBack {
        public String onExternalPathSwitch(String filePath);
    }
}
