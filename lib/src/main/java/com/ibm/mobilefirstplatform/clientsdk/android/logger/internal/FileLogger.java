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

package com.ibm.mobilefirstplatform.clientsdk.android.logger.internal;


import android.content.Context;
import android.util.Log;

import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class FileLogger extends FileLoggerInterface {

    private static FileLogger singleton;
    private static FileLogger noopSingleton = new FileLogger(null, null);
    private static String filePath;
    private static ClientLogFormatter formatter;
    private static Context context;

    private FileLogger (String name, String resourceBundleName) {
        super (name, resourceBundleName);
    }

    public static FileLogger getInstance() {
        if (null == singleton && context != null) {
            // we need to initialize some stuff
            singleton = new FileLogger(null, null);
            filePath = context.getFilesDir () + System.getProperty("file.separator") + Logger.FILENAME;
            formatter = new ClientLogFormatter();
            singleton.setLevel (Level.ALL);
        } else {
            return noopSingleton;
        }
        return singleton;
    }

    public static void setContext (Context newContext) {
        context = newContext;
    }

    private static class ClientLogFormatter extends Formatter {

        @Override
        public String format(final LogRecord logRecord) {
            // the message is all we care about
            return logRecord.getMessage();
        }
    }

    /**
     * Callers should pass a JSONObject in the format described in the class-level documentation.  It will be
     * placed as-is, using JSONObject.toString(), with no additional contextual information automatically appended.
     * We use java.util.logging simply to take advantage of its built-in thread-safety and log rollover.
     *
     * @param logData JSONObject, placed in log file as-is using JSONObject.toString()
     * @throws IOException
     * @throws SecurityException
     */
    public synchronized void log(final JSONObject logData, String fileName) throws SecurityException, IOException {
        if (null != singleton) {
            filePath = context.getFilesDir () + System.getProperty("file.separator") + fileName;
            FileHandler handler = null;
            handler = new FileHandler(filePath, Logger.getMaxStoreSize(), Logger.MAX_NUM_LOG_FILES, true);
            handler.setFormatter(formatter);
            singleton.addHandler (handler);
            singleton.log (Level.FINEST, logData.toString() + ",");

            singleton.getHandlers()[0].close();
            singleton.removeHandler (handler);
        }

    }

    @Override
    public byte[] getFileContentsAsByteArray(File file) throws UnsupportedEncodingException {
        return getByteArrayFromFile(file.getName());
    }

    // set to package protected visibility for unit testing only
    // public for testing only
    private byte[] getByteArrayFromFile (final String file) throws UnsupportedEncodingException {
        String ret = "";
        File fl = new File(context.getFilesDir (), file);
        if (fl.exists()) {
            try {
                FileInputStream fin = new FileInputStream(fl);
                ByteArrayOutputStream baos = new ByteArrayOutputStream((int) fl.length ());
                copyStream (fin, baos);
                return baos.toByteArray ();
            } catch (IOException e) {
                Log.e(Logger.LOG_PACKAGE_NAME, "problem reading file " + fl.toString(), e);
            }
        }
        return ret.getBytes ("UTF-8");
    }

    private static void copyStream (InputStream is, OutputStream os)
            throws IOException {
        final int buffer_size = 1024;
        byte[] bytes = new byte[buffer_size];
        for (;;) {
            int count = is.read (bytes, 0, buffer_size);
            if (count == -1) {
                break;
            }
            os.write (bytes, 0, count);
        }
        is.close();
    }
}