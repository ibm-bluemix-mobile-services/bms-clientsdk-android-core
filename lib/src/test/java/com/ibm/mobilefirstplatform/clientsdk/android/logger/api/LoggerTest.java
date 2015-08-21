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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.provider.Settings;

import com.ibm.mobilefirstplatform.clientsdk.android.core.BuildConfig;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.FailResponse;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger.LEVEL;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.internal.FileLoggerInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.httpclient.FakeHttp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Config(sdk = Build.VERSION_CODES.JELLY_BEAN_MR2, constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class LoggerTest {

    private static final String FILE_NAME0 = "wl.log.0";
    private static final String FILE_NAME1 = "wl.log.1";
    private static final String ANALYTICS_FILE_NAME0 = "analytics.log.0";
    private static final String ANALYTICS_FILE_NAME1 = "analytics.log.1";

    private static final String MESSAGE_KEY = "msg";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String PACKAGE_KEY = "pkg";
    private static final String THREADID_KEY = "threadid";
    private static final String LEVEL_KEY = "level";

    protected Context activity = RuntimeEnvironment.application.getApplicationContext();

    // a spy!
    private static class FileLoggerMock extends FileLoggerInterface {

        private JSONArray jsonObjects = new JSONArray();
        private Context activity;

        protected FileLoggerMock(Context activity) {
            super(null, null);
            this.activity = activity;
        }

        @Override
        public void log(JSONObject logData, String fileName) throws SecurityException,
                IOException {
            File file = new File(activity.getFilesDir(), FILE_NAME0);
            File file2 = new File(activity.getFilesDir(), ANALYTICS_FILE_NAME0);

            try {
                if (!file.exists() || file.length() == 0 && !logData.getString("level").equals("ANALYTICS")) {
                    // make a non-empty file so send() continues, but it will actually pick up data from this mock
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write('a');
                    fos.close();
                }
                if (!file2.exists() || file2.length() == 0 && logData.getString("level").equals("ANALYTICS")){
                    // make a non-empty file so sendAnalytics() continues, but it will actually pick up data from this mock
                    FileOutputStream fos = new FileOutputStream(file2);
                    fos.write('a');
                    fos.close();
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            jsonObjects.put(logData);
        }

        public JSONArray getAccumulatedLogCalls() {
            return jsonObjects;
        }

        @Override
        public byte[] getFileContentsAsByteArray(File file)
                throws UnsupportedEncodingException {
            StringBuilder sb1 = new StringBuilder();
            sb1.append("");
            for(int i = 0; i < jsonObjects.length(); i++) {
                try {
                    sb1.append(jsonObjects.getJSONObject(i).toString() + ",");
                } catch (JSONException e) {
                }
            }
            return sb1.toString().getBytes("utf-8");
        }

    }

    @Before
    public void reset() throws Exception {
        // reset Logger defaults:
        Logger.unsetContext();

        // clear all shared prefs
        SharedPreferences sharedPreferences = RuntimeEnvironment.application.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();
        System.setProperty("http.agent", "Test user agent");

        PackageManager pm = activity.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(activity.getPackageName(), PackageManager.GET_SIGNATURES);
        pi.signatures = new Signature[]{ new Signature("1234567890abcdef") };

        File file = new File(activity.getFilesDir(), FILE_NAME0);
        file.delete();
        file = new File(activity.getFilesDir(), FILE_NAME1);
        file.delete();
        file = new File(activity.getFilesDir(), FILE_NAME0 + ".send");
        file.delete();
        file = new File(activity.getFilesDir(), FILE_NAME1 + ".send");
        file.delete();
        file = new File(activity.getFilesDir(), ANALYTICS_FILE_NAME0);
        file.delete();
        file = new File(activity.getFilesDir(), ANALYTICS_FILE_NAME1);
        file.delete();
        file = new File(activity.getFilesDir(), ANALYTICS_FILE_NAME0 + ".send");
        file.delete();
        file = new File(activity.getFilesDir(), ANALYTICS_FILE_NAME1 + ".send");
        file.delete();

        // some tests below use FakeHttp.addPendingHttpResponse();.  We should clear them after every test.
        FakeHttp.clearPendingHttpResponses();

        // reset the static flags in Logger that prevent accidental double sending of the
        // persistent file contents
        Field f1 = Logger.class.getDeclaredField("sendingLogs");
        f1.setAccessible(true);
        f1.set(null, false);
        Field f2 = Logger.class.getDeclaredField("sendingAnalyticsLogs");
        f2.setAccessible(true);
        f2.set(null, false);
    }

    @After
    public void tearDown() throws Exception {
        // some tests below use FakeHttp.addPendingHttpResponse();.  We should clear them after every test.
        FakeHttp.clearPendingHttpResponses();

        // reset the static flags in Logger that prevent accidental double sending of the
        // persistent file contents
        Field f1 = Logger.class.getDeclaredField("sendingLogs");
        f1.setAccessible(true);
        f1.set(null, false);
        Field f2 = Logger.class.getDeclaredField("sendingAnalyticsLogs");
        f2.setAccessible(true);
        f2.set(null, false);
    }

    @Test
    public void testNullContext() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        // we should not get any exceptions
        String s = new String(Logger.getByteArrayFromFile(new File(activity.getFilesDir(), FILE_NAME0)));
        assertEquals("", s);

        Logger logger = Logger.getInstance("testNullContext");
        Logger.setCapture(true);
        logger.info("hi");
        // no file should exist, since we can't write to file when the static Context object in Logger is null
        assertEquals(0, mockFileLogger.getAccumulatedLogCalls().length());
    }

    @Test
    public void testGetInstance() {
        Logger.setContext(activity);
        Logger loggerInstance1 = Logger.getInstance("tag 1");
        Logger loggerInstance2 = Logger.getInstance("tag 2");
        // should be exactly the same instance:
        assertFalse("instances returned from createInstance are the same object, but should not be", loggerInstance1.hashCode() == loggerInstance2.hashCode());
    }

    @Test
    public void testDefaults() {
        // test defaults before setContext is called
        assertEquals(Logger.LEVEL.DEBUG, Logger.getLevel());
        assertTrue(Logger.getCapture());
        assertEquals(100000, Logger.getMaxStoreSize());
    }

    @Test
    public void testGetInstanceSameTag() {
        Logger.setContext(activity);
        Logger loggerInstance1 = Logger.getInstance("tag");
        Logger loggerInstance2 = Logger.getInstance("tag");
        // should be exactly the same instance:
        assertTrue("instances returned from createInstance are not the same object, but should be", loggerInstance1.hashCode() == loggerInstance2.hashCode());
    }

    @Test
    public void testCapture() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        System.out.println(mockFileLogger.getAccumulatedLogCalls().length());
        Logger.setContext(activity);
//		System.out.println(mockFileLogger.getAccumulatedLogCalls().getJSONObject(0).getString("msg"));
//		System.out.println(mockFileLogger.getAccumulatedLogCalls().getJSONObject(1).getString("msg"));
        Logger.setLevel(Logger.LEVEL.DEBUG);

        Logger logger = Logger.getInstance("tag 2");

        logger.info("message 1");
        waitForNotify(logger);

        logger.info("message 2");
        waitForNotify(logger);


        // persistence is on by default, so...
        assertEquals(2, mockFileLogger.getAccumulatedLogCalls().length());

        // this will verify file existence and the code path through 'getStringFromFile(FILE_NAME0)'

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(1);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs (persistence was only on for second logger call)
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("tag 2", jsonObject.get(PACKAGE_KEY));

        assertEquals("INFO", jsonObject.get(LEVEL_KEY));
        assertEquals("message 2", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testSetCaptureServerOverride() throws Exception {
        Logger.setContext(activity);

        RuntimeEnvironment.application.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE).edit().putBoolean(Logger.SHARED_PREF_KEY_logPersistence_from_server, true).commit();

        // even when setCapture API call is made, the presence and setting of SHARED_PREF_KEY_logPersistence_from_server wins
        Logger.setCapture(false);

        waitForNotify(Logger.WAIT_LOCK);

        // when SHARED_PREF_KEY_logPersistence_from_server is set, it takes precedence over whatever was set programmatically by Logger.setCapture API call
        assertTrue(Logger.getCapture());

        // also should pick up the server override SHARED_PREF_KEY_logPersistence_from_server value when Logger.setContext is called:
        Logger.unsetContext();
        Logger.setContext(activity);

        assertTrue(Logger.getCapture());

        // when the SHARED_PREF_KEY_logPersistence_from_server is removed, and setCapture is called, it should now take effect again:
        RuntimeEnvironment.application.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE).edit().remove(Logger.SHARED_PREF_KEY_logPersistence_from_server).commit();
        // even when setCapture API call is made, the presense and setting of SHARED_PREF_KEY_logPersistence_from_server wins
        Logger.setCapture(false);
        assertFalse(Logger.getCapture());

        // and when Logger.setContext is called:
        Logger.unsetContext();
        Logger.setContext(activity);

        assertFalse(Logger.getCapture());
    }

    @Test
    public void testGetInstanceNullTag() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance(null);
        logger.info("message 1");

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        assertEquals("NONE", jsonObject.getString(PACKAGE_KEY));
    }

    @Test
    public void testGetInstanceEmptyTag() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("  ");
        logger.info("message 1");

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        assertEquals("NONE", jsonObject.getString(PACKAGE_KEY));
    }

    @Test
    public void testSharedPrefsDefaults() throws InterruptedException {
        Logger.setContext(activity);

        SharedPreferences prefs = activity.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE);

        // should pick up the defaults:
        assertEquals(Logger.getLevelDefault(), Logger.LEVEL.fromString(prefs.getString(Logger.SHARED_PREF_KEY_level, null)));
        assertEquals(Logger.DEFAULT_logFileMaxSize, prefs.getInt(Logger.SHARED_PREF_KEY_logFileMaxSize, 0));
        assertEquals(Logger.DEFAULT_capture, prefs.getBoolean(Logger.SHARED_PREF_KEY_logPersistence, !Logger.DEFAULT_capture));
        // empty JSONArray object
        assertEquals("{}", prefs.getString(Logger.SHARED_PREF_KEY_filters, null));
    }

    @Test
    public void testSharedPrefsTakesValueBeforeContext() throws Exception {
        // methods called before setContext should result in their values going into SharedPrefs:
        Logger.setCapture(!Logger.DEFAULT_capture);

        waitForNotify(Logger.WAIT_LOCK);

        Logger.setLevel(LEVEL.WARN);

        waitForNotify(Logger.WAIT_LOCK);

        Logger.setMaxStoreSize(90000);

        HashMap<String, LEVEL> filters = new HashMap<String, LEVEL>();
        filters.put("jsonstore", Logger.LEVEL.WARN);
        Logger.setFilters(filters);

        waitForNotify(Logger.WAIT_LOCK);

        Logger.setContext(activity);

        SharedPreferences prefs = activity.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE);

        // should pick up the values set in method calls above
        assertEquals(LEVEL.WARN, Logger.LEVEL.fromString(prefs.getString(Logger.SHARED_PREF_KEY_level, null)));
        assertEquals(90000, prefs.getInt(Logger.SHARED_PREF_KEY_logFileMaxSize, 0));
        assertEquals(!Logger.DEFAULT_capture, prefs.getBoolean(Logger.SHARED_PREF_KEY_logPersistence, Logger.DEFAULT_capture));
        assertEquals("{\"jsonstore\":\"WARN\"}", prefs.getString(Logger.SHARED_PREF_KEY_filters, ""));
        assertEquals("{\"jsonstore\":\"WARN\"}", prefs.getString(Logger.SHARED_PREF_KEY_filters, ""));
    }


    @Test
    public void testGetByteArrayFromFile() throws Exception {
        // don't use a mock for the FileLogger in this test
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("tag");
        Logger.setCapture(true);

        logger.info("message");

        waitForNotify(logger);

        // read raw file contents into byte array:
        String result = new String(Logger.getByteArrayFromFile(new File(activity.getFilesDir(), FILE_NAME0)));
        // confirm last two characters are a JSON object close and a comma delimiter
        assertEquals("},", result.substring(result.length() - 2, result.length()));
    }

    @Test
    public void testGetStringFromFileDoesNotExist() throws Exception {
        Logger.setContext(activity);

        // read raw file contents into byte array (but file does not exist):
        String result = new String(Logger.getByteArrayFromFile(new File(activity.getFilesDir(), FILE_NAME0)));
        // return of empty string when file does not exist
        assertEquals("", result);
    }

    @Test
    public void testJULLoggerMappings() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        // test that the JUL path works, and the mappings are good
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger.setCapture(true);

        HashMap<String, LEVEL> messagesMapping = new HashMap<String, LEVEL>();
        messagesMapping.put("JULsevere", LEVEL.ERROR);
        messagesMapping.put("JULwarning", LEVEL.WARN);
        messagesMapping.put("JULinfo", LEVEL.INFO);
        messagesMapping.put("JULconfig", LEVEL.DEBUG);
        messagesMapping.put("JULfine", LEVEL.DEBUG);
        messagesMapping.put("JULfiner", LEVEL.DEBUG);
        messagesMapping.put("JULfinest", LEVEL.DEBUG);
        messagesMapping.put("ENTRY", LEVEL.DEBUG);
        messagesMapping.put("RETURN", LEVEL.DEBUG);
        messagesMapping.put("ENTRY {0}", LEVEL.DEBUG);
        messagesMapping.put("RETURN {0}", LEVEL.DEBUG);

        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("package");

        // a trick to get this test to wait
        Logger logger = Logger.getInstance("package");

        // JUL API:
        julLogger.severe("JULsevere");
        waitForNotify(logger);
        julLogger.warning("JULwarning");
        waitForNotify(logger);
        julLogger.info("JULinfo");
        waitForNotify(logger);
        julLogger.config("JULconfig");
        waitForNotify(logger);
        julLogger.fine("JULfine");
        waitForNotify(logger);
        julLogger.finer("JULfiner");
        waitForNotify(logger);
        julLogger.finest("JULfinest");
        waitForNotify(logger);
        julLogger.entering("enteringClass", "enteringMethod");
        waitForNotify(logger);
        julLogger.exiting("exitingClass", "exitingMethod");
        waitForNotify(logger);
        julLogger.entering("enteringClass", "enteringMethod", new String("param"));
        waitForNotify(logger);
        julLogger.exiting("exitingClass", "exitingMethod", new String("param"));
        waitForNotify(logger);

        int foundCount = 0;

        JSONArray arr = mockFileLogger.getAccumulatedLogCalls();

        for (int i = 0; i < arr.length(); i++) {
            Object obj = null;
            try {
                obj = arr.get(i);
            } catch (JSONException e) {
                // ignore
            }
            if (obj != null) {
                String message = arr.getJSONObject(i).getString("msg");
                LEVEL l = LEVEL.fromString(arr.getJSONObject(i).getString("level"));
                if (messagesMapping.get(message).equals(l /*LEVEL.fromString(arr.getJSONObject(i).getString("level"))*/)) {
                    foundCount++;
                }
            }
        }

        assertEquals(messagesMapping.size(), foundCount);


    }

    @Test
    public void testJULLoggerGoodMetadata() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        // test that the JUL path works, and the metadata shows the caller, not JULLogger
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger.setCapture(true);

        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("package");

        // a trick to wait for the logger to finish in this test
        Logger logger = Logger.getInstance("package");

        // JUL API:
        julLogger.severe("JULsevere");

        // a trick to wait for the logger to finish in this test
        waitForNotify(logger);

        int foundCount = 0;

        JSONArray arr = mockFileLogger.getAccumulatedLogCalls();

        for (int i = 0; i < arr.length(); i++) {
            Object obj = null;
            try {
                obj = arr.get(i);
            } catch (JSONException e) {
                // ignore
            }
            if (obj != null) {
                String message = arr.getJSONObject(i).getString("msg");
                if (message.equals("JULsevere")) {
                    // we found the right entry, now confirm the metadata
                    System.out.println(arr.getJSONObject(i));
                    foundCount++;
                    break;
                }
            }
        }

        assertEquals(1, foundCount);
    }

    @Test
    public void testIString() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.info("message");

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // INFO
        assertEquals("INFO", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testIStringThrowable() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.INFO);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.info("message", new Exception());

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // INFO
        assertEquals("INFO", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testDString() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.debug("message");

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // DEBUG
        assertEquals("DEBUG", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testDStringThrowable() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.debug("message", new Exception());

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // DEBUG
        assertEquals("DEBUG", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testEString() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.error("message");

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // ERROR
        assertEquals("ERROR", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testEStringThrowable() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.error("message", new Exception());

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // ERROR
        assertEquals("ERROR", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testFString() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.fatal("message");

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // FATAL
        assertEquals("FATAL", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));

        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testFStringThrowable() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.fatal("message", new Exception());

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // FATAL
        assertEquals("FATAL", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testWString() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.warn("message");

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // WARN
        assertEquals("WARN", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testWStringThrowable() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.warn("message", new Exception());

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // ERROR
        assertEquals("WARN", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }

    @Test
    public void testAString() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setAnalyticsCapture(true);

        logger.analytics("message",null);

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // WARN
        assertEquals("ANALYTICS", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);

    }

    @Test
    public void testAStringWithFilters() throws Exception {
        // Filters should not prevent analytics captures
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        HashMap<String, LEVEL> filters = new HashMap<String, LEVEL>();
        filters.put("jsonstore", Logger.LEVEL.INFO);
        Logger.setFilters(filters); // should send to file anyway
        Logger.setAnalyticsCapture(true);

        logger.analytics("message", null);

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // WARN
        assertEquals("ANALYTICS", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);

    }

    @Test
    public void testJSONArrayHashMapConversions() {
        HashMap<String, LEVEL> filters = new HashMap<String, LEVEL>();
        filters.put("joe", Logger.LEVEL.WARN);
        filters.put("bob", Logger.LEVEL.INFO);
        JSONObject filtersObject = Logger.HashMapToJSONObject(filters);  // convert
        HashMap<String, LEVEL> filters2 = Logger.JSONObjectToHashMap(filtersObject);  // convert it back
        assertEquals(filters, filters2);
    }

    @Test
    public void testFilters() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        // make sure filters are honored
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);

        Logger.setCapture(true);

        waitForNotify(Logger.WAIT_LOCK);

        HashMap<String, LEVEL> filters = new HashMap<String, LEVEL>();
        filters.put("jsonstore", Logger.LEVEL.INFO);

        Logger.setFilters(filters);

        waitForNotify(Logger.WAIT_LOCK);

        Logger logger1 = Logger.getInstance("package");
        Logger logger2 = Logger.getInstance("jsonstore");

        logger1.debug("package debug 1");

        waitForNotify(logger1);

        logger2.debug("jsonstore debug");

        waitForNotify(logger2);

        logger2.warn("jsonstore warn");

        waitForNotify(logger2);

        logger2.info("jsonstore info");

        waitForNotify(logger2);

        Logger.setFilters(null);  // remove the filters

        waitForNotify(Logger.WAIT_LOCK);

        logger1.debug("package debug 2");

        waitForNotify(logger1);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();

        // there should be three entries
        assertEquals(3, jsonArray.length());
        assertEquals("jsonstore warn", jsonArray.getJSONObject(0).get(MESSAGE_KEY));
        assertEquals("jsonstore info", jsonArray.getJSONObject(1).get(MESSAGE_KEY));
        assertEquals("package debug 2", jsonArray.getJSONObject(2).get(MESSAGE_KEY));
    }

    @Test
    public void testSetFiltersServerOverride() throws Exception {
        Logger.setContext(activity);

        RuntimeEnvironment.application.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE).edit().putString(Logger.SHARED_PREF_KEY_filters_from_server, "{'otherPkg':'WARN'}").commit();

        // even when setCapture API call is made, the presence and setting of SHARED_PREF_KEY_filters_from_server wins
        HashMap<String, LEVEL> filters = new HashMap<String, LEVEL>();
        filters.put("jsonstore", Logger.LEVEL.INFO);
        Logger.setFilters(filters);

        waitForNotify(Logger.WAIT_LOCK);

        // when SHARED_PREF_KEY_filters_from_server is set, it takes precedence over whatever was set programmatically by Logger.setCapture API call
        assertEquals(LEVEL.WARN, Logger.getFilters().get("otherPkg"));

        // also should pick up the server override SHARED_PREF_KEY_filters_from_server value when Logger.setContext is called:
        Logger.unsetContext();
        Logger.setContext(activity);

        assertEquals(LEVEL.WARN, Logger.getFilters().get("otherPkg"));

        // try setting the server override shared pref value to null and empty:
        RuntimeEnvironment.application.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE).edit().putString(Logger.SHARED_PREF_KEY_filters_from_server, "{}").commit();
        // even when setCapture API call is made, the presence and setting of SHARED_PREF_KEY_filters_from_server wins
        Logger.setFilters(filters);

        waitForNotify(Logger.WAIT_LOCK);

        assertEquals(0, Logger.getFilters().size());

        // when the SHARED_PREF_KEY_filters_from_server is removed, and setCapture is called, it should now take effect again:
        RuntimeEnvironment.application.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE).edit().remove(Logger.SHARED_PREF_KEY_filters_from_server).commit();
        // even when setCapture API call is made, the presence and setting of SHARED_PREF_KEY_filters_from_server wins
        Logger.setFilters(filters);

        waitForNotify(Logger.WAIT_LOCK);

        assertNull(Logger.getFilters().get("otherPkg"));
        assertEquals(LEVEL.INFO, Logger.getFilters().get("jsonstore"));

        // and when Logger.setContext is called:
        Logger.unsetContext();
        Logger.setContext(activity);

        assertEquals(LEVEL.INFO, Logger.getFilters().get("jsonstore"));
    }

    @Test
    public void testLevelFromString() {
        assertEquals(Logger.LEVEL.FATAL, Logger.LEVEL.fromString("FaTaL"));
        assertEquals(Logger.LEVEL.ERROR, Logger.LEVEL.fromString("ErROR"));
        assertEquals(Logger.LEVEL.WARN, Logger.LEVEL.fromString("WArN"));
        assertEquals(Logger.LEVEL.INFO, Logger.LEVEL.fromString("INFo"));
        assertEquals(Logger.LEVEL.DEBUG, Logger.LEVEL.fromString("dEBUG"));
        assertEquals(null, Logger.LEVEL.fromString("some invalid string"));
    }

    @Test
    public void testNullLevel() throws Exception {
        setFileLoggerInstanceField(activity);
        Logger.setContext(activity);

        // just make sure things don't break
        Logger.setLevel(null);

        Logger logger = Logger.getInstance("tag");
        logger.info("hi");

		/*
		 * WARN:  this test can occasionally experience timing issues.  The Logger.setContext() will notify on the
		 * WAIT_LOCK multiple times, so the wait above may be over too soon.  We Thread.sleep with hopefully enough
		 * time to make up for it.  If this test shows problems, try adding more time to the sleep:
		 */
        Thread.sleep(100);
    }

    @Test
    public void testLevels() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        final String hi = "hi";

        // no calls should create a file:

        Logger.setLevel(LEVEL.ERROR);
        logger.warn(hi);
        waitForNotify(logger);
        logger.info(hi);
        waitForNotify(logger);
        logger.debug(hi);
        waitForNotify(logger);

        Logger.setLevel(LEVEL.WARN);
        logger.info(hi);
        waitForNotify(logger);
        logger.debug(hi);
        waitForNotify(logger);

        Logger.setLevel(LEVEL.INFO);
        logger.debug(hi);
        waitForNotify(logger);

        // "hi" should not appear in the file

        assertEquals(0, mockFileLogger.getAccumulatedLogCalls().length());

    }

    @Test
    public void testSetLevelServerOverride() throws Exception {
        Logger.setContext(activity);

        RuntimeEnvironment.application.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE).edit().putString(Logger.SHARED_PREF_KEY_level_from_server, "DeBuG").commit();

        // even when setCapture API call is made, the presence and setting of SHARED_PREF_KEY_level_from_server wins
        Logger.setLevel(LEVEL.WARN);

        waitForNotify(Logger.WAIT_LOCK);

        // when SHARED_PREF_KEY_level_from_server is set, it takes precedence over whatever was set programmatically by Logger.setCapture API call
        assertEquals(LEVEL.DEBUG, Logger.getLevel());

        // also should pick up the server override SHARED_PREF_KEY_level_from_server value when Logger.setContext is called:
        Logger.unsetContext();
        Logger.setContext(activity);

        assertEquals(LEVEL.DEBUG, Logger.getLevel());

        // when the SHARED_PREF_KEY_level_from_server is removed, and setCapture is called, it should now take effect again:
        RuntimeEnvironment.application.getSharedPreferences(Logger.SHARED_PREF_KEY, Context.MODE_PRIVATE).edit().remove(Logger.SHARED_PREF_KEY_level_from_server).commit();
        // even when setCapture API call is made, the presense and setting of SHARED_PREF_KEY_level_from_server wins
        Logger.setLevel(LEVEL.WARN);
        assertEquals(LEVEL.WARN, Logger.getLevel());

        // and when Logger.setContext is called:
        Logger.unsetContext();
        Logger.setContext(activity);

        assertEquals(LEVEL.WARN, Logger.getLevel());
    }

    @Test
    public void testNullThrowable() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.warn("message", null);  // null!

        waitForNotify(logger);

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        // jsonObject should have a fixed number of key/value pairs
        assertEquals("resulting jsonobject in file has wrong number of key/value pairs", 6, jsonObject.length());
        // verify the key/value pairs
        assertTrue(jsonObject.has(TIMESTAMP_KEY));  // don't test the value, as it may differ from current
        assertEquals("package", jsonObject.get(PACKAGE_KEY));
        // WARN
        assertEquals("WARN", jsonObject.get(LEVEL_KEY));
        assertEquals("message", jsonObject.get(MESSAGE_KEY));
        // ensure no exception is thrown by parsing the threadid value:
        assertFalse(jsonObject.getLong(THREADID_KEY) == 0);
    }


    @Test
    public void testSetMaxStoreSizeTooSmall() throws Exception {
        // don't use the mock FileLogger in this test
        final int size = 100;

        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger.setMaxStoreSize(size);
        Logger.setCapture(true);

        Logger logger = Logger.getInstance("tag");

        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = 'a';
        }

        logger.info(new String(bytes));

        waitForNotify(logger);

        // file size is now over 100 bytes.  We set the threshold
        // to 100, but the implementation does not allow a setting
        // lower than 10000

        // a log call here should be appended, and we should have two log entries
        logger.info("hi");

        waitForNotify(logger);

        String result = new String(Logger.getByteArrayFromFile(new File(activity.getFilesDir(), FILE_NAME0)));
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }
        JSONArray jsonArray = new JSONArray("[" + result + "]");

        assertEquals(2, jsonArray.length());
    }

    @Test
    public void testSetMaxStoreSizeAcceptableSize() throws Exception {
        // don't use the mock FileLogger in this test
        final int size = 11000;

        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        Logger.setMaxStoreSize(size);
        Logger.setCapture(true);

        Logger logger = Logger.getInstance("tag");

        // no need to do 'size' log calls since the data is big enough.  Doing 'size' calls makes a sloooow test.
        for (int i = 0; i < (size/50); i++) {
            logger.info(String.valueOf(i));
            waitForNotify(logger);
        }

        // our code protects against writing additional data to a too-big file, so
        // this should append and push out old entries.
        logger.info("hi");

        waitForNotify(logger);

        String result = new String(Logger.getByteArrayFromFile(new File(activity.getFilesDir(), FILE_NAME0)));
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }
        JSONArray jsonArray = new JSONArray("[" + result + "]");

        // we should not find a log entry with message = "0", but we should find "hi"
        assertFalse(jsonArray.getJSONObject(0).getInt("msg") == 0);  // first log entry
        assertEquals("hi", jsonArray.getJSONObject(jsonArray.length() - 1).getString("msg"));  // last log entry

    }

    @Test
    public void testFileRecreated() throws Exception {

        // don't use the mock FileLogger in this test
        Logger.setContext(activity);
        Logger.setLevel(Logger.LEVEL.DEBUG);
        File file = new File(activity.getFilesDir(), FILE_NAME0);

        Logger logger = Logger.getInstance("package");
        Logger.setCapture(true);

        logger.info("a message");

        waitForNotify(logger);
        assertTrue(file.exists());

        // If I deleted it instead of letting java.util.logger manage the files, will
        // java.util.logger recreate it?  Let's make sure it does.
        file.delete();
        assertFalse(file.exists());

        logger.info("another message");

        waitForNotify(logger);

        assertTrue(file.exists());

    }

    @Test
    public void testThreadQueueOrderSetters() throws Exception {
        FileLoggerMock mockFileLogger = setFileLoggerInstanceField(activity);
        // order in the file should reflect the order of the calls even though multi-threaded:
        Logger.setContext(activity);
        Logger logger = Logger.getInstance("pkg");
        final int limit = 100;
        for (int i = 0; i < limit; i++) {
            if (i % 2 == 0) {
                Logger.setCapture(true);
            } else {
                Logger.setCapture(false);
            }
            if (i % 2 == 0) {
                Logger.setLevel(Logger.LEVEL.DEBUG);
            } else {
                Logger.setLevel(Logger.LEVEL.WARN);
            }
            logger.debug(String.valueOf(i));
        }

        Thread.sleep(limit * 5);  // hope that's long enough for all the threads to complete

        JSONArray jsonArray = mockFileLogger.getAccumulatedLogCalls();

        assertEquals(50, jsonArray.length());

        // check the order.  If you were to change the LogThreadPoolWorkQueue
        // size to something other than 1, this should fail
        for (int i = 0; i < 50; i++) {
            assertEquals(String.valueOf(i * 2), jsonArray.getJSONObject(i).getString("msg"));
        }
    }

    @Test
    public void testThreadQueueOrderGetCapture() throws Exception {
        // order of the calls to setCapture/getCapture should be preserved even though multi-threaded
        final int limit = 100;
        ArrayList<Boolean> results = new ArrayList<Boolean>();
        for (int i = 0; i < limit; i++) {
            if (i % 2 == 0) {
                Logger.setCapture(true);
            } else {
                Logger.setCapture(false);
            }
            results.add(Logger.getCapture());
        }
        for (int i = 0; i < limit; i++) {
            if (i % 2 == 0) {
                assertTrue(results.get(i));
            } else {
                assertFalse(results.get(i));
            }
        }
    }

    @Test
    public void testThreadQueueOrderGetLevel() throws Exception {
        // order of the calls to setLevel/getLevel should be preserved even though multi-threaded
        final int limit = 100;
        ArrayList<Logger.LEVEL> results = new ArrayList<Logger.LEVEL>();
        for (int i = 0; i < limit; i++) {
            if (i % 2 == 0) {
                Logger.setLevel(Logger.LEVEL.DEBUG);
            } else {
                Logger.setLevel(Logger.LEVEL.INFO);
            }
            results.add(Logger.getLevel());
        }
        for (int i = 0; i < limit; i++) {
            if (i % 2 == 0) {
                assertEquals(Logger.LEVEL.DEBUG, results.get(i));
            } else {
                assertEquals(Logger.LEVEL.INFO, results.get(i));
            }
        }
    }

    // UTILITIES

    /**
     * a public utility method for others, should they need it.  Because Logger.unsetContext is so useful
     * for unit test, but is protected visibility, only classes in the same java package can call it.
     * Other tests, however, may have good reason to call it, and I didn't want to make Logger.unsetContext
     * public, so they can do so through this utility method.  (See WLDroidGapTest, for an example of
     * another test needing to do this.)
     */
    public static void unsetLoggerContext() {
        Logger.unsetContext();
    }

    /**
     * so we can set the Logger.fileLoggerInstance field to a mock instance to avoid lots of file i/o
     * @return
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    public static FileLoggerMock setFileLoggerInstanceField(Context activity) throws Exception {
        Field field = Logger.class.getDeclaredField("fileLoggerInstance");
        field.setAccessible(true);

        FileLoggerMock fileLoggerInterfaceInstance = new FileLoggerMock(activity);
        field.set(null, fileLoggerInterfaceInstance);
        return fileLoggerInterfaceInstance;
    }

    // we do this so as not to repeat code, and make our test methods long and ugly
    private static void waitForNotify(Object obj) throws InterruptedException {
        synchronized(obj) {
            obj.wait(10000);
        }
        if (obj.equals(Logger.WAIT_LOCK)) {
            Thread.sleep(100);  // always sleep an extra 100 ms to avoid post-wait log calls that may be sprinkled through the code
        }
    }

}
