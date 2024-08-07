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

import android.util.Log;
import com.amlogic.update.util.DesUtils;
import com.amlogic.update.util.UpgradeInfo;
import com.amlogic.update.util.PrefUtil;
/**
 * @ClassName UpdateTasks
 * @Description Work Thread For Online Update,Known Subclasses is @see CheckUpdateTask and @see DownloadUpdateTask
 * @Date 2012/12/03
 * @Email
 * @Author
 * @Version V1.0
 */
public abstract class UpdateTasks implements Runnable {
    public static final int NO_ERROR               = 0;
    private boolean DEBUG = true||UpgradeInfo.isDebugAble();
    public static final int RUNNING_STATUS_UNSTART = 0;
    public static final int RUNNING_STATUS_RUNNING = 1;
    public static final int RUNNING_STATUS_PAUSE   = 2;
    public static final int RUNNING_STATUS_FINISH  = 3;
    protected Thread        mThread;
    protected long           mProgress              = 0;
    protected int           mErrorCode             = NO_ERROR;
    protected int           mRunningStatus         = RUNNING_STATUS_UNSTART;
    protected Object        mResult;
    private boolean resuming = false;

    // ----------------Runtime Method
    /**
     * Start work Thread and set status to @see RUNNING_STATUS_RUNNING
     *
     * @param null
     */
    public void start() {
        if (mRunningStatus != RUNNING_STATUS_RUNNING) {
            if (PrefUtil.LOG_DEBUG) Log.d("Upgrade","Upgrade:start");
            mRunningStatus = RUNNING_STATUS_RUNNING;
            mThread = new Thread(this);
            onStart();
            mThread.start();
        }
    }

    protected void onStart() {}

    @Override
    public void run() {
        onRunning();
        stop();
    }

    protected void onRunning() {
    }
    /**
     * pause work thread and set status to @see RUNNING_STATUS_PAUSE
     *
     * @param null
     */
    public void pause() {
        if (PrefUtil.LOG_DEBUG) Log.d("Upgrade","pause()");
        resuming = false;
        mRunningStatus = RUNNING_STATUS_PAUSE;
        onPause();
    }

    protected void onPause() {}
    /**
     * resume work thread and set status from @see RUNNING_STATUS_PAUSE to @see RUNNING_STATUS_RUNNING
     *
     * @param null
     */
    public void resume() {
        if (mRunningStatus == RUNNING_STATUS_PAUSE) {
            if (mThread != null && mThread.isAlive()) {
                if (PrefUtil.LOG_DEBUG) Log.d("Upgrade","download thread onResume");
                mRunningStatus = RUNNING_STATUS_RUNNING;
                onResume();
            } else {
                if (PrefUtil.LOG_DEBUG) Log.d("Upgrade","download onResume restart");
                start();
            }
        }
    }

    protected void onResume() {
        resuming = true;
    }
    /**
     * stop work thread and set status from @see RUNNING_STATUS_RUNNING to @see RUNNING_STATUS_FINISH
     * @param null
     */
    public void stop() {
        if (mRunningStatus != RUNNING_STATUS_UNSTART) {
            onStop();
            if (PrefUtil.LOG_DEBUG) Log.d("download","stop set running to finsh");
            mRunningStatus = RUNNING_STATUS_FINISH;
        }
    }

    protected void onReset() {}
    /**
     * reset work thread and set status to @see RUNNING_STATUS_UNSTART
     *
     * @param null
     */
    public boolean reset() {
        if (mRunningStatus == RUNNING_STATUS_FINISH
                || mRunningStatus == RUNNING_STATUS_PAUSE
                || mRunningStatus == RUNNING_STATUS_RUNNING) {
            onReset();
            mProgress = 0;
            mRunningStatus = RUNNING_STATUS_UNSTART;
            return true;
        }
        return false;
    }

    protected void onStop() {}

    // -----------------Public Method
    public int getErrorCode() {
        return mErrorCode;
    }

    public int getRunningStatus() {
        return mRunningStatus;
    }

    public long getProgress() {
        return mProgress;
    }

    public Object getResult() {
        return mResult;
    }
}
