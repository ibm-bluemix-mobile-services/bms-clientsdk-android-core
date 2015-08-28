/*
    Copyright 2015 IBM Corp.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.ibm.mobilefirstplatform.clientsdk.android.logger.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.MFPAnalytics;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.FailResponse;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.MFPRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResourceRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.internal.FileLogger;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.internal.FileLoggerInterface;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.internal.JULHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.security.auth.x500.X500Principal;

/**
 * Logger is an abstraction of, and pass through to, android.util.Log.  Logger provides some
 * enhanced capability such as capturing log calls, package filtering, and log level control at
 * both global and individual package scope.  It also provides a method call to send captured logs to
 * the Bluemix application.
 *
 * When this Logger class's capture flag is turned on via setCapture(true) method call,
 * all messages passed through this class's log methods will be persisted to file in the
 * following JSON object format:
 * <p>
 * <pre>
 * {
 *   "timestamp"    : "17-02-2013 13:54:27:123",  // "dd-MM-yyyy hh:mm:ss:S"
 *   "level"        : "ERROR",                    // FATAL || ERROR || WARN || INFO || DEBUG
 *   "package"      : "your_package_name",                 // typically a class name, app name, or JavaScript object name
 *   "msg"          : "the message",              // a helpful log message
 *   "metadata"     : {"hi": "world"},            // (optional) additional JSON metadata, appended via doLog API call
 *   "threadid"     : long                        // (optional) id of the current thread
 * }
 * </pre>
 * </p>
 * <p>
 * Log data is accumulated persistently to a log file until the file size is greater than FILE_SIZE_LOG_THRESHOLD.
 * At this point the log file is rolled over.  Log data will only be captured once setContext(Context)
 * is called.  Once both files are full, the oldest log data is pushed out to make room for new log data.
 * </p>
 * <p>
 * Log file data is sent to the Bluemix application when this class's send() method is called and the accumulated log
 * size is greater than zero.  When the log data is successfully uploaded, the persisted local log data is deleted.
 * </p>
 * <p>
 * All of this class's method calls, such as info(String), are pass-throughs to the equivalent method
 * call in android.util.Logger when the LEVEL log function called is at or above the set LEVEL.
 * </p>
 * <p>
 * As a convenience, this Logger also sets a global java.util.logging.Handler.  Developers who would rather
 * use java.util.logging.Logger API may do so, with the understanding that java.util.logging.Logger API calls
 * will not be captured until setContext(Context) is called.  The mapping of java.util.logging.Level to Logger.LEVEL is:
 * </p>
 * <p>
 * <table>
 * <tr><td>SEVERE</td><td>ERROR</td></tr>
 * <tr><td>WARNING</td><td>WARN</td></tr>
 * <tr><td>INFO</td><td>INFO</td></tr>
 * <tr><td>CONFIG</td><td>DEBUG</td></tr>
 * <tr><td>FINE</td><td>DEBUG</td></tr>
 * <tr><td>FINER</td><td>DEBUG</td></tr>
 * <tr><td>FINEST</td><td>DEBUG</td></tr>

 * </table>
 * </p>
 *
 */
public final class Logger {

    /**
     * @exclude
     *
     * Use this object if you need a synchronized wait lock to wait for completion of set*
     * method calls.  Like this:
     * <p>
     * <pre>
     * synchronized(Logger.WAIT_LOCK) {
     *     Logger.WAIT_LOCK.wait(timeout);  // wait to continue execution until WAIT_LOCK.notify()
     * }
     * </pre>
     * </p>
     */
    static public final Object WAIT_LOCK = new Object();

    // for internal logging to android.util.Log only, not our log collection
    public static final String LOG_PACKAGE_NAME = Logger.class.getName ();
    private static final String CONTEXT_NULL_MSG = Logger.class.getName() + ".setContext(Context) must be called to fully enable debug log capture.  Currently, the 'capture' flag is set but the 'context' field is not.  This warning will only be printed once.";
    private static boolean context_null_msg_already_printed = false;
    /**
     * @exclude
     */
    public static final String FILENAME = "wl.log";
    /**
     * @exclude
     */
    public static final String ANALYTICS_FILENAME = "analytics.log";
    /**
     * @exclude
     */
    public static final String SHARED_PREF_KEY = Logger.class.getName();
    // protected for unit testing:
    protected static final String SHARED_PREF_KEY_logFileMaxSize = "logFileMaxSize";
    /**
     * @exclude
     */
    public static final String SHARED_PREF_KEY_CRASH_DETECTED = "crashDetected";

    /**
     * @exclude
     */
    public static final String SHARED_PREF_KEY_logPersistence = "logPersistence";
    /**
     * @exclude
     */
    public static final String SHARED_PREF_KEY_level = "level";
    /**
     * @exclude
     */
    public static final String SHARED_PREF_KEY_filters = "filters";

    // when configuration is set from the server in production, these keys take precedence:
    /**
     * @exclude
     */
    public static final String SHARED_PREF_KEY_logPersistence_from_server = "logPersistenceFromServer";
    /**
     * @exclude
     */
    public static final String SHARED_PREF_KEY_level_from_server = "levelFromServer";
    /**
     * @exclude
     */
    public static final String SHARED_PREF_KEY_filters_from_server = "filtersFromServer";

    // some defaults, protected for unit testing
    /**
     * @exclude
     */
    public static final boolean DEFAULT_capture = true;  // capture is on by default
    /**
     * @exclude
     */
    public static final boolean DEFAULT_analyticsCapture = true; // analytics is enabled by default
    protected static final int DEFAULT_logFileMaxSize = 100000;  // bytes

    // number of files java.util.logging.FileHandler can roll over
    /**
     * @exclude
     */
    public static final int MAX_NUM_LOG_FILES = 2;

