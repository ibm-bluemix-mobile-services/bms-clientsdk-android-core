/*
 *     Copyright 2017 IBM Corp.
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


package com.ibm.mobilefirstplatform.clientsdk.android.core.app;


import android.util.Log;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ProgressListener;


public class MyProgressListener implements ProgressListener {


    private String requestURL;

    public MyProgressListener(String requestURL) {
        this.requestURL = requestURL;
    }

    @Override
    public void onProgress(long bytesSoFar, long totalBytesToSend) {
        if (totalBytesToSend > 0) {
            double progress = (double)bytesSoFar / (double)(totalBytesToSend) * 100;
            Log.i("BMSCore", String.format("Progress: %.1f%% for %s", progress, requestURL));
        }
        else {
            Log.i("BMSCore", String.format("Bytes sent so far for " + requestURL + ": %.1f%%", bytesSoFar));
        }
    }
}
