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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

class DatabaseHelper extends SQLiteOpenHelper {

    static final String DOWNTASKTABLE = "download_task";
    static final String DOWNLOADINGRECORD = "record_task";

    /**
     * @param context
     * @param name
     * @param version
     */
    DatabaseHelper(Context context, String name, int version) {
        super(context, name, null, version);
    }

    /**
     * @param context
     * @param name
     * @param version
     */
    DatabaseHelper(Context context) {
        super(context, "download.db", null, VERSION);
    }

    private static final int VERSION = 1;

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + DOWNTASKTABLE
            + "(_id integer PRIMARY KEY AUTOINCREMENT, file_name char, "
            + "file_location char,file_md5 char,url char)");
        db.execSQL("create table "
            + DOWNLOADINGRECORD
            + "(_id integer PRIMARY KEY AUTOINCREMENT, thread_id integer, "
            + "start_pos integer, end_pos integer, complete_size integer,url char,is_start integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
    }

    DatabaseHelper(Context context, String name) {
        this(context, name, VERSION);
    }

    void cleartable(String name) {
        SQLiteDatabase database = getWritableDatabase();
        try {
            database.delete(name, "_id>=?", new String[] { "0" });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
        }
    }

}