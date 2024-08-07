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

import android.app.backup.BackupManager;
import android.app.backup.IBackupManager;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Build.VERSION;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.amlogic.update.util.PrefUtil;
import com.amlogic.update.util.UpgradeInfo;

public final class Backup {
    private boolean DEBUG = false || UpgradeInfo.isDebugAble();
    private IBackupConfirmListener mConfirmListener;
    static final String TAG = PrefUtil.TAG;
    static String[] mArgs;
    int mNextArg;
    private IBinder mIBinder;
    private IBackupManager mBackupManager;
    private Context mCxt;

    public Backup(Context cxt) {
        mCxt = cxt;
    }

    public void setConfirmListener(IBackupConfirmListener confirmlistener) {
        mConfirmListener = confirmlistener;
    }

    public void main(String[] args) {
        if (DEBUG)
            Log.d(TAG, "Beginning: " + args[0]);
        mArgs = args;
        try {
            // new Backup().
            run();
        } catch (Exception e) {
            if (DEBUG)
                Log.e(TAG, "Error running backup/restore", e);
        }
        if (DEBUG)
            Log.d(TAG, "Finished.");
    }

    String mFileName = null;

    public void run() {
        mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        if (mBackupManager == null) {
            if (DEBUG)
                Log.e(TAG, "Can't obtain Backup Manager binder");
            return;
        }
        mFileName = nextArg();
        int socketFd = 0; // Integer.parseInt(nextArg());
        String arg = nextArg();
        if (arg.equals("backup")) {
            doFullBackup(socketFd);
        } else if (arg.equals("restore")) {
            doFullRestore(socketFd);
        } else {
            if (DEBUG)
                Log.e(TAG, "Invalid operation '" + arg + "'");
        }
    }

    private void doFullBackup(int socketFd) {
        ArrayList<String> packages = new ArrayList<String>();
        boolean saveApks = true;
        boolean saveObbs = false;
        boolean saveShared = false;
        boolean doEverything = true;
        boolean allIncludesSystem = true;
        String arg;
        while ((arg = nextArg()) != null) {
            if (arg.startsWith("-")) {
                if ("-apk".equals(arg)) {
                    saveApks = true;
                } else if ("-noapk".equals(arg)) {
                    saveApks = false;
                } else if ("-obb".equals(arg)) {
                    saveObbs = true;
                } else if ("-noobb".equals(arg)) {
                    saveObbs = false;
                } else if ("-shared".equals(arg)) {
                    saveShared = true;
                } else if ("-noshared".equals(arg)) {
                    saveShared = false;
                } else if ("-system".equals(arg)) {
                    allIncludesSystem = true;
                } else if ("-nosystem".equals(arg)) {
                    allIncludesSystem = false;
                } else if ("-all".equals(arg)) {
                    doEverything = true;
                } else {
                    Log.w(TAG, "Unknown backup flag " + arg);
                    continue;
                }
            } else {
                // Not a flag; treat as a package name
                packages.add(arg);
            }
        }
        if (doEverything && packages.size() > 0) {
            if (DEBUG)
                Log.w(TAG, "-all passed for backup along with specific package names");
        }
        if (!doEverything && !saveShared && packages.size() == 0) {
            if (DEBUG)
                Log.e(TAG, "no backup packages supplied and neither -shared nor -all given");
            return;
        }
        // try {
        ParcelFileDescriptor fd = null;
        try {
            fd = ParcelFileDescriptor.open(new File(mFileName),
                    ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE);
            Log.d(TAG, "param: saveApks " + saveApks + "saveObbs " + saveObbs + " saveShared " + saveShared
                    + " fd==null" + (fd == null));
            String[] packArray = new String[packages.size()];
            try {
                Method getbuMethod = null;
                Log.d("tt", "android.os.version:" + android.os.Build.VERSION.SDK_INT);
                if (android.os.Build.VERSION.SDK_INT < 21) {
                     getbuMethod = Class.forName("android.app.backup.IBackupManager")
                            .getMethod("fullBackup", new Class[] { ParcelFileDescriptor.class, boolean.class,
                                    boolean.class, boolean.class, boolean.class, boolean.class, String[].class });
                    getbuMethod.invoke(mBackupManager, fd, saveApks, saveObbs, saveShared, doEverything,
                            allIncludesSystem, (Object) (packages.toArray(packArray)));
                } else if (android.os.Build.VERSION.SDK_INT < 26) {
                     getbuMethod = Class.forName("android.app.backup.IBackupManager")
                            .getMethod("fullBackup",
                                    new Class[] { ParcelFileDescriptor.class, boolean.class, boolean.class,
                                            boolean.class, boolean.class, boolean.class, boolean.class, boolean.class,
                                            String[].class });
                    getbuMethod.invoke(mBackupManager, fd, saveApks, saveObbs, saveShared, doEverything,
                            allIncludesSystem, allIncludesSystem, true, /* compress */
                            (Object) (packages.toArray(packArray)));

                }else{
                     getbuMethod = Class.forName("android.app.backup.IBackupManager")
                            .getMethod("adbBackup",
                                    new Class[] { ParcelFileDescriptor.class, boolean.class, boolean.class,
                                            boolean.class, boolean.class, boolean.class, boolean.class, boolean.class,
                                            boolean.class,String[].class });
                    getbuMethod.invoke(mBackupManager, fd, true, true, saveShared, true, true
                            , allIncludesSystem, true, /* compress */true/*doKeyValue*/,
                            (Object) (packages.toArray(packArray)));
                }
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            /*
             * mBackupManager.fullBackup(fd, saveApks, saveObbs, saveShared,
             * doEverything, allIncludesSystem, allIncludesSystem,
             * allIncludesSystem, packages.toArray(packArray));
             */
        } catch (FileNotFoundException e) {
            Log.e(TAG, "file not found ", e);
        } finally {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException e) {
                }
            }
            if (mConfirmListener != null) {
                mConfirmListener.onBackupComplete();
            }
        }
        /*
         * } catch (RemoteException e) { Log.e(TAG,
         * "Unable to invoke backup manager for backup"); }
         */
    }

    private void doFullRestore(int socketFd) {
        // No arguments to restore
        ParcelFileDescriptor fd = null;
        try {
            fd = ParcelFileDescriptor.open(new File(mFileName),
                    ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE);
            // ParcelFileDescriptor fd = ParcelFileDescriptor.adoptFd(socketFd);
            Method getbuMethod = null;
            //if (android.os.Build.VERSION.SDK_INT < 26) {
            //  mBackupManager.fullRestore(fd);
            //}else{

                getbuMethod = Class.forName("android.app.backup.IBackupManager")
                        .getMethod("adbRestore",new Class[]{ParcelFileDescriptor.class});
                getbuMethod.invoke(mBackupManager, fd);
            //}
        } catch (FileNotFoundException e) {
            Log.e(TAG, "file not found ", e);
        } /*catch (RemoteException e) {
            Log.e(TAG, "Unable to invoke backup manager for restore");
        } */catch (IllegalAccessException e) {
            Log.e(TAG, "Unable to invoke backup manager method for restore");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException e) {
                }
            }
            if (mConfirmListener != null) {
                mConfirmListener.onRestoreComplete();
            }
        }
    }

    private String nextArg() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mNextArg];
        mNextArg++;
        return arg;
    }

    public interface IBackupConfirmListener {
        public void onBackupComplete();

        public void onRestoreComplete();
    }
}
