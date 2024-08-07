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

public class DownFilesInfo {
    public String name;
    public String local;
    public String md5;
    public String url;
    int    complete;
    int    filesize;

    DownFilesInfo(String name, String local, String md5, String url) {
        this.name = name;
        this.local = local;
        this.md5 = md5;
        this.url = url;
    }

    DownFilesInfo(int filesize, int complete, String url) {
        this.filesize = filesize;
        this.complete = complete;
        this.url = url;
    }

    public DownFilesInfo() {}

    public String getName() {
        return name;
    }

    public String getLocal() {
        return local;
    }

    public String getMd5() {
        return md5;
    }

    public String getUrl() {
        return url;
    }

    public int getComplete() {
        return complete;
    }

    @Override
    public String toString() {
        return "DownloadFilesInfo [name=" + name + ", local=" + local
                + ", md5=" + md5 + ", url=" + url + ", complete=" + complete
                + "]";
    }

    void setName(String name) {
        this.name = name;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setComplete(int complete) {
        this.complete = complete;
    }
}
