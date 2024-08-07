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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.SyncFailedException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

import org.apache.http.HttpConnection;

import com.amlogic.update.DownloadUpdateTask;
import com.amlogic.update.DownloadUpdateTask.DownloadSize;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DownloadUtil {
    private boolean DEBUG = (true||UpgradeInfo.isDebugAble());
    private static final String TAG = PrefUtil.TAG;
    public static int defaultThread = 3;
    private int threadNum;
    static final int MSG_FILE_ZISE = 100;
    static final int MSG_DOWNLOAD_SIZE = 101;
    static final int MSG_DOWNLOAD_FINISH = 102;
    static final int MSG_DOWNLOAD_ERROR = 103;
    private Context mContext;
    private static final int INIT = 1;
    private static final int DOWNLOADING = 2;
    private static final int PAUSE = 3;
    private DownloadSize mDownloadSize = null;
    private int state = INIT;
    private DownloadUpdateTask downloadTask;
    ThreadPoolExecutor service;
    private CountDownLatch countDownLatch = null;
    private static List<SaveFileThread> workers = new ArrayList<SaveFileThread>();

    public boolean isdownloading() {
        return state == DOWNLOADING;
    }

    int getThreadNum() {
        return threadNum;
    }

    public DownloadUtil(DownloadSize downloadSize, Context mContext, DownloadUpdateTask downloadThread) {
        this.mDownloadSize = downloadSize;
        this.mContext = mContext;
        this.downloadTask = downloadThread;
        // defaultThread = getDefaultThread(mContext);
        service = new ThreadPoolExecutor(defaultThread, defaultThread + 2, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(5), new ThreadPoolExecutor.DiscardOldestPolicy());
        Dao.getInstance(mContext).resetInfos();//reset thread is not start
    }

    boolean isExistFile(String filePath) {
        if (PrefUtil.LOG_DEBUG) Log.d("FILE","isExistFile"+filePath);
        File file = new File(filePath);
        return file.exists();
    }

    void createDir(String dirPath) {
        if (dirPath != null || !"".equals(dirPath)) {
            if (PrefUtil.LOG_DEBUG) Log.d("FILE","createDir"+dirPath);
            File file = new File(dirPath);
            if (!file.isDirectory()) {
                file.mkdirs();
            }
        }
    }

    private static int getDefaultThread(Context cxt) {
        int threadDefault = 3;
        ConnectivityManager connectivityManager = (ConnectivityManager) cxt
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            threadDefault = 1;
        }
        return threadDefault;
    }

    /**
     * param urlstr: Url to download
     */
    private boolean isFirst(String urlstr) {
        boolean ret = Dao.getInstance(mContext).isHasInfors(urlstr);
        return ret;
    }

    public boolean isServicePartDownload(int startPoi,long l,String urlStr){

        HttpURLConnection httpConn=null;
        boolean partDownload =false;
        try{
            URL url = new URL(urlStr);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setAllowUserInteraction(false);
            httpConn.setRequestProperty("Connection","Keep-Alive");
            httpConn.setConnectTimeout(6000*3);//never time out
            httpConn.setReadTimeout(0);
            httpConn.setDefaultUseCaches(false);
            httpConn.setRequestProperty("RANGE", "bytes=" + startPoi + "-" + l);
            httpConn.connect();
            if (httpConn.getResponseCode() == 206) {
                partDownload = true;
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            httpConn.disconnect();
        }
        if (PrefUtil.LOG_DEBUG)
            Log.d(TAG,"Download update file,may service support partial download: " + partDownload);
        return partDownload;

    }

    int getFileLength(String url) {
        URL murl;
        int len = -1;
        try {
            murl = new URL(url);
            HttpURLConnection httpConn = (HttpURLConnection) murl.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setRequestProperty("Accept-Encoding", "identity");
            if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                len = httpConn.getContentLength();
                httpConn.disconnect();
                if (PrefUtil.LOG_DEBUG)
                    Log.d(TAG, "getFileLength:" + len);
                return len;
            }
        } catch (MalformedURLException e) {
            downloadTask.setErrorCode(DownloadUpdateTask.ERROR_NETWORK_UNAVAIBLE);
            e.printStackTrace();
            return -1;
        } catch (IOException ex) {
            downloadTask.setErrorCode(DownloadUpdateTask.ERROR_IOERROR);
            ex.printStackTrace();
            return -1;
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return -1;
    }

    public int Save2File(String urlAddress, String saveDir, String fileName, final String md5) throws IOException {
        if (PrefUtil.LOG_DEBUG)
            Log.d(TAG,
                "Save2File urlAddress :" + urlAddress + "saveDir:" + saveDir + " fileName:" + fileName + " md5:" + md5);
        state = DOWNLOADING;
        long urlTotalSize = -1;
        final File file = new File(saveDir+".temp");
        try {
            List<DownloadFile> downInfo;

            urlTotalSize = getFileLength(urlAddress);
            if (urlTotalSize <= 0)
                return -1;
            if (isFirst(urlAddress)) {
                if (file.exists()) {
                    file.delete();
                }
                downInfo = new ArrayList<DownloadFile>();
                threadNum = (int) (urlTotalSize / DownloadFile.BUFFER_SIZE) + 1;
                if (PrefUtil.LOG_DEBUG)
                    Log.d(TAG, "download target :" + file.getAbsolutePath() + "size:" + urlTotalSize);
                if (threadNum > defaultThread) {
                    threadNum = defaultThread;
                }
                if (!isServicePartDownload(0, urlTotalSize / 2, urlAddress)) {
                    threadNum = 1;
                }
                long spanSize = (long) Math.ceil((float) urlTotalSize / threadNum);

                if (PrefUtil.LOG_DEBUG)
                    Log.d(TAG, "spanSize=" + spanSize + " urlTotalSize=" + urlTotalSize);
                if (urlTotalSize <= 0)
                    return -1;

                for (int i = 0; i < threadNum; i++) {
                    DownloadFile downloadFile = new DownloadFile();
                    downloadFile.setFile(file);
                    downloadFile.setUrl(urlAddress);
                    downloadFile.setStartPos((int) (i * spanSize));
                    int ends = (int) ((i + 1) * spanSize - 1);
                    if ((threadNum - 1) == i || ends >= urlTotalSize) {
                        downloadFile.setEndPos((int) urlTotalSize - 1);
                    } else {
                        downloadFile.setEndPos(ends);
                    }
                    downloadFile.setCurrentThread(i);
                    downloadFile.setFileName(fileName);
                    downloadFile.setIsStart(false);
                    if (DEBUG) Log.d(TAG, "dump download info:" + downloadFile.toString());
                    downInfo.add(downloadFile);
                }
                if (downInfo.size() > 0) {
                    Dao.getInstance(mContext).saveInfos(downInfo);
                }
            } else {
                downInfo = Dao.getInstance(mContext).getInfos(urlAddress);
                if (downInfo.size() > 0) {
                    threadNum = downInfo.size();
                    if (PrefUtil.LOG_DEBUG)
                        Log.d("OTA", downInfo.toString());
                }
                if (PrefUtil.LOG_DEBUG)
                    Log.d("OTA", "down Thread target:" + downInfo.size() + " file:" + file.getAbsolutePath());
            }

            countDownLatch = new CountDownLatch(downInfo.size() );

            for (int i = 0; i < downInfo.size(); i++) {
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rws");
                DownloadFile downloadFile = downInfo.get(i);
                if (PrefUtil.LOG_DEBUG) Log.d("OTA", "down Thread target:" + downloadFile.isStart() );
                if (downloadFile.isStart()) {
                    continue;
                }
                try {
                    SaveFileThread work = new SaveFileThread(randomAccessFile, downloadFile, countDownLatch,
                            mDownloadSize);
                    synchronized(workers){
                        if (PrefUtil.LOG_DEBUG) Log.d(TAG,"add workers and workers size is "+workers.size());
                        workers.add(work);
                    }
                    service.execute(work);
                } catch (RejectedExecutionException ex) {
                    downloadTask.setErrorCode(DownloadUpdateTask.ERROR_NETWORK_UNAVAIBLE);
                    ex.printStackTrace();
                    return -1;
                }
            }
        } catch (Exception ex) {
            downloadTask.setErrorCode(DownloadUpdateTask.ERROR_UNKNOWN);
            ex.printStackTrace();
            throw new IOException();
        } finally {
            try {
                if (countDownLatch != null) {
                    countDownLatch.await();
                }
                if (PrefUtil.LOG_DEBUG)
                    Log.d("OTA", "await success" );
            } catch (InterruptedException ex) {
                downloadTask.setErrorCode(DownloadUpdateTask.ERROR_UNKNOWN);
                ex.printStackTrace();
                //throw new IOException();
                return -1;
            }
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }finally{
                    synchronized(workers){
                        if (workers.size() == 0) {
                            if (PrefUtil.LOG_DEBUG) Log.d(TAG,"************workers.size is 0******");
                            break;
                        }
                    }
                }
            }
        }
        if (state == PAUSE) {
            Log.d(TAG, "loading status is pause:" + state);
            return -1;
        }
        if (PrefUtil.LOG_DEBUG)
            Log.d(TAG, "check md5 values of file:" + file.getName() + "path:" + file.getAbsolutePath());

        if (!MD5.checkMd5(md5, file)) {
            if (PrefUtil.LOG_DEBUG)
                Log.d(TAG, "xml md5 check failed");
            if (file.exists()) {
                 file.delete();
                mDownloadSize.deleteSize(urlTotalSize);
                downloadTask.setErrorCode(DownloadUpdateTask.ERROR_UNKNOWN);
            }
           // fileIn.close();
            return -1;
        } else {
            if (PrefUtil.LOG_DEBUG) Log.d(TAG, "xml md5 check ok");
            File file2 = new File(saveDir);
            file.renameTo(file2);
            //fileIn.close();
            return 0;
        }
    }

    class SaveFileThread implements Runnable {
        private RandomAccessFile randomFile;
        private DownloadFile downloadFile;
        private CountDownLatch countDownLatch;
        private DownloadSize mDownloadSize;
        private boolean stopFlag = false;

        SaveFileThread(RandomAccessFile randomFile, DownloadFile downloadFile, CountDownLatch countDownLatch,
                DownloadSize downloadSize) {
            this.randomFile = randomFile;
            this.downloadFile = downloadFile;
            this.countDownLatch = countDownLatch;
            this.mDownloadSize = downloadSize;
        }

        public void stopSave() {
            stopFlag = true;
            //downloadFile.closeHttpConn();
        }

        public void run() {
            boolean isExceptionOccur = false;
            try {
                InputStream is = null;
                // try service 3 times wait for 206 response
                for (int count = 0; count < 3; count++) {
                    is = this.downloadFile.getInputStreamByPos();
                    if (is != null) {
                        break;
                    }
                }
                int length = 0;
                int lastLen = -1;
                if (is != null) {
                    stopFlag = false;
                    if (PrefUtil.LOG_DEBUG) Log.d(TAG, "this.downloadFile.getStartPos():" + this.downloadFile.getStartPos() + ":"
                            + this.downloadFile.getCompleteSize()+"Thread:"+Thread.currentThread().getName());
                    long alreayload = this.downloadFile.getStartPos() + this.downloadFile.getCompleteSize();
                    this.randomFile.seek(alreayload);
                    byte[] by = new byte[1024 * 64];
                    Dao.getInstance(mContext).updataInfos(this.downloadFile.getCurrentThread(),
                            this.downloadFile.getCompleteSize(), this.downloadFile.getUrl(), true);
                    int count = 0;
                    while (!stopFlag && (length = is.read(by, 0, by.length)) > 0) {

                        downloadFile.setIsStart(true);
                        this.randomFile.write(by, 0, length);
                        int n=64*1024/length;
                        //Log.d(TAG,"getInputStreamPos->"+Thread.currentThread().getName()+"---write----"+length+" complete:"+downloadFile.getCompleteSize()+
                        //                "pointer:"+randomFile.getFilePointer()+"  start:"+downloadFile.getStartPos());
                        if ((randomFile.getFilePointer()-downloadFile.getCompleteSize() - downloadFile.getStartPos()) != length) {
                            if (PrefUtil.LOG_DEBUG) Log.d(TAG,"OOOOOOOPS,write is not as u think");
                        }
                       // downloadFile.setCompleteSize(this.downloadFile.getCompleteSize() + length);
                        downloadFile.setCompleteSize((this.randomFile.getFilePointer() - downloadFile.getStartPos()));
                        if (stopFlag)
                            break;
                        if (lastLen != 0 && (count++) % (16*n) == 0) {
                            mDownloadSize.updateSize(lastLen);
                            lastLen = 0;
                        }
                        lastLen += length;
                        if (false) {
                            Log.d(TAG, "download fileSize:" + lastLen + " threadId"
                                    + this.downloadFile.getCurrentThread());
                        }
                    }
                    Dao.getInstance(mContext).updataInfos(this.downloadFile.getCurrentThread(),
                            this.downloadFile.getCompleteSize(), this.downloadFile.getUrl(), false);
                    is.close();
                    is = null;
                    downloadFile.setIsStart(false);
                    // this.randomFile.close();
                    // this.downloadFile.closeHttpConn();
                    if (PrefUtil.LOG_DEBUG)
                        Log.d(TAG, "execute Ok Delete" + state + "Thread:" + Thread.currentThread().getName()
                                + "   state:" + state);

                }
            }catch (IOException ex) {
                // handler.sendEmptyMessage(MSG_DOWNLOAD_ERROR);
                isExceptionOccur = true;
               // ex.printStackTrace();
                if (PrefUtil.LOG_DEBUG)
                    Log.d(TAG, "download file err...." + this.downloadFile + " " + ex.fillInStackTrace());
            } catch (Exception e) {
                e.printStackTrace();
                isExceptionOccur = true;
                if (PrefUtil.LOG_DEBUG)
                    Log.d(TAG, "download err...." + this.downloadFile + " " + e.fillInStackTrace());
            } finally {

                if (PrefUtil.LOG_DEBUG) Log.d(TAG,"finally"+Thread.currentThread().getName()+"stopFlag="+stopFlag);
                if (this.downloadFile.getCompleteSize()+this.downloadFile.getStartPos() == (1+this.downloadFile.getEndPos())) {
                    if (DEBUG) Log.d(TAG,"delete downloadthread~!"+Thread.currentThread().getName());
                    Dao.getInstance(mContext).delete(downloadFile.getUrl(), downloadFile.getCurrentThread());
                } else {
                    if (PrefUtil.LOG_DEBUG) Log.d(TAG,Thread.currentThread().getName()+"download:"+ this.downloadFile.getCompleteSize()
                        +" start:"+this.downloadFile.getStartPos()+"-"+this.downloadFile.getEndPos());
                    Dao.getInstance(mContext).updataInfos(this.downloadFile.getCurrentThread(),
                            this.downloadFile.getCompleteSize(), this.downloadFile.getUrl(), false);
                }

                try {
                    this.randomFile.close();
                    this.downloadFile.closeHttpConn();
                    this.countDownLatch.countDown();
                    synchronized(workers){
                        workers.remove(this);
                    }
                } catch (Exception exp) {
                    exp.printStackTrace();
                }

            }
            // }
        }
    }

    public void delete(String urlstr) {
        Dao.getInstance(mContext).delete(urlstr);
    }

    public synchronized void pause() {
        state = PAUSE;
        synchronized(workers){
            for (SaveFileThread t : workers) {
                if (!t.stopFlag) {
                    t.stopSave();
                    t.downloadFile.closeHttpConn();
                }
            }
        }
    }

    void reset() {
        state = INIT;
    }

    public synchronized void resume() {
        state = DOWNLOADING;
    }

    public synchronized void stop() {
        state = PAUSE;
        if (service != null && !service.isShutdown()) {
            service.shutdown();
        }
    }
}
