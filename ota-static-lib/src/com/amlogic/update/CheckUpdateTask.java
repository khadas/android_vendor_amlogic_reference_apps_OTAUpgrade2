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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amlogic.update.util.DownFileDao;
import com.amlogic.update.util.DownFilesInfo;
import com.amlogic.update.util.PrefUtil;
import com.amlogic.update.util.UpdatePositionStage;
import com.amlogic.update.util.UpgradeInfo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

/**
 * @ClassName CheckUpdateTask
 * @Description Check for an new Update version, returns the updated information
 * @see UpdateTasks
 * @Date 2012/12/03
 * @Email
 * @Author
 * @Version V1.0
 */
public class CheckUpdateTask extends UpdateTasks {
    private static boolean DEBUG = true || UpgradeInfo.isDebugAble();
    private static final String TAG = PrefUtil.TAG;
    private static final boolean CHECK_DEBUG = PrefUtil.DEBUG;
    public static final int ERROR_UNDISCOVERY_NEW_VERSION = 1;
    public static final int ERROR_NETWORK_UNAVAIBLE = 2;
    public static final int ERROR_UNKNOWN = 3;
    private Context mContext;
    private PrefUtil mPreferences;
    private UpgradeInfo mUtil;
    private DownFileDao dao;
    private static final int HANDLE_XML_DOWNLOAD_FINISH = 100;
    private static final int HANDLE_XML_DOWNLOAD_FAIL = 101;
    private static final int CHECK_TIMEOUT = 20 * 1000;
    public static String XML_NAME = "ota_update.xml";
    public static String FILE_NAME = "update.xml";
    private static String XmlDir = null;
    private static final String OTA_COMMAND = "update_with_inc_ota";
    private static final String SCRIPT_COMMAND = "update_with_script";
    private Notifier notifier = null;
    private String mCustomData = null;
    private String updatename = null;
    // private CheckPathCallBack mcheckTaskCallback;
    /**
     * Constructs a object of CheckUpdateTask for Check Update Work Thread
     *
     * @param mContext
     */
    public CheckUpdateTask(Context mContext) {
        this.mContext = mContext;
        XmlDir = mContext.getFilesDir().getAbsolutePath();
        mPreferences = new PrefUtil(mContext);
        mUtil = new UpgradeInfo(mContext);
        dao = new DownFileDao(mContext);
        mResult = new CheckUpdateResult();
    }

