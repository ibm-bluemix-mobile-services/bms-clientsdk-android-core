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

package com.ibm.mobilefirstplatform.clientsdk.android.logger.internal;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONObject;

/**
 * This interface is used to talk with the LogPersister in the new Analytics SDK.
 */
public interface LogPersisterInterface {
    void setLogLevel(Logger.LEVEL level);
    Logger.LEVEL getLogLevel();
    Logger.LEVEL getLogLevelSync();

    void storeLogs(final boolean shouldStoreLogs);
    boolean isStoringLogs();

    void setMaxLogStoreSize(final int bytes);
    int getMaxLogStoreSize();

    void send(ResponseListener listener);

    boolean isUncaughtExceptionDetected();

    void doLog(final Logger.LEVEL calledLevel, String message, final long timestamp, final Throwable t, JSONObject additionalMetadata, Logger logger);
}
