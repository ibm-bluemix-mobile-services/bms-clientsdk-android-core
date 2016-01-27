/*
 *     Copyright 2015 IBM Corp.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.internal;

import android.os.Handler;

import com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.MFPAnalytics;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.UUID;

/**
 * MFPAnalyticsActivityLifecycleListener is a singleton used to help manage instrumentation of the android activity lifecycle.
 */
public class MFPAnalyticsActivityLifecycleListener {
    protected static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + MFPAnalyticsActivityLifecycleListener.class.getSimpleName());

    /**
     * The duration of the postDelayed call back when an activity is paused
     */
    protected static final long ACTIVITY_DELAY = 500;

    protected static MFPAnalyticsActivityLifecycleListener instance;
    protected static boolean isPaused;
    protected static Handler handler;
    protected static Runnable delayedCheck;
    protected static Long appUseStartTimestamp;
    protected static String appSessionID;

    protected static final String SESSION_DURATION_KEY = "$duration";
    protected static final String APP_SESSION_CATEGORY = "appSession";
    protected static final String CLOSED_BY_KEY = "$closedBy";

    /**
     * Represents how an application was close - by the user or a crash
     */
    public enum AppClosedBy {
        USER,
        CRASH
    }

    public static MFPAnalyticsActivityLifecycleListener getInstance() {
        if (instance == null) {
            instance = new MFPAnalyticsActivityLifecycleListener();
        }
        return instance;
    }

    private MFPAnalyticsActivityLifecycleListener() {
        isPaused = true;
        handler = new Handler();
        appUseStartTimestamp = null;
        appSessionID = null;
    }

    public void onResume() {
        isPaused = false;

        // remove the callbacks from an onPause if they are there
        if (delayedCheck != null) {
            handler.removeCallbacks(delayedCheck);
        }

        logAppForeground();
    }

    public void onPause() {
        isPaused = true;

        if (delayedCheck != null) {
            handler.removeCallbacks(delayedCheck);
        }

        // create a new runnable which runs with a small delay - after this delay if another activity
        // has resumed then isPaused will == false. If isPaused is still true the user has left the application
        handler.postDelayed(delayedCheck = new Runnable() {
            @Override
            public void run() {
                if (isPaused) {
                    logAppBackground();

                    // reset timestamp
                    appUseStartTimestamp = null;
                }
            }
        }, ACTIVITY_DELAY);
    }

    protected void logAppForeground() {
        if (appUseStartTimestamp == null) {
            appUseStartTimestamp = (new Date()).getTime();

            // Generate UUID for app session id
            appSessionID = UUID.randomUUID().toString();

            // Create JSON object with start app session metadata
            JSONObject metadata = new JSONObject();
            try {
                metadata.put(MFPAnalytics.CATEGORY, APP_SESSION_CATEGORY);
                metadata.put("timestamp", appUseStartTimestamp);
                metadata.put(MFPAnalytics.APP_SESSION_ID_KEY, appSessionID);
            } catch (JSONException e) {
                // should not happen
                logger.debug("JSONException encountered logging app session: " + e.getMessage());
            }
        }
    }

    protected void logAppBackground() {
        logAppSession(false);
    }

    public void logAppCrash() {
        logAppSession(true);
    }

    private void logAppSession(boolean isCrash) {
        if (appUseStartTimestamp == null) {
            // if the timestamp is null no app session was started, so don't record an app session
            String sessionType = isCrash ? "app crash" : "app session";
            logger.debug("Tried to record an " + sessionType + " without a starting timestamp");
            return;
        }

        long timestamp = (new Date()).getTime();
        AppClosedBy closedBy = isCrash ? AppClosedBy.CRASH : AppClosedBy.USER;

        // Create JSON object with close app session metadata
        JSONObject metadata = new JSONObject();
        try {
            metadata.put(MFPAnalytics.CATEGORY, APP_SESSION_CATEGORY);
            metadata.put(SESSION_DURATION_KEY, timestamp - appUseStartTimestamp);
            metadata.put(CLOSED_BY_KEY, closedBy.toString());
            metadata.put(MFPAnalytics.APP_SESSION_ID_KEY, appSessionID);
        } catch (JSONException e) {
            // should not happen
            logger.debug("JSONException encountered logging app session: " + e.getMessage());
        }

        MFPAnalytics.log(metadata);
    }

    public static String getAppSessionID() {
        return appSessionID;
    }
}