    /*
     * public void setCallbacks(CheckPathCallBack callback){ mcheckTaskCallback
     * = callback; }
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    public void setCustomRequestData(String val) {
        mCustomData = val;
    }

    public void setCustomUrl(String url) {
        if (!url.isEmpty()) {
            UpgradeInfo.postUrl = url;
        }
    }

    @Override
    protected void onRunning() {
        super.onRunning();
        sendPost();
    }

    private void sendPost() {
        if (DEBUG)
            Log.v(TAG, "send post to server");
        HttpPost post = new HttpPost(UpgradeInfo.postUrl);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("updating_apk_version", UpgradeInfo.updating_apk_version));
        params.add(new BasicNameValuePair("brand", UpgradeInfo.brand));
        params.add(new BasicNameValuePair("device", UpgradeInfo.device));
        params.add(new BasicNameValuePair("board", UpgradeInfo.board));
        params.add(new BasicNameValuePair("mac", UpgradeInfo.mac));
        params.add(new BasicNameValuePair("firmware", UpgradeInfo.firmware));
        params.add(new BasicNameValuePair("android", UpgradeInfo.android));
        params.add(new BasicNameValuePair("time", UpgradeInfo.time));
        params.add(new BasicNameValuePair("builder", UpgradeInfo.builder));
        params.add(new BasicNameValuePair("fingerprint", UpgradeInfo.fingerprint));
        params.add(new BasicNameValuePair("id", mPreferences.getID() + ""));
        if (mCustomData != null) {
            String[] custom = mCustomData.split("&");
            for (int i = 0; i < custom.length; i++) {
                String[] data = custom[i].split("=");
                params.add(new BasicNameValuePair(data[0], data[1]));
            }
        }
        if (DEBUG)
            Log.d(TAG, params.toString());
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(httpParameters, CHECK_TIMEOUT);
        HttpClient httpClient = new DefaultHttpClient(httpParameters);
        try {
            post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse response = httpClient.execute(post);
            if (DEBUG)
                Log.i(TAG, "response status:  " + response.getStatusLine().getStatusCode());
            if (response.getStatusLine().getStatusCode() < 400 && response.getStatusLine().getStatusCode() >= 200) {
                HttpEntity entity = response.getEntity();
                String msg = EntityUtils.toString(entity);
                if (DEBUG)
                    Log.i(TAG, "get data:  " + msg);
                String url[] = msg.split("=");
                Log.i(TAG, "get data:  " + url.length + "url:" + url[0]);
                if (url.length == 2 && url[0].equals("url") && url[1].length() > 10) {
                    if (DEBUG)
                        Log.i(TAG, "xml url:" + url[1]);
                    url[1] = url[1].replace(" ", "");
                    url[1] = url[1].replace("\r\n", "");
                    downloadXML(url[1], XML_NAME);
                } else {
                    if (DEBUG)
                        Log.i(TAG, "Can'n find new firmware");
                    mErrorCode = ERROR_UNDISCOVERY_NEW_VERSION;
                }
            } else {
                mErrorCode = ERROR_UNKNOWN;
            }
        } catch (Exception ex) {
            mErrorCode = ERROR_UNKNOWN;
            if (DEBUG) {
                Log.d(TAG, "download err...." + ex.fillInStackTrace());
            }
        }
        if (DEBUG)
            Log.v(TAG, "finish send post to server");
    }

    private void downloadXML(String url, String xmlName) {
        if (DEBUG)
            Log.i(TAG, "start download a xml file:" + xmlName + " url:" + url);
        mPreferences.resetAll();
        if (XmlDir == null)
            return;
        File dir = new File(XmlDir);
        File file = new File(XmlDir + "/" + xmlName);
        if (DEBUG)
            Log.i(TAG, "dir:" + dir.getAbsolutePath() + " exixts:" + dir.exists() + " mkdir:" + dir.mkdirs() + "file:"
                    + file.getAbsolutePath());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream xmlfile = null;
        int filesize = 0;
        int readSize = 0;
        try {
            URL Url = new URL(url);
            HttpURLConnection cn = (HttpURLConnection) Url.openConnection();
            InputStream input = cn.getInputStream();
            filesize = cn.getContentLength();
            if (filesize <= 0 || input == null) {
                mErrorCode = ERROR_UNKNOWN;
                handleDownloadResult(HANDLE_XML_DOWNLOAD_FAIL, null);
            }
            xmlfile = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int numread;
            while ((numread = input.read(buf)) != -1) {
                xmlfile.write(buf, 0, numread);
                readSize += numread;
            }
            xmlfile.flush();
            cn.disconnect();
        } catch (Exception ex) {
            if (DEBUG) {
                Log.d(TAG, "download err...." + ex.fillInStackTrace());
            }
        } finally {
            try {
                if (xmlfile != null) {
                    xmlfile.close();
                    xmlfile = null;
                }
                if (filesize == readSize && filesize != 0) {
                    handleDownloadResult(HANDLE_XML_DOWNLOAD_FINISH, xmlName);
                } else {
                    mErrorCode = ERROR_UNKNOWN;
                    handleDownloadResult(HANDLE_XML_DOWNLOAD_FAIL, null);
                }
            } catch (IOException e) {
                mErrorCode = ERROR_UNKNOWN;
                if (PrefUtil.DEBUG)
                    Log.d(TAG, "exception", e.fillInStackTrace());
                handleDownloadResult(HANDLE_XML_DOWNLOAD_FAIL, null);
            }
        }
    }

    private void handleDownloadResult(int msg, Object obj) {
        switch (msg) {
            case HANDLE_XML_DOWNLOAD_FINISH:
                if (DEBUG)
                    Log.i(TAG, "xml " + obj.toString() + " download finish");
                dao.initrecord();
                parserXml(XML_NAME);
                mErrorCode = NO_ERROR;
                break;
            case HANDLE_XML_DOWNLOAD_FAIL:
                if (DEBUG)
                    Log.i(TAG, "xml download fail");
                mErrorCode = ERROR_NETWORK_UNAVAIBLE;
                dao.cleardata();
                break;
        }
    }

    /* parser xml command file */
    private void parserXml(String xML_NAME2) {
        DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder domBuilder;
        String command = null;
        String storagemem = null;
        String force = null;
        String sdcardask = null;
        String description = null;
        String country = null;
        updatename = null;
        HashMap<String, DownFilesInfo> downlist;
        if (XmlDir == null)
            return;
        try {
            domBuilder = domfac.newDocumentBuilder();
            InputStream in = new FileInputStream(XmlDir + "/" + XML_NAME);
            Document doc = domBuilder.parse(in);
            Element root = doc.getDocumentElement();
            NodeList nodelist_1 = root.getChildNodes();
            dao.cleardata();
            if (nodelist_1 != null) {
                for (int i = 0; i < nodelist_1.getLength(); i++) {
                    Node node_1 = nodelist_1.item(i);
                    if (node_1.getNodeName().equals("command")) {
                        command = node_1.getAttributes().getNamedItem("name").getNodeValue();
                        force = node_1.getAttributes().getNamedItem("force").getNodeValue();
                        if (!command.equals(OTA_COMMAND) && !command.equals(SCRIPT_COMMAND)) {
                            mErrorCode = ERROR_UNDISCOVERY_NEW_VERSION;
                            return;
                        } else if (command.equals(SCRIPT_COMMAND)) {
                            sdcardask = "true";
                        } else {
                            sdcardask = "false";
                        }
                        NodeList nodelist_2 = node_1.getChildNodes();
                        String isUpdateZip;
                        if (nodelist_2 != null) {
                            downlist = new HashMap<String, DownFilesInfo>();
                            DownFilesInfo fileinfo;
                            String fileNodeName = null;
                            for (int j = 0; j < nodelist_2.getLength(); j++) {
                                Node node_2 = nodelist_2.item(j);
                                if (node_2.getNodeName().equals("url")) {
                                    fileinfo = new DownFilesInfo();
                                    fileinfo.name = node_2.getAttributes().getNamedItem("name").getNodeValue();
                                    fileNodeName = fileinfo.name;
                                    fileinfo.local = node_2.getAttributes().getNamedItem("locattr").getNodeValue();

                                    /*
                                     * if(mcheckTaskCallback!=null){
                                     * fileinfo.local =
                                     * mcheckTaskCallback.onExternalPathSwitch(
                                     * fileinfo.local); }
                                     */
                                    isUpdateZip = node_2.getAttributes().getNamedItem("updatezip").getNodeValue();
                                    fileinfo.url = node_2.getFirstChild().getNodeValue();
                                    if (PrefUtil.DEBUG)
                                        Log.d(TAG, " afileinfo.local" + fileinfo.local + "fileinfo.name");
                                    /*
                                     * configure on server local attribute is
                                     * NULL,useDefault Directory
                                     */
                                    if (fileinfo.name.equals(null) || fileinfo.name.equals("")
                                            || fileinfo.local.equals(null) || fileinfo.local.equals("")) {
                                        fileinfo.name = PrefUtil.DEFAULT_UPDATE_FILENAME;
                                        fileinfo.local = XmlDir + "/" + fileinfo.name;
                                    }
                                    if (isUpdateZip.equals("true")) {
                                        Log.d(TAG,"updatename set here"+fileinfo.local);
                                        updatename = fileinfo.local;
                                    }
                                    if (CHECK_DEBUG)
                                        Log.d(TAG, "fileInfo:" + fileinfo.toString());
                                    downlist.put(fileinfo.name, fileinfo);
                                } /* end parse url */
                                if (fileNodeName != null && node_2.getNodeName().equals("md5")) {
                                    // int listsize = downlist.size();
                                    fileinfo = downlist.get(fileNodeName);
                                    Log.d(TAG, "fileInfo md5 fileinfo!=null ?:" + (fileinfo != null));
                                    if (fileinfo != null) {
                                        if (DEBUG)
                                            Log.i(TAG, "get xml md5:" + fileinfo.name + "url :" + fileinfo.url
                                                    + " local:" + fileinfo.local);
                                        if (!fileinfo.name
                                                .equals(node_2.getAttributes().getNamedItem("name").getNodeValue())) {
                                            downlist.remove(fileinfo.name);
                                            continue;
                                        }
                                        fileinfo.md5 = node_2.getFirstChild().getNodeValue();
                                        if (DEBUG)
                                            Log.i(TAG, "get xml md5:" + fileinfo.md5);
                                        downlist.put(fileinfo.name, fileinfo);
                                    }
                                } /* end parse md5 */
                                if (node_2.getNodeName().equals("storagemem")) {
                                    storagemem = node_2.getFirstChild().getNodeValue();
                                    Log.i(TAG, "get storagemem:" + storagemem+"sdcardask"+sdcardask);
                                    /*if (sdcardask.equals("false")) {
                                        repDirForBigFile(storagemem, downlist);
                                    }*/
                                }
                                if (node_2.getNodeName().equals("description")) {
                                    country = node_2.getAttributes().getNamedItem("country").getNodeValue();
                                    if (description == null) {
                                        if (country.equals(mUtil.country) || country.equals("ELSE"))
                                            description = node_2.getFirstChild().getNodeValue();
                                    }
                                }
                            }
                            if (DEBUG) {
                                Log.i(TAG, "get xml description:" + description);
                            }
                            mPreferences.setDownFileList(downlist);
                            dao.saveDowntasks(downlist);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mErrorCode = ERROR_UNDISCOVERY_NEW_VERSION;
            return;
        }
        mPreferences.setPackageDescriptor(description);
        Log.i(TAG, "##################" + Long.parseLong(storagemem)+"updatename"+updatename);
        mPreferences.setDownloadSize(Long.parseLong(storagemem));
        mPreferences.setUpdateFile(updatename);
        if (sdcardask.equals("true")) {
            mPreferences.setUpdateWithScript(true);
        }
    }
    /**replace storage directory when file size is more than 460M**/
    private boolean repDirForBigFile(String fileSize, HashMap map) {
        Long size = Long.valueOf(fileSize);

        if (size < UpdatePositionStage.M400ZIE) {
            return false;
        } else {
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                DownFilesInfo val = (DownFilesInfo) entry.getValue();
                if (val.local.startsWith(XmlDir)) {
                    val.local=val.local.replace(XmlDir,UpdatePositionStage.getSizeDecidePosition(size));
                    updatename = val.local;
                }
                map.put(entry.getKey(), val);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void setNotify(Notifier notifier) {
        this.notifier = notifier;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notifier != null) {
            if (mErrorCode == NO_ERROR && mPreferences != null) {
                mPreferences.setLastCheckTime(new Date().toString());
                notifier.Successnotify();
            } else {
                notifier.Failednotify();
            }
        }
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
    public class CheckUpdateResult {

        public CheckUpdateResult() {
        }

        /**
         * return description information of new version
         *
         * @return new version description
         * @param null
         */
        public String getmUpdateDescript() {
            return mPreferences.getPackageDescriptor();
        }

        /**
         * return path of update file for execute update
         *
         * @return path of specified by Online update Server
         * @param null
         */
        public String getmUpdateFileName() {
            return mPreferences.getUpdatFile();
        }

        /**
         * @return whether this update is update_with_ota or update_with_script,
         *         and return true when update is update_with_script
         * @param null
         */
        public boolean ismIsScriptAsk() {
            return mPreferences.getScriptASK();
        }

        /**
         * @return memory needed by all update files
         *
         * @param null
         */
        public long getMemoryAsk() {
            return mPreferences.getDownloadSize();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

    };

    /* interface for file path switch */
    /*
     * public interface CheckPathCallBack{ public String
     * onExternalPathSwitch(String filePath); }
     */
}
