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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

//import javax.swing.text.html.HTMLDocument.Iterator;

import android.os.Environment;
import android.util.Log;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DownFileDao {

    private DatabaseHelper dbHelper;
    private Context mContext;

    public DownFileDao(Context co) {
        mContext = co;
        dbHelper = new DatabaseHelper(co);
    }

    public List<DownFilesInfo> getDowntasks() {
        List<DownFilesInfo> list = new ArrayList<DownFilesInfo>();
        SQLiteDatabase database = getConnection();
        String sql = "select * from " + DatabaseHelper.DOWNTASKTABLE;
        Cursor cursor = database.rawQuery(sql,null);
        while (cursor.moveToNext()) {
          if (PrefUtil.DEBUG)
            Log.d("db","getDowntasks:1-"+cursor.getString(1)+" 2-"+cursor.getString(2)+" 3-"+cursor.getString(3)+" 4-"+cursor.getString(4));

          DownFilesInfo info = new DownFilesInfo(cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
          list.add(info);
        }
        cursor.close();
        database.close();
        return list;
    }

    DownFilesInfo getDownFile(String url) {
        SQLiteDatabase database = getConnection();
        DownFilesInfo info = null;
        Cursor cursor = null;
        try {
            String sql = "select *  from " + DatabaseHelper.DOWNTASKTABLE + " where url=?";
            cursor = database.rawQuery(sql, new String[] { url });
            if (cursor.moveToNext()) {
                info = new DownFilesInfo(cursor.getString(1), cursor.getString(2), cursor.getString(3),
                cursor.getString(4));
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
       return info;
    }
    synchronized SQLiteDatabase getConnection() {
        SQLiteDatabase sqliteDatabase = null;
        try {
          sqliteDatabase= dbHelper.getReadableDatabase();
        } catch (Exception e) {
        }
        return sqliteDatabase;
    }

    public void saveDowntasks(List<DownFilesInfo> tasks) {
      SQLiteDatabase database = dbHelper.getWritableDatabase();
      //Log.d("db","saveDowntasks:"+tasks.size());
      for (DownFilesInfo task : tasks) {
          String sql = "insert into "+DatabaseHelper.DOWNTASKTABLE+"(file_name,file_location,file_md5,url) values (?,?,?,?)";
          Object[] bindArgs = { task.getName(), task.getLocal(),
            task.getMd5(),task.getUrl()};
          database.execSQL(sql, bindArgs);
      }
       if (null != database) {
        database.close();
       }
    }

    public void saveDowntasks(HashMap<String,DownFilesInfo> taskMap) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        for (DownFilesInfo task :taskMap.values()) {
            String sql = "insert into "+DatabaseHelper.DOWNTASKTABLE+"(file_name,file_location,file_md5,url) values (?,?,?,?)";
            String name = task.getLocal();
            Object[] bindArgs = { task.getName(), task.getLocal(),
              task.getMd5(),task.getUrl()};
            database.execSQL(sql, bindArgs);
        }
         if (null != database) {
          database.close();
         }
      }

    public void delFile(String locate) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        try {
            database.delete(DatabaseHelper.DOWNTASKTABLE, "file_location=?", new String[] { locate});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
        }
    }
    public void cleardata() {
        dbHelper.cleartable(DatabaseHelper.DOWNTASKTABLE);
        dbHelper.cleartable(DatabaseHelper.DOWNLOADINGRECORD);
    }
    public void initrecord() {
        dbHelper.cleartable(DatabaseHelper.DOWNLOADINGRECORD);
    }
}
