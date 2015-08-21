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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

/**
 * This class is registered as a global java.util.logging.Handler in com.ibm.bms.clientsdk.android.logger.api.Logger.setContext
 *
 */
public class JULHandler extends Handler {

    @Override
    public void close() throws SecurityException {
        // noop
    }

    @Override
    public void flush() {
        // noop
    }

    @Override
    public void publish(LogRecord logRecord) {
        Logger logger = Logger.getInstance(logRecord.getLoggerName());
        // no need to intercept INFO, we already do so by overriding java.util.logging.Logger.info in Logger
        if (logRecord.getLevel().equals(Level.SEVERE)) {
            logger.error(logRecord.getMessage(), logRecord.getThrown());
        } else if (logRecord.getLevel().equals(Level.WARNING)) {
            logger.warn(logRecord.getMessage());
        } else if (logRecord.getLevel().equals(Level.INFO)) {
            logger.info(logRecord.getMessage());
        } else {  // CONFIG, FINE, FINER, and FINEST map to DEBUG
            // may have come from call to java.util.logging.Logger.entering/exiting.  We already record the calling
            // class+method in Logger, so no need to extract these from the logRecord.
            logger.debug(logRecord.getMessage());
        }
    }
}