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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

class DownloadFile {
    private File file;
    private String url;
    private String fileName;
    private long startPos;
    private long endPos;
    private long completeSize;
    private int threadNumTotal = 1;
    private int currentThread = 1;
    private boolean startDown = false;
    private HttpURLConnection httpConn = null;
    static int BUFFER_SIZE = 1024 * 512;

    DownloadFile() {
    }

    /**
     * @param int1
     * @param int2
     * @param int3
     * @param int4
     * @param string
     */
    DownloadFile(int thread_id, long start_pos, long end_pos,
        long complete_size, String url,boolean startDown) {
        currentThread = thread_id;
        startPos = start_pos;
        endPos = end_pos;
        startDown = false;
        completeSize = complete_size;
        this.url = url;
        this.startDown = startDown;
    }

    File getFile() {
        return file;
    }

    void setFile(File file) {
        this.file = file;
    }

    String getUrl() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }

    String getFileName() {
        return fileName;
    }

    void setFileName(String fileName) {
        this.fileName = fileName;
    }

    long getStartPos() {
        return startPos;
    }

    void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    long getEndPos() {
        return endPos;
    }

    void setEndPos(long endPos) {
        this.endPos = endPos;
    }

    int getThreadNumTotal() {
        return threadNumTotal;
    }

    void setThreadNumTotal(int threadNumTotal) {
        this.threadNumTotal = threadNumTotal;
    }

    int getCurrentThread() {
        return currentThread;
    }

    void setCurrentThread(int currentThread) {
        this.currentThread = currentThread;
    }

    static int getBUFFER_SIZE() {
        return BUFFER_SIZE;
    }

    static void setBUFFER_SIZE(int bUFFER_SIZE) {
        BUFFER_SIZE = bUFFER_SIZE;
    }

    InputStream getInputStreamByPos() {
        try {
            if (PrefUtil.DEBUG) Log.d(PrefUtil.TAG,"return InputStream"+(this.startPos+this.completeSize)+" end:"+this.endPos);
            if (this.url != null && !"".equals(this.url)) {

                if (this.startPos >= 0 && this.endPos >= 0 &&
                        this.completeSize >= 0 && (this.startPos + this.completeSize) < this.endPos) {
                    long mStartPoi = (long) (this.startPos+this.completeSize);
                    URL url = new URL(this.url);
                    httpConn = (HttpURLConnection) url.openConnection();
                    httpConn.setRequestMethod("GET");
                    httpConn.setAllowUserInteraction(false);
                    httpConn.setRequestProperty("Connection","Keep-Alive");
                    httpConn.setConnectTimeout(6000);//6s
                    httpConn.setReadTimeout(1000*600);//10min
                    httpConn.setDefaultUseCaches(false);
                    httpConn.setRequestProperty("RANGE", "bytes=" + mStartPoi + "-" + getEndPos());
                    httpConn.connect();
                    if (httpConn.getResponseCode() != 206 && httpConn.getResponseCode() != 200) {
                        Log.d("down","connect refused");
                        return null;
                    }
                    return httpConn.getInputStream();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public void closeHttpConn(){
        if (httpConn != null) {
            httpConn.disconnect();
            httpConn=null;
        }
        startDown = false;
    }

    public boolean isStart(){
        return startDown;
    }

    public void setIsStart(boolean start){
        startDown = start;
    }

    @Override
    public String toString() {
        return "DownloadFile [file=" + file + ", url=" + url + ", fileName="
                + fileName + ", startPos=" + startPos + ", endPos=" + endPos
                + ", completeSize=" + completeSize + ", threadNumTotal="
                + threadNumTotal + ", currentThread=" + currentThread
                + ", httpConn=" + httpConn + "]";
    }

    public long getCompleteSize() {
        return completeSize;
    }

    public void setCompleteSize(long completeSize) {
        this.completeSize = completeSize;
    }

}