    private static Context context;
    private static Boolean capture = null;  // save log messages to file?
    private static Boolean analyticsCapture = null; // save analytics data to file?
    // size when we stop accumulating data in the file:
    private static Integer logFileMaxSize = null;  // bytes
    private static LEVEL level = null;
    // we keep a global java.util.logging.Handler to capture third-party stuff:
    private static JULHandler julHandler = new JULHandler();
    private static HashMap<String, LEVEL> filters = new HashMap<String, LEVEL>();
    // don't set up the static UncaughtExceptionHandler until after we have a context
    private static UncaughtExceptionHandler uncaughtExceptionHandler = null;
    // Track instances so we give back the same one for the same 'packageName' passed to getInstance method.
    // We use a WeakHashMap because some instances in this map may go out of scope
    // very soon after instantiation, thus no reason to keep a strong reference, and let
    // the garbage collector do its job.
    private static WeakHashMap<String, Logger> instances = new WeakHashMap<String, Logger>();

    // we have a fixed size thread pool for higher performance log capture.  This way we never block the UI or main thread
    // when Logger.* methods that are run in the work queue are called.  Keep this set to '1' in the createThreadPoolWorkQueue
    // otherwise log entries and behavior will get out of chronological order.
    // I got much better memory usage and performance with my created ThreadPoolExecutor rather than Executors.newSingleThreadExecutor()

