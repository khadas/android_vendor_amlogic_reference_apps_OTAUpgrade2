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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Dao {

    private static Dao dao=null;
    private Context context;
    private  Dao(Context context) {
        this.context=context;
    }
    static  Dao getInstance(Context context){
        if (dao == null) {
            dao=new Dao(context);
        }
        return dao;
    }

    SQLiteDatabase getConnection() {
        SQLiteDatabase sqliteDatabase = null;
        try {
          sqliteDatabase= new DatabaseHelper(context).getReadableDatabase();
        } catch (Exception e) {
        }
        return sqliteDatabase;
    }

    /**
    *
    */
    synchronized boolean isHasInfors(String urlstr) {
        SQLiteDatabase database = getConnection();
        int count = -1;
        Cursor cursor = null;
        try {

           String sql = "select count(*)  from "+DatabaseHelper.DOWNLOADINGRECORD+" where url=?";
           cursor = database.rawQuery(sql, new String[] { urlstr });
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
            if (null != cursor) {
                cursor.close();
            }
        }
        return count == 0;
    }

    /**
    *
    */
    synchronized void saveInfos(List<DownloadFile> infos) {
        //init
        SQLiteDatabase database = getConnection();
        try {
            for (DownloadFile info : infos) {
                 String sql = "insert into "+DatabaseHelper.DOWNLOADINGRECORD+"(thread_id,start_pos, end_pos,complete_size,url,is_start) values (?,?,?,?,?,?)";
                 Object[] bindArgs = {info.getCurrentThread(),info.getStartPos(),info.getEndPos(),info.getCompleteSize(),info.getUrl(),0};
                 database.execSQL(sql, bindArgs);
           }
        } catch (Exception e) {
           //e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
        }
    }

    /**
    *
    */
    synchronized List<DownloadFile> getInfos(String urlstr) {
        List<DownloadFile> list = new ArrayList<DownloadFile>();
        SQLiteDatabase database = getConnection();
        Cursor cursor = null;
        try {
           String sql = "select thread_id, start_pos, end_pos,complete_size,url,is_start from "+DatabaseHelper.DOWNLOADINGRECORD+" where url=?";
           cursor = database.rawQuery(sql, new String[] { urlstr });
            while (cursor.moveToNext()) {
                long complete_size = cursor.getLong(3);
                long download_startpos = cursor.getLong(1);
                long download_endpos = cursor.getLong(2);
                boolean download_start = (cursor.getInt(4)==1?true:false);
                DownloadFile info = new DownloadFile(cursor.getInt(0),
                        download_startpos, download_endpos, complete_size,
                cursor.getString(4),download_start);
                list.add(info);
            }
        } catch (Exception e) {
           e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
            if (null != cursor) {
                cursor.close();
            }
        }
        return list;
    }

    /**
    *
    */
    synchronized void updataInfos(int threadId, long completeSize, String urlstr, boolean isStart) {
        SQLiteDatabase database = getConnection();
        int start = (isStart?1:0);
        try {
            String sql = "update "+DatabaseHelper.DOWNLOADINGRECORD+" set complete_size=?,is_start=? where url=? and thread_id=?";
            Object[] bindArgs = { completeSize,start,urlstr,threadId};
            database.execSQL(sql, bindArgs);
        } catch (Exception e) {
           e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
        }
    }

    /**
    *
    */
    synchronized void resetInfos() {
        SQLiteDatabase database = getConnection();
        try {
            String sql = "update "+DatabaseHelper.DOWNLOADINGRECORD+" set is_start=?";
            Object[] bindArgs = {0};
            database.execSQL(sql, bindArgs);
        } catch (Exception e) {
           e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
        }
    }
    /**
    *
    */
    synchronized void delete(String url) {
        SQLiteDatabase database = getConnection();
        try {
           database.delete(DatabaseHelper.DOWNLOADINGRECORD, "url=?", new String[] { url });
        } catch (Exception e) {
           e.printStackTrace();
        } finally {
           if (null != database) {
            database.close();
           }
        }
    }

    synchronized void delete(String url,int threadId) {
        SQLiteDatabase database = getConnection();
        try {
            database.delete(DatabaseHelper.DOWNLOADINGRECORD, "url=?", new String[] { url });
            String sql = "delete from "+DatabaseHelper.DOWNLOADINGRECORD+" where thread_id=? and url=?";
            Object[] bindArgs = { threadId, url };
            database.execSQL(sql, bindArgs);
        } catch (Exception e) {
           e.printStackTrace();
        } finally {
           if (null != database) {
            database.close();
           }
        }
    }
}
