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
import android.util.Log;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.internal.LogPersisterInterface;

import org.json.JSONObject;

import java.util.Date;
import java.util.WeakHashMap;

/**
 * Logger is an abstraction of, and pass through to, android.util.Log.  Logger provides some
 * enhanced capability such as capturing log calls, filtering by logger name, and log level control at
 * both global and individual logger scope.  It also provides a method call to send captured logs to
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
 *   "name"         : "your_logger_name",         // can be anything, typically a class name, app name, or JavaScript object name
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
    public static final String INTERNAL_PREFIX = "mfpsdk.";

    private static LEVEL level = LEVEL.FATAL; //Defaulting to fatal since we can't check if the app has been signed or not without the context.

    // Track instances so we give back the same one for the same logger name passed to getInstance method.
    // We use a WeakHashMap because some instances in this map may go out of scope
    // very soon after instantiation, thus no reason to keep a strong reference, and let
    // the garbage collector do its job.
    private static WeakHashMap<String, Logger> instances = new WeakHashMap<>();

    private final String name;

    //Use this flag to determine if internal debug and info logs should be output to Logcat or not:
    protected static boolean internalDebugLoggingEnabled = false;

    protected static LogPersisterInterface logPersister = null;

    /**
     * Levels supported in this Logger class.
     */
    public enum LEVEL {
        /**
         * @exclude
         */
        ANALYTICS { @Override protected int getLevelValue() {return  25;}; }, // Specifies data needs to go to analytics file
        FATAL	  { @Override protected int getLevelValue() {return  50;}; },
        ERROR     { @Override protected int getLevelValue() {return 100;}; },
        WARN      { @Override protected int getLevelValue() {return 200;}; },
        INFO      { @Override protected int getLevelValue() {return 300;}; },
        DEBUG     { @Override protected int getLevelValue() {return 400;}; };

        protected abstract int getLevelValue();

        public boolean isLoggable() {  // call like this:  LEVEL.WARN.isLoggable(), which checks against the global level object
            LEVEL currentLevel = getLogLevel();
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

    // private, for factory creation of Logger objects
    private Logger(final String name) {
        this.name = (name == null || name.trim().equals ("")) ? "NONE" : name.trim();
    }

    /**
     * Get the Logger for the given name.
     *
     * @param name the tag that should be printed with log messages.  The value is passed
     *        through to android.util.Log and persistently recorded when log capture is enabled.
     * @return the Logger for the given name
     */
    static synchronized public Logger getLogger(final String name) {
        Logger logger = instances.get(name); {
            if (null == logger) {
                logger = new Logger(name);
                instances.put (name,  logger);
            }
        }
        return logger;
    }

    /**
     * Set the level and above at which log messages should be saved/printed.
     * For example, passing LEVEL.INFO will log INFO, WARN, ERROR, and FATAL.  A
     * null parameter value is ignored and has no effect.
     *
     * @param desiredLevel @see LEVEL
     */
    static public void setLogLevel(final LEVEL desiredLevel) {
        //No configuration persistence unless the Analytics SDK has been included:
        if(logPersister == null){
            level = desiredLevel;
        }
        else{
            logPersister.setLogLevel(desiredLevel);
        }
    }

    /**
     * Get the current Logger.LEVEL.
     *
     * @return Logger.LEVEL
     */
    static public LEVEL getLogLevel() {
        //No configuration persistence unless the Analytics SDK has been included:
        if(logPersister == null){
            return level;
        }
        else{
            return logPersister.getLogLevel();
        }
    }

    /**
     * Global setting: turn persisting of log data passed to this class's log methods on or off.
     *
     * @param shouldStoreLogs flag to indicate if log data should be saved persistently
     */
    static public void storeLogs(final boolean shouldStoreLogs) {
        //No-op unless the Analytics SDK has been included:
        if(logPersister != null){
            logPersister.storeLogs(shouldStoreLogs);
        }
    }

    /**
     * Determine if logs are currently being stored.
     *
     * @return true if logs are being stored
     */
    static public boolean isStoringLogs() {
        //No-op unless the Analytics SDK has been included:
        if(logPersister == null){
            return false;
        }
        else{
            return logPersister.isStoringLogs();
        }
    }

    /**
     * Set the maximum size of the local log file.  Once the maximum file size is reached,
     * no more data will be appended.  Consider that this file is sent to a server.
     *
     * @param bytes maximum size of the file in bytes, minimum 10000
     */
    static public void setMaxLogStoreSize(final int bytes) {
        //No-op unless the Analytics SDK has been included:
        if(logPersister != null){
            logPersister.setMaxLogStoreSize(bytes);
        }
    }

    /**
     * Get the current setting for the max file size threshold.
     *
     * @return current max file size threshold
     */
    static public int getMaxLogStoreSize() {
        //No-op unless the Analytics SDK has been included:
        if(logPersister == null){
            return -1;
        }
        else{
            return logPersister.getMaxLogStoreSize();
        }
    }

    /**
     * Enable displaying all Bluemix Mobile Services SDK debug logs in Logcat. By default, no debug messages are displayed.
     * @param enabled Determines whether to display Bluemix Mobile Services SDK debug logs in Logcat.
     */
    public static void setSDKDebugLoggingEnabled(boolean enabled){
        internalDebugLoggingEnabled = enabled;
    }

    /**
     * Check if displaying all Bluemix Mobile Services SDK debug logs in Logcat is enabled.
     * @returns true if debug logging is enabled for Bluemix Mobile Services SDK
     */
    public static boolean isSDKDebugLoggingEnabled(){
        return internalDebugLoggingEnabled;
    }

    /*
     * @exclude
     *
     * Determine if the given logger instance is internal.
     *
     * @param logger the logger instance to be determined if it is internal or not
     * @return true if internal
     */
    public static boolean isInternalLogger(Logger logger){
        return logger.getName().startsWith(INTERNAL_PREFIX);
    }

    /*
     * @exclude
     *
     * Set a new Logger for this class. To be used by MFPAnalytics.init() to add persistence functionality to this Logger.
     *
     * @param newLogger the new Logger
     */
    public static void setLogPersister(LogPersisterInterface newLogPersister){
        logPersister = newLogPersister;
    }

    /**
     * Send the accumulated log data when the persistent log buffer exists and is not empty.  The data
     * accumulates in the log buffer from the use of {@link com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger}
     * with log storage turned on (see {@link com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger#storeLogs(boolean)}).
     *
     */
    static public void send () {
        send(null);
    }

    /**
     * See {@link #send()}
     *
     * @param listener {@link com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener} which specifies a success and failure callback
     */
    static public void send (ResponseListener listener) {
        //No-op unless the Analytics SDK has been included:
        if(logPersister != null){
            logPersister.send();
        }
    }

    /**
     * Ask the Logger if an uncaught exception, which often appears to the user as a crashed app, is present in the persistent capture buffer.
     * This method should not be called after calling {@link com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient#initialize(Context, String, String, String)}.  If it is called too early, an error message is issued and false is returned.
     *
     * @return boolean if an uncaught exception log entry is currently in the persistent log buffer
     */
    static public boolean isUnCaughtExceptionDetected () {
        //No-op unless the Analytics SDK has been included:
        if(logPersister == null){
            return false;
        }
        else{
            return logPersister.isUncaughtExceptionDetected();
        }
    }

    //region Logger API - instance methods

    /**
     * Get the name for this Logger.
     *
     * @return The name for this instance of Logger
     */
    public String getName() {
        return this.name;
    }

    /**
     * @exclude
     *
     * Log at ANALYTICS level.
     *
     * @param message the message to log
     */
    public void analytics(final String message, JSONObject additionalMetadata) {
        doLog(LEVEL.ANALYTICS, message, (new Date()).getTime(), null, additionalMetadata);
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
    protected void doLog(final LEVEL calledLevel, String message, final long timestamp, final Throwable t) {
        doLog(calledLevel, message,timestamp, t, null);
    }

    protected void doLog(final LEVEL calledLevel, String message, final long timestamp, final Throwable t, JSONObject additionalMetadata) {
        if(logPersister == null){
            boolean canLog = (calledLevel != null) && calledLevel.isLoggable();

            if (canLog || (calledLevel == Logger.LEVEL.ANALYTICS)) {
                message = (null == message) ? "(null)" : message;  // android.util.Log can't handle null, so protect it
                switch (calledLevel) {
                    case FATAL:
                    case ERROR:
                        if (null == t) { Log.e(getName(), message); } else { Log.e(getName(), message, t); }
                        break;
                    case WARN:
                        if (null == t) { Log.w(getName(), message); } else { Log.w(getName(), message, t); }
                        break;
                    case INFO:
                        if (null == t) { Log.i(getName(), message); } else { Log.i(getName(), message, t); }
                        break;
                    case DEBUG:
                        if(!Logger.isInternalLogger(this) || Logger.isSDKDebugLoggingEnabled()){
                            if (null == t) { Log.d(getName(), message); } else { Log.d(getName(), message, t); }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        else{
            logPersister.doLog(calledLevel, message, timestamp, t, additionalMetadata, this);
        }
    }

    //endregion
}