    // the Runnable instances don't take up much memory,
    // so we have a rather large queue.
    static private final ThreadPoolExecutor ThreadPoolWorkQueue = new ThreadPoolExecutor(1, 1,
            100, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000));
    // by default (unfortunately) the ThreadPoolExecutor will throw an exception
    // when you submit the (queueMaxSize + 1) job, to have it block you do:
    static {
        ThreadPoolWorkQueue.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                // Even though this dude says this is not safe:
                // http://stackoverflow.com/questions/3446011/threadpoolexecutor-block-when-queue-is-full?lq=1
                // for our purposes it is safe.  Our ThreadPoolExecutor is only ever running
                // one thread.
                try {
                    executor.getQueue().put (r);
                }
                catch (InterruptedException e) {
                    // To get here, someone would have to intentionally interrupt the ThreadPoolExecutor,
                    // which can't really happen.  If it does, however, it would simply result in the loss
                    // of a line of log data or the postponement of the log sending until the next try.
                }
            }
        });
    }

    private final String packageName;

    //Use these flags to determine if a send request is in progress, either for the logs or for the analytics:
    private static boolean sendingLogs = false;
    private static boolean sendingAnalyticsLogs = false;



    /**
     * Levels supported in this Logger class.
     */
    static public enum LEVEL {
        /**
         * @exclude
         */
        ANALYTICS { @Override protected int getLevelValue() {return  25;}; }, // Specifies data needs to go to analytics file
        FATAL	  { @Override protected int getLevelValue() {return  50;}; },
        ERROR     { @Override protected int getLevelValue() {return 100;}; },
        WARN      { @Override protected int getLevelValue() {return 200;}; },
        INFO      { @Override protected int getLevelValue() {return 300;}; },
        DEBUG     { @Override protected int getLevelValue() {return 400;}; };

        /**
         * @exclude
         */
        protected abstract int getLevelValue();
        /**
         * @exclude
         */
        protected boolean isLoggable() {  // call like this:  LEVEL.WARN.isLoggable(), which checks against the global level object
            LEVEL currentLevel = getLevelSync();
            return (null != currentLevel) && currentLevel.getLevelValue() >= this.getLevelValue();
        }

        /**
         * Get the LEVEL enum from the level String parameter, or null if not found.
         * <p>
         * Example:  LEVEL.fromString("ERROR");  // returns LEVEL.ERROR enum
         * </p>
         *
         * @param level a String representing the LEVEL enum
         * @return LEVEL enum matching the level parameter, or null
         */
        static public LEVEL fromString (final String level) {
            try {
                return LEVEL.valueOf (level.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
    }

    static private FileLoggerInterface fileLoggerInstance;

    // log application crash stack traces.  This handler is registered after we have the context object in Logger.setContext(Context)
    private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
    {
        private final Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

        @Override
        public final void uncaughtException(final Thread t, final Throwable e)
        {
            // place a marker that indicates for next run that a crash was caught:
            if (null != context) {
                context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE).edit ().putBoolean (SHARED_PREF_KEY_CRASH_DETECTED, true).commit();
            }
            // log it to file:
            Logger logger = Logger.getInstance (this.getClass().getName());
            // TODO: add context to metadata object
            logger.fatal ("Uncaught Exception", e);
            // allow it to pass through:
            defaultUEH.uncaughtException(t, e);
        }
    }

    // private, for factory creation of Logger objects
    private Logger(final String packageName) {
        if (fileLoggerInstance == null) {
            FileLogger.setContext(context);
            fileLoggerInstance = FileLogger.getInstance();
        }
        this.packageName = (packageName == null || packageName.trim().equals ("")) ? "NONE" : packageName.trim();
    };

    /*
     * @exclude
     * For unit testing only.  Also unsets static setting fields.
     */
    protected static void unsetContext() {
        instances.clear();
        context = null;
        capture = null;
        analyticsCapture = null;
        logFileMaxSize = null;
        level = null;
        filters = null;
        uncaughtExceptionHandler = null;
        fileLoggerInstance = null;
        LogManager.getLogManager().getLogger("").removeHandler(julHandler);
    }


    /**
     * Get or create an instance of this logger.  If an instance already exists for
     * 'packageName' parameter, that instance will be returned.
     *
     * @param packageName the package or tag that should be printed with log messages.  The value is passed
     *        through to android.util.Log and persistently recorded when log capture is enabled.
     * @return an instance of this class
     */
    static synchronized public Logger getInstance(final String packageName) {
        Logger logger = instances.get (packageName); {
            if (null == logger) {
                logger = new Logger(packageName);
                instances.put (packageName,  logger);
            }
        }
        return logger;
    }


    /**
     * Context object must be set in order to use the Logger API.  This is called automatically by BMSClient.
     *
     * @param context Android Context object
     */
    static public void setContext(final Context context) {
        // once setContext is called, we can set up the uncaught exception handler since
        // it will force logging to the file
        if (null == Logger.context) {

            // set a custom JUL Handler so we can capture third-party and internal java.util.logging.Logger API calls
            LogManager.getLogManager().getLogger("").addHandler(julHandler);
            java.util.logging.Logger.getLogger("").setLevel(Level.ALL);

            Logger.context = context;

            // now that we have a context, let's set the fileLoggerInstance properly unless it was already set by tests
            if (fileLoggerInstance == null || fileLoggerInstance instanceof FileLogger) {
                FileLogger.setContext(context);
                fileLoggerInstance = FileLogger.getInstance();
            }

            SharedPreferences prefs = Logger.context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE);

            // level
            if (null != level) {  // someone called setLevel method before setContext
                setLevelSync(level);  // seems redundant, but we do this to save to SharedPrefs now that we have Context
            } else {  // set it to the SharedPrefs value, or DEFAULT if no value in SharedPrefs yet
                setLevelSync(LEVEL.fromString (prefs.getString (SHARED_PREF_KEY_level, getLevelDefault().toString())));
            }

            // logFileMaxSize
            if (null != logFileMaxSize) {  // someone called setMaxStoreSize method before setContext
                setMaxStoreSize(logFileMaxSize);  // seems redundant, but we do this to save to SharedPrefs now that we have Context
            } else {  // set it to the SharedPrefs value, or DEFAULT if no value in SharedPrefs yet
                setMaxStoreSize(prefs.getInt(SHARED_PREF_KEY_logFileMaxSize, DEFAULT_logFileMaxSize));
            }

            // capture
            if (null != capture) {  // someone called setCapture method before setContext
                setCaptureSync(capture);  // seems redundant, but we do this to save to SharedPrefs now that we have Context
            } else {  // set it to the SharedPrefs value, or DEFAULT if no value in SharedPrefs yet
                setCaptureSync(prefs.getBoolean (SHARED_PREF_KEY_logPersistence, DEFAULT_capture));
            }

            // filters
            if (null != filters) {  // someone called setFilters method before setContext
                setFiltersSync(filters);  // seems redundant, but we do this to save to SharedPrefs now that we have Context
            } else {  // set it to the SharedPrefs value, or DEFAULT if no value in SharedPrefs yet
                try {
                    setFiltersSync(JSONObjectToHashMap(new JSONObject(prefs.getString (SHARED_PREF_KEY_filters, "{}"))));
                } catch (JSONException e) {
                    // not possible
                }
            }

            uncaughtExceptionHandler = new UncaughtExceptionHandler ();
            Thread.setDefaultUncaughtExceptionHandler (uncaughtExceptionHandler);
        }
    }


    /**
     * Set the level and above at which log messages should be saved/printed.
     * For example, passing LEVEL.INFO will log INFO, WARN, ERROR, and FATAL.  A
     * null parameter value is ignored and has no effect.
     *
     * @param desiredLevel @see LEVEL
     */
    static public void setLevel(final LEVEL desiredLevel) {
        ThreadPoolWorkQueue.execute(new Runnable() {
            @Override public void run() {
                setLevelSync(desiredLevel);
                // we do this mostly to enable unit tests to logger.wait(100) instead of
                // Thread.sleep(100) -- it's faster, more stable, and more deterministic that way
                synchronized (WAIT_LOCK) {
                    WAIT_LOCK.notifyAll ();
                }
            }
        });
    }

    private synchronized static void setLevelSync(final LEVEL desiredLevel) {
        // to avoid thread deadlocks, we have this method that can be called within a thread that is already on the work queue
        if (null == desiredLevel) {
            return;
        }
        level = desiredLevel;
        if (null != context) {
            SharedPreferences prefs = context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE);
            prefs.edit ().putString (SHARED_PREF_KEY_level, level.toString ()).commit();
            // we still processed the setCapture call, but when SHARED_PREF_KEY_level_from_server is present, it is used for the level field value (it's an override)
            level = LEVEL.fromString(prefs.getString(SHARED_PREF_KEY_level_from_server, level.toString()));
        }
    }

    /**
     * Get the current Logger.LEVEL.
     *
     * @return Logger.LEVEL
     */
    static public LEVEL getLevel() {
        final Future<LEVEL> task = ThreadPoolWorkQueue.submit(new Callable<LEVEL>() {
            @Override public LEVEL call() {
                return getLevelSync();
            }
        });

        try {
            return task.get();
        }
        catch (Exception e) {
            return getLevelSync();
        }
    }

    static synchronized private LEVEL getLevelSync() {
        // to avoid thread deadlocks, we have this method that can be called within a thread that is already on the work queue
        LEVEL returnValue = (null == level) ? getLevelDefault() : level;
        if (null != context) {
            SharedPreferences prefs = context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE);
            // use the override
            returnValue = LEVEL.fromString(prefs.getString(SHARED_PREF_KEY_level_from_server, returnValue.toString()));
        }
        return returnValue;
    }

    /**
     * Global setting: turn persisting of log data passed to this class's log methods on or off.
     *
     * @param capture flag to indicate if log data should be saved persistently
     */
    static public void setCapture(final boolean capture) {
        ThreadPoolWorkQueue.execute(new Runnable() {
            @Override public void run() {
                setCaptureSync(capture);
                // we do this mostly to enable unit tests to logger.wait(100) instead of
                // Thread.sleep(100) -- it's faster, more stable, and more deterministic that way
                synchronized (WAIT_LOCK) {
                    WAIT_LOCK.notifyAll ();
                }
            }
        });
    }

    static synchronized private void setCaptureSync(final boolean capture) {
        // to avoid thread deadlocks, we have this method that can be called within a thread that is already on the work queue
        Logger.capture = capture;
        if (null != context) {
            SharedPreferences prefs = context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE);
            prefs.edit ().putBoolean (SHARED_PREF_KEY_logPersistence, Logger.capture).commit();
            // we still processed the setCapture call, but when SHARED_PREF_KEY_logPersistence_from_server is present, it is used for the capture field value (it's an override)
            Logger.capture = prefs.getBoolean(SHARED_PREF_KEY_logPersistence_from_server, Logger.capture);
        }
    }

    /**
     * Get the current value of the capture flag, indicating that the Logger is recording log calls persistently.
     *
     * @return current value of capture flag
     */
    static public boolean getCapture() {
        final Future<Boolean> task = ThreadPoolWorkQueue.submit(new Callable<Boolean>() {
            @Override public Boolean call() {
                return getCaptureSync();
            }
        });

        try {
            return task.get();
        }
        catch (Exception e) {
            return getCaptureSync();
        }
    }

    static synchronized private boolean getCaptureSync() {
        // to avoid thread deadlocks, we have this method that can be called within a thread that is already on the work queue
        boolean returnValue = (null == capture) ? DEFAULT_capture : capture;
        if (null != context) {
            SharedPreferences prefs = context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE);
            // use the override
            returnValue = prefs.getBoolean(SHARED_PREF_KEY_logPersistence_from_server, returnValue);
        }
        return returnValue;
    }

    /**
     * @exclude
     * Global setting: turn persisting of analytics data passed to this class's analytics methods on or off.
     *
     * @param capture flag to indicate if analytics data should be saved persistently
     */
    static public void setAnalyticsCapture(final boolean capture) {
        ThreadPoolWorkQueue.execute(new Runnable() {
            @Override public void run() {
                setAnalyticsCaptureSync(capture);
                // we do this mostly to enable unit tests to logger.wait(100) instead of
                // Thread.sleep(100) -- it's faster, more stable, and more deterministic that way
                synchronized (WAIT_LOCK) {
                    WAIT_LOCK.notifyAll ();
                }
            }
        });
    }

    static synchronized private void setAnalyticsCaptureSync(final boolean capture) {
        // to avoid thread deadlocks, we have this method that can be called within a thread that is already on the work queue
        analyticsCapture = capture;
    }

    /**
     * @exclude
     * Get the current value of the analyticsCapture flag, indicating that the Logger is recording analytics calls persistently.
     *
     * @return current value of analyticsCapture flag
     */
    static public boolean getAnalyticsCapture() {
        final Future<Boolean> task = ThreadPoolWorkQueue.submit(new Callable<Boolean>() {
            @Override public Boolean call() {
                return getAnalyticsCaptureSync();
            }
        });

        try {
            return task.get();
        }
        catch (Exception e) {
            return getAnalyticsCaptureSync();
        }
    }

    static synchronized private boolean getAnalyticsCaptureSync() {
        // to avoid thread deadlocks, we have this method that can be called within a thread that is already on the work queue
        boolean returnValue = (null == analyticsCapture) ? DEFAULT_analyticsCapture : analyticsCapture;
        return returnValue;
    }

    /**
     * Filter on packages at and above designated LEVEL.  This is a white list.  Any package not listed in the filters will not be logged.
     * Pass null or an empty HashMap parameter to remove filters and resume logging at the default LEVEL.
     *
     * @param filters set of filters and associated level (and above) to allow for logging
     */
    static public void setFilters(final HashMap<String, LEVEL> filters) {
        ThreadPoolWorkQueue.execute(new Runnable() {
            @Override public void run() {
                setFiltersSync(filters);
                // we do this mostly to enable unit tests to logger.wait(100) instead of
                // Thread.sleep(100) -- it's faster, more stable, and more deterministic that way
                synchronized (WAIT_LOCK) {
                    WAIT_LOCK.notifyAll ();
                }
            }
        });
    }

    private static void setFiltersSync(final HashMap<String, LEVEL> filters) {
        // to avoid thread deadlocks, we have this method that can be called within a thread that is already on the work queue
        Logger.filters = filters;
        if (null != context) {
            SharedPreferences prefs = context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE);
            prefs.edit ().putString (SHARED_PREF_KEY_filters, HashMapToJSONObject(Logger.filters).toString ()).commit();
            // we still processed the setFilters call, but when SHARED_PREF_KEY_filters_from_server is present, it is used for the filters field value (it's an override)
            try {
                Logger.filters = JSONObjectToHashMap(new JSONObject(prefs.getString (SHARED_PREF_KEY_filters_from_server, HashMapToJSONObject(Logger.filters).toString ())));
            } catch (JSONException e) {
                // not possible
            }
        }
    }

    /**
     * Get the current list of filters.
     *
     * @return map of white list package filters and the designated LEVEL
     */
    static public HashMap<String, LEVEL> getFilters() {
        final Future<HashMap<String, LEVEL>> task = ThreadPoolWorkQueue.submit(new Callable<HashMap<String, LEVEL>>() {
            @Override public HashMap<String, LEVEL> call() {
                return getFiltersSync();
            }
        });

        try {
            return task.get();
        }
        catch (Exception e) {
            return getFiltersSync();
        }
    }

    static synchronized private HashMap<String, LEVEL> getFiltersSync() {
        // to avoid thread deadlocks, we have this method that can be called within a thread that is already on the work queue
        HashMap<String, LEVEL> returnValue = filters;
        if (null != context) {
            SharedPreferences prefs = context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE);
            // use the override
            try {
                returnValue = JSONObjectToHashMap(new JSONObject(prefs.getString (SHARED_PREF_KEY_filters_from_server, HashMapToJSONObject(returnValue).toString ())));
            } catch (JSONException e) {
                // not possible
            }
        }
        return returnValue;
    }


    /**
     * Set the maximum size of the local log file.  Once the maximum file size is reached,
     * no more data will be appended.  Consider that this file is sent to a server.
     *
     * @param bytes maximum size of the file in bytes, minimum 10000
     */
    static public void setMaxStoreSize(final int bytes) {
        // TODO: also check if bytes is bigger than remaining disk space?
        if (bytes >= 10000) {
            logFileMaxSize = bytes;
        }
        if (null != context) {
            SharedPreferences prefs = context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE);
            prefs.edit ().putInt (SHARED_PREF_KEY_logFileMaxSize, logFileMaxSize).commit();
        }
    }

    /**
     * Get the current setting for the max file size threshold.
     *
     * @return current max file size threshold
     */
    static public int getMaxStoreSize() {
        return (null == logFileMaxSize) ? DEFAULT_logFileMaxSize : logFileMaxSize;
    }

    /**
     * Send the accumulated log data when the persistent log buffer exists and is not empty.  The data
     * accumulates in the log buffer from the use of {@link com.ibm.bms.clientsdk.android.logger.api.Logger} with capture
     * (see {@link com.ibm.bms.clientsdk.android.logger.api.Logger#setCapture(boolean)}) turned on.
     *
     */
    static public void send () {
        send(null);
    }

    /**
     * See {@link import Logger#send()}
     *
     * @param listener ResponseListener which specifies an onSuccess callback and an onFailure callback (see {@link ResponseListener})
     */
    static public void send (ResponseListener listener) {
        if(sendingLogs){
            return;
        }
        else{
            sendingLogs = true;
            sendFiles(Logger.FILENAME, listener);
        }
    }

    /**
     * @exclude
     * Send the accumulated log data when the persistent log buffer exists and is not empty.  The data
     * accumulates in the log buffer from the use of {@link com.ibm.bms.clientsdk.android.logger.api.Logger} with capture
     * (see {@link com.ibm.bms.clientsdk.android.logger.api.Logger#setAnalyticsCapture(boolean)}) turned on.
     *
     * @param listener ResponseListener which specifies an onSuccess callback and an onFailure callback
     */
    static public void sendAnalytics (ResponseListener listener) {
        if(sendingAnalyticsLogs){
            return;
        }
        else{
            sendingAnalyticsLogs = true;

            sendFiles(Logger.ANALYTICS_FILENAME, listener);
        }
    }

    /**
     * Ask the Logger if an uncaught exception, which often appears to the user as a crashed app, is present in the persistent capture buffer.
     * This method should not be called before {@link #setContext(Context)}.  If it is called too early, an error message is issued and false is returned.
     *
     * @return boolean if an uncaught exception log entry is currently in the persistent log buffer
     */
    static public boolean isUnCaughtExceptionDetected () {
        if (context == null) {
            if (!context_null_msg_already_printed) {
                Log.w(LOG_PACKAGE_NAME, CONTEXT_NULL_MSG);
                context_null_msg_already_printed = true;
            }
            return false;
        }
        return context.getSharedPreferences (SHARED_PREF_KEY, Context.MODE_PRIVATE).getBoolean (SHARED_PREF_KEY_CRASH_DETECTED, false);
    }



    //region Logger API - instance methods

    /**
     * Get the package name for this Logger. This can be passed to the {@link #setFilters(HashMap)} method.
     *
     * @return The package name for this instance of Logger
     */
    public String getPackageName() {
        return this.packageName;
    }

    /**
     * @exclude
     *
     * Log at ANALYTICS level.
     *
     * @param message the message to log
     */
    public void analytics(final String message, JSONObject additionalMetadata) {
        doLog(LEVEL.ANALYTICS, message, (new Date()).getTime(), null);
    }

    /**
     * Log at FATAL level.
     *
     * @param message the message to log
     */
    public void fatal(final String message) {
        fatal(message, null);
    }

    /**
     * Log at FATAL level.
     *
     * @param message the message to log
     * @param t a Throwable whose stack trace is converted to string and logged, and passed through as-is to android.util.Log
     */
    public void fatal(final String message, final Throwable t) {
        doLog(LEVEL.FATAL, message, (new Date()).getTime(), t);
    }

    /**
     * Log at ERROR level.
     *
     * @param message the message to log
     */
    public void error(final String message) {
        error(message, null);
    }

    /**
     * Log at ERROR level.
     *
     * @param message the message to log
     * @param t a Throwable whose stack trace is converted to string and logged, and passed through as-is to android.util.Log
     */
    public void error(final String message, final Throwable t) {
        doLog(LEVEL.ERROR, message, (new Date()).getTime(), t);
    }

    /**
     * Log at WARN level.
     *
     * @param message
     */
    public void warn(final String message) {
        warn(message, null);
    }

    /**
     * Log at WARN level.
     *
     * @param message the message to log
     * @param t a Throwable whose stack trace is converted to string and logged, and passed through as-is to android.util.Log
     */
    public void warn(final String message, final Throwable t) {
        doLog(LEVEL.WARN, message, (new Date()).getTime(), t);
    }

    /**
     * Log at INFO level.
     *
     * @param message the message to log
     */
    public void info(final String message) {
        info(message, null);
    }

    /**
     * Log at INFO level.
     *
     * @param message the message to log
     * @param t a Throwable whose stack trace is converted to string and logged, and passed through as-is to android.util.Log
     */
    public void info(final String message, final Throwable t) {
        doLog(LEVEL.INFO, message, (new Date()).getTime(), t);
    }

    /**
     * Log at DEBUG level.
     *
     * @param message the message to log
     */
    public void debug(final String message) {
        debug(message, null);
    }

    /**
     * Log at DEBUG level.
     *
     * @param message the message to log
     * @param t a Throwable whose stack trace is converted to string and logged, and passed through as-is to android.util.Log
     */
    public void debug(final String message, final Throwable t) {
        doLog(LEVEL.DEBUG, message, (new Date()).getTime(), t);
    }

    /**
     * @exclude
     *
     * All log calls flow through here.  Use this method when you want to control the timestamp, attach additional metadata,
     * and attach a Throwable's call stack to the log output.
     *
     * @param calledLevel specify the Logger.LEVEL (a null parameter results in no log entry)
     * @param message (optional) the data for the log entry
     * @param timestamp the number of milliseconds since January 1, 1970, 00:00:00 GMT
     * @param t (optional) an Exception or Throwable, may be null
     */
    public void doLog(final LEVEL calledLevel, String message, final long timestamp, final Throwable t) {
        // we do this outside of the thread, otherwise we can't find the caller to attach the call stack metadata
        JSONObject metadata = generateStackMetadata();
        ThreadPoolWorkQueue.execute(new DoLogRunnable(calledLevel, message, timestamp, metadata, t, this));
    }

    //endregion


    // private utility methods


    // if we have callstack metadata, prepend it to the message
    private static String prependMetadata (String message, JSONObject metadata) {
        try {
            if (null != metadata) {
                String clazz = "";
                String method = "";
                String file = "";
                String line = "";
                if (metadata.has ("$class")) {
                    clazz = metadata.getString ("$class");
                    clazz = clazz.substring (clazz.lastIndexOf ('.') + 1, clazz.length ());
                }
                if (metadata.has ("$method")) {
                    method = metadata.getString ("$method");
                }
                if (metadata.has ("$file")) {
                    file = metadata.getString ("$file");
                }
                if (metadata.has ("$line")) {
                    line = metadata.getString ("$line");
                }
                if (!(clazz + method + file + line).equals ("")) {
                    // we got something...
                    message = clazz + "." + method + " in " + file + ":" + line + " :: " + message;
                }
            }
        } catch (Exception e) {
            // ignore... it's best effort anyway
        }
        return message;
    }

    /**
     * Get stack trace caused by Logger exceptions
     *
     * @return Returns the JSONObject with stack metadata
     */
    private JSONObject generateStackMetadata() {

        JSONObject jsonMetadata = new JSONObject();

        try {
            // try/catch Exception wraps all because I don't know yet if I can trust getStackTrace... needs more testing
            // below is slightly more performant than: Thread.currentThread().getStackTrace();
            StackTraceElement[] stackTraceElements = new Exception().getStackTrace();

            int index = 0;
            // find the start of the Logger call stack:
            while(!stackTraceElements[index].getClassName ().equals (Logger.class.getName())) {
                index++;
            }
            // then find the caller:
            while(stackTraceElements[index].getClassName().equals (Logger.class.getName())
                    || stackTraceElements[index].getClassName().startsWith (JULHandler.class.getName())
                    || stackTraceElements[index].getClassName().startsWith (java.util.logging.Logger.class.getName())
                    || stackTraceElements[index].getClassName().startsWith (MFPAnalytics.class.getName())) {
                index++;
            }

            jsonMetadata.put ("$class", stackTraceElements[index].getClassName ());
            jsonMetadata.put ("$file", stackTraceElements[index].getFileName ());
            jsonMetadata.put ("$method", stackTraceElements[index].getMethodName ());
            jsonMetadata.put ("$line", stackTraceElements[index].getLineNumber ());
            jsonMetadata.put ("$src", "java");

        } catch (Exception e) {
            Log.e(LOG_PACKAGE_NAME, "Could not generate jsonMetadata object.", e);
        }

        return jsonMetadata;
    }

    /**
     * @exclude
     *
     * for filters, public.
     */
    public static HashMap<String, LEVEL> JSONObjectToHashMap(JSONObject object) {
        HashMap<String, LEVEL> pairs = new HashMap<String, LEVEL>();
        @SuppressWarnings("rawtypes")
        Iterator it = object.keys();
        while (it.hasNext()) {
            String n = (String) it.next();
            try {
                pairs.put(n, LEVEL.valueOf(object.getString(n).toUpperCase()));
            } catch (JSONException e) {
                // not possible
            }
        }
        return pairs;
    }

    /**
     * @exclude
     *
     * for filters, public.
     */
    public static JSONObject HashMapToJSONObject(HashMap<String, LEVEL> pairs) {
        if(pairs == null){
            return new JSONObject();
        }

        Set<String> set = pairs.keySet();
        JSONObject jsonObj = new JSONObject();
        @SuppressWarnings("rawtypes")
        Iterator it = set.iterator();
        while (it.hasNext()) {
            String n = (String) it.next();
            try {
                jsonObj.put(n, pairs.get(n).toString());
            } catch (JSONException e) {
                // not possible
            }
        }
        return jsonObj;
    }

    /**
     * @exclude
     */
    static public LEVEL getLevelDefault() {
        if (context == null) {
            return LEVEL.DEBUG;
        }

        X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");	// default debug common name
        PackageManager packageManager = context.getPackageManager();
        boolean debug = false;

        try {
            Signature raw = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures[0];
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(raw.toByteArray()));
            debug = cert.getSubjectX500Principal().equals(DEBUG_DN);
        } catch (Exception e) {
            debug = false;
        }
        return (debug) ? LEVEL.DEBUG : LEVEL.FATAL;
    }

    /**
     * Will create JSONObject with the passed parameters, and other relevant information.  See class-level documentation.
     *
     * @param level
     * @param message
     * @param t a Throwable whose stack trace is converted to string and logged, and passed through as-is to android.util.Log
     * @return a JSONObject as shown in the class introduction
     */
    private static JSONObject createJSONObject(final LEVEL level, final String pkg, final String message, long timestamp, final JSONObject jsonMetadata, final Throwable t) {
        JSONObject jsonObject = new JSONObject();
        SimpleDateFormat s = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:S");
        s.setTimeZone(TimeZone.getTimeZone("GMT"));
        String format = s.format(timestamp);
        try {
            jsonObject.put ("timestamp", format);
            jsonObject.put ("level", level.toString());
            jsonObject.put ("pkg", pkg);
            jsonObject.put ("msg", message);
            jsonObject.put ("threadid", Thread.currentThread ().getId());
            if (null != jsonMetadata) {
                jsonObject.put ("metadata", jsonMetadata);
            }
            if (null != t) {
                jsonObject.put ("metadata", appendFullStackTrace(jsonMetadata, t));
            }
        }
        catch (JSONException e) {
            Log.e(LOG_PACKAGE_NAME, "Error adding JSONObject key/value pairs", e);
        }
        return jsonObject;

    }

    /**
     * append the full stacktrace to a $stacktrace key JSONArray in the passed jsonMetadata object
     * and return it.
     */
    private static JSONObject appendFullStackTrace (JSONObject jsonMetadata, Throwable t) {
        JSONArray stackArray = new JSONArray();
        StackTraceElement[] stackTraceElements = t.getStackTrace ();
        for (int i = 0; i < stackTraceElements.length; i++) {
            stackArray.put (stackTraceElements[i].toString());
        }

        try {
            if (null == jsonMetadata) {
                jsonMetadata = new JSONObject();
            }
            jsonMetadata.put("$stacktrace", stackArray);
            jsonMetadata.put ("$exceptionMessage", t.getLocalizedMessage ());
            jsonMetadata.put ("$exceptionClass", t.getClass().getName());
        }
        catch (JSONException e) {
            // ignore.  getting the stacktrace is best effort
        }
        return jsonMetadata;
    }


    /**
     * We only persist (append) to the log file if the passed jsonObject parameter has data,
     * the Logger capture flag is set to true, and the log file size is less than
     * FILE_SIZE_LOG_THRESHOLD.
     *
     * @param jsonObject the object to write into the log file.
     */
    private synchronized static void captureToFile (final JSONObject jsonObject, LEVEL calledLevel) {
        boolean cap = getCaptureSync();
        boolean analyticsCap = getAnalyticsCaptureSync();

        if (context == null) {
            if (!context_null_msg_already_printed) {
                Log.w(LOG_PACKAGE_NAME, CONTEXT_NULL_MSG);
                context_null_msg_already_printed = true;
            }
            return;
        }

        if (jsonObject.length() == 0) {
            return;
        }

        try {
            // Determine whether is needs to go to analytics or logger
            if (analyticsCap && calledLevel.equals(LEVEL.ANALYTICS)) {
                fileLoggerInstance.log(jsonObject, ANALYTICS_FILENAME);
            } else if (cap) {
                fileLoggerInstance.log(jsonObject, FILENAME);
            }
        } catch (Exception e) {
            Log.e(LOG_PACKAGE_NAME, "An error occurred capturing data to file.", e);
        }
    }

    private static class DoLogRunnable implements Runnable {

        private LEVEL calledLevel;
        private String message;
        private long timestamp;
        private JSONObject metadata;
        private Throwable t;
        private Logger logger;

        public DoLogRunnable(final LEVEL calledLevel, final String message, final long timestamp, final JSONObject metadata, final Throwable t, final Logger logger) {
            this.calledLevel = calledLevel;
            this.message = message;
            this.timestamp = timestamp;
            this.metadata = metadata;
            this.t = t;
            this.logger = logger;
        }

        @Override
        public void run() {

            boolean canLog = true;

            HashMap<String, LEVEL> currentFilters = getFiltersSync();

            // honor the filters as a whitelist, if present
            if (currentFilters != null && currentFilters.size() > 0) {
                if (currentFilters.containsKey(logger.packageName)) {
                    canLog = calledLevel.getLevelValue() <= currentFilters.get(logger.packageName).getLevelValue();
                } else {
                    canLog = false;
                }
            } else {
                canLog = ((calledLevel != null) && calledLevel.isLoggable());
            }

            if (canLog || (calledLevel == LEVEL.ANALYTICS)) {
                Logger.captureToFile(Logger.createJSONObject(calledLevel, logger.packageName, message, timestamp, metadata, t), calledLevel);
                message = (null == message) ? "(null)" : message;  // android.util.Log can't handle null, so protect it
                message = Logger.prependMetadata (message, metadata);
                switch (calledLevel) {
                    case FATAL:
                    case ERROR:
                        if (null == t) { Log.e(logger.packageName, message); } else { Log.e(logger.packageName, message, t); };
                        break;
                    case WARN:
                        if (null == t) { Log.w(logger.packageName, message); } else { Log.w(logger.packageName, message, t); };
                        break;
                    case INFO:
                        if (null == t) { Log.i(logger.packageName, message); } else { Log.i(logger.packageName, message, t); };
                        break;
                    case DEBUG:
                        if (null == t) { Log.d(logger.packageName, message); } else { Log.d(logger.packageName, message, t); };
                        break;
                    default:
                        break;
                }
            }
            // we do this mostly to enable unit tests to logger.wait(100) instead of
            // Thread.sleep(100) -- it's faster, more stable, and more deterministic that way
            synchronized(logger) {
                logger.notifyAll ();
            }
        }
    }


    /**
     * @exclude
     *
     * This is mostly to enable unit test to avoid the filesystem.  fileLoggerInstance may be
     * a mock injected by LoggerTest, for example.
     *
     */
    public static byte[] getByteArrayFromFile(File file) throws UnsupportedEncodingException {
        return (fileLoggerInstance == null) ? new byte[]{} : fileLoggerInstance.getFileContentsAsByteArray(file);
    }

    /**
     * @exclude
     */
    private synchronized static void sendFiles(String fileName, ResponseListener listener) {

        if (context == null) {
            // no need to log
            return;
        }

        // java.util.logging.FileHandler can roll over.
        // We should send the oldest logs first
        for (int i = Logger.MAX_NUM_LOG_FILES - 1; i > -1; i--) {
            File file = new File(context.getFilesDir (), fileName + "." + i);
            if (file.length() > 0) {

				/*
				 * Read the file contents, send the data to the server, and
				 * delete the file upon successful receipt by the server.
				 *
				 * The logic here is to allow multi-threaded apps to continue writing to [FILE]:
				 *
				 * if ([FILE].send exists due to previous failure to send) {
				 *     currentFilename = [FILE].send
				 * } else {
				 *     move [FILE] to [FILE].send
				 *     currentFilename = [FILE].send
				 * }
				 * send currentFilename
				 * if (send success)
				 *     delete currentFilename
				 * else
				 *     keep currentFilename
				 */

                // Use a temporary file to allow multi-threads to continue to write to the
                // original log file, so we don't lose log data by wiping out a log file that
                // has accumulated more log entries during our attempt to send in this thread.
                File fileToSend = new File(file + ".send");
                if (!fileToSend.exists()) {
                    Logger.getInstance(LOG_PACKAGE_NAME).debug("Moving " + file + " to " + fileToSend);
                    file.renameTo (fileToSend);
                }

                boolean isAnalyticsRequest = fileName.equalsIgnoreCase(Logger.ANALYTICS_FILENAME);

                String logUploadPath = "/imfmobileanalytics/v1/receiver/apps/";
                String logUploaderURL = BMSClient.getInstance().getBackendRoute() + logUploadPath + BMSClient.getInstance().getBackendGUID();

                SendLogsRequestListener requestListener = new SendLogsRequestListener(fileToSend, listener, isAnalyticsRequest, logUploaderURL);

                try {
                    byte[] payload = Logger.getByteArrayFromFile(fileToSend);
                    if (payload.length == 0) {  // I doubt this would ever happen, but just in case...
                        return;  // don't bother sending empty string; return now
                    }

                    MFPRequest sendLogsRequest = new MFPRequest(logUploaderURL, ResourceRequest.POST);

                    sendLogsRequest.addHeader("Content-Type","application/json");

                    sendLogsRequest.send(payload, requestListener);
                } catch (IOException e) {
                    Logger.getInstance(LOG_PACKAGE_NAME).error("Failed to send logs due to exception.", e);
                }

            }
        }
    }

    /**
     * @exclude
     */
    static class SendLogsRequestListener implements ResponseListener {

        private static final Logger logger = Logger.getInstance(SendLogsRequestListener.class.getName());

        private final File file;

        private ResponseListener userDefinedListener;

        private boolean isAnalyticsRequest = false;

        private String url = "";

        public SendLogsRequestListener(File file, ResponseListener userDefinedListener, boolean isAnalyticsRequest, String url) {
            super();
            this.file = file;
            this.userDefinedListener = userDefinedListener;

            this.isAnalyticsRequest = isAnalyticsRequest;
            this.url = url;
        }

        @Override
        public void onSuccess(Response response) {
            try{

                // regardless of success or failure, reaching this code indicates we successfully communicated with the WL server (we got a reply).
                // Thus, we should delete:
                file.delete ();

                if(response.getStatus() == 201){
                    logger.debug("Successfully POSTed log data from file " + file + " to URL " + url + ".  HTTP response code: " + response.getStatus());

                    if (userDefinedListener != null) {
                        userDefinedListener.onSuccess(response);
                    }
                }
                else{
                    logger.error("Failed to POST data from file " + file + " due to: HTTP response code: " + response.getStatus());

                    if (userDefinedListener != null) {
                        userDefinedListener.onFailure(new FailResponse(FailResponse.ErrorCode.SERVER_ERROR, response), null);
                    }
                }

                // remove the crash detection marker since we just attempted to send the log to the server
                context.getSharedPreferences (Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE).edit ().putBoolean (Logger.SHARED_PREF_KEY_CRASH_DETECTED, false).commit();
            }
            finally{
                //Turn off the send flag:
                if(isAnalyticsRequest){
                    sendingAnalyticsLogs = false;
                }
                else{
                    sendingLogs = false;
                }

                // we do this for testing, so unit test code can deterministically stop waiting, and don't have to Thread.sleep
                synchronized(WAIT_LOCK) {
                    WAIT_LOCK.notifyAll ();
                }
            }
        }

        @Override
        public void onFailure(FailResponse response, Throwable t) {

            if (userDefinedListener != null) {
                userDefinedListener.onFailure(response, t);
            }

            //Turn off the send flag:
            if(isAnalyticsRequest){
                sendingAnalyticsLogs = false;
            }
            else{
                sendingLogs = false;
            }

            // we do this for testing, so unit test code can deterministically stop waiting, and don't have to Thread.sleep
            synchronized(WAIT_LOCK) {
                WAIT_LOCK.notifyAll ();
            }
        }

    }

}
