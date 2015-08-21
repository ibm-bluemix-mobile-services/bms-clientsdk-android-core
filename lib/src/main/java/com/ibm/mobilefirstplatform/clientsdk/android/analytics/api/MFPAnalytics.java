/*
 * IBM Confidential OCO Source Materials
 *
 * 5725-I43 Copyright IBM Corp. 2006, 2015
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *
 */
package com.ibm.mobilefirstplatform.clientsdk.android.analytics.api;


import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * MFAnalytics provides means of persistently capturing analytics data and provides a method call to send captured data to
 * the Bluemix server.
 * </p>
 * <p>
 * Capture is on by default.
 * </p>
 * <p>
 * When this MFPAnalytics class's capture flag is turned on via enable method call,
 * all analytics will be persisted to file in the following JSON object format:
 * <p>
 * <pre>
 * {
 *   "timestamp"    : "17-02-2013 13:54:27:123",  // "dd-MM-yyyy hh:mm:ss:S"
 *   "level"        : "ERROR",                    // ERROR || WARN || INFO || LOG || DEBUG
 *   "package"      : "your_tag",                 // typically a class name, app name, or JavaScript object name
 *   "msg"          : "the message",              // a helpful log message
 *   "metadata"     : {"hi": "world"},            // (optional) additional JSON metadata
 *   "threadid"     : long                        // (optional) id of the current thread
 * }
 * </pre>
 * </p>
 * <p>
 * Log data is accumulated persistently to a log file until the file size is greater than FILE_SIZE_LOG_THRESHOLD.
 * At this point the log file is rolled over. Log data will only be captured once
 * {@link com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient#setApplicationContext(Context) } is called.  Once both files are full, the oldest log data
 * is pushed out to make room for new log data.
 * </p>
 * <p>
 * Log file data is sent to the Bluemix server when this class's send() method is called and the accumulated log
 * size is greater than zero.  When the log data is successfully uploaded, the persisted local log data is deleted.
 * </p>
 */
public class MFPAnalytics {

    private static final String KEY_METADATA_CATEGORY = "$category";
    private static final String TAG_CATEGORY_EVENT = "event";
    public static final String KEY_METADATA_DURATION = "$duration";
    public static final String KEY_METADATA_TYPE = "$type";
    public static final String TAG_SESSION = "$session";
    public static final String KEY_METADATA_START_TIME = "$startTime";

    protected static Logger logger = Logger.getInstance("imf.analytics");

    private static final Map<String, JSONObject> lifecycleEvents = new HashMap<>();

    /**
     * Enable persistent capture of analytics data.  Enable, and thus capture, is the default.
     */
    public static void enable () {
        Logger.setAnalyticsCapture(true);
    }

    /**
     * Disable persistent capture of analytics data.
     */
    public static void disable () {
        Logger.setAnalyticsCapture(false);
    }

    /**
     * Send the accumulated log data when the persistent log buffer exists and is not empty.  The data
     * accumulates in the log buffer from the use of {@link MFPAnalytics} with capture
     * (see {@link MFPAnalytics#enable()}) turned on.
     *
     */
    public static void send () {
        Logger.sendAnalytics(null);
    }

    /**
     * See {@link MFPAnalytics#send()}
     *
     * @param listener RequestListener which specifies an onSuccess callback and an onFailure callback (see {@link ResponseListener})
     */
    public static void send(ResponseListener listener) {
        Logger.sendAnalytics(listener);
    }

    public static void logApplicationEntry(){
        JSONObject metadata = new JSONObject();

        long startTime = System.currentTimeMillis();

        try {
            metadata.put(KEY_METADATA_CATEGORY, TAG_CATEGORY_EVENT);
            metadata.put(KEY_METADATA_TYPE, TAG_SESSION);
            metadata.put(KEY_METADATA_START_TIME, startTime);

            logger.analytics("", metadata);

            metadata.put("$sessionId", UUID.randomUUID());

            lifecycleEvents.put(TAG_SESSION, metadata);
        } catch (JSONException e) {
            //Do nothing.
        }
    }

    public static void logApplicationExit(){
        JSONObject metadata = new JSONObject();

        JSONObject startMetadata = lifecycleEvents.get(TAG_SESSION);

        long startTime = startMetadata.optInt("KEY_METADATA_START_TIME");
        long endTime = System.currentTimeMillis();
        long eventDuration = endTime - startTime;

        try {
            metadata.put(KEY_METADATA_CATEGORY, TAG_CATEGORY_EVENT);
            metadata.put(KEY_METADATA_DURATION, eventDuration);
            metadata.put(KEY_METADATA_TYPE, TAG_SESSION);

            logger.analytics("", metadata);

            lifecycleEvents.remove(TAG_SESSION);
        } catch (JSONException e) {
            //Do nothing.
        }
    }

    public static void logSessionStart(){
        JSONObject metadata = new JSONObject();

        long startTime = System.currentTimeMillis();

        try {
            metadata.put(KEY_METADATA_CATEGORY, TAG_CATEGORY_EVENT);
            metadata.put(KEY_METADATA_TYPE, TAG_SESSION);
            metadata.put(KEY_METADATA_START_TIME, startTime);

            logger.analytics("", metadata);

            metadata.put("$sessionId", UUID.randomUUID());

            lifecycleEvents.put(TAG_SESSION, metadata);
        } catch (JSONException e) {
            //Do nothing.
        }
    }

    public static void logSessionEnd(){
        JSONObject metadata = new JSONObject();

        JSONObject startMetadata = lifecycleEvents.get(TAG_SESSION);

        long startTime = startMetadata.optInt("KEY_METADATA_START_TIME");
        long endTime = System.currentTimeMillis();
        long eventDuration = endTime - startTime;

        try {
            metadata.put(KEY_METADATA_CATEGORY, TAG_CATEGORY_EVENT);
            metadata.put(KEY_METADATA_DURATION, eventDuration);
            metadata.put(KEY_METADATA_TYPE, TAG_SESSION);

            logger.analytics("", metadata);

            lifecycleEvents.remove(TAG_SESSION);
        } catch (JSONException e) {
            //Do nothing.
        }
    }

}
