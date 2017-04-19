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


package com.ibm.mobilefirstplatform.clientsdk.android.core.api;


public interface ProgressListener {

    /**
     * This method will be called repeatedly as a download or upload is in progress.
     * Specifically, this method will be called once for every 2 KiB downloaded or uploaded.
     *
     * If the ProgressListener is used for downloading and the response does not include a
     * "Content-Length" header, the totalBytesExpected parameter will be 0.
     *
     * @param bytesSoFar            The number of bytes sent or received so far
     * @param totalBytesExpected    The total number of bytes expected to be sent or received
     */
    void onProgress(long bytesSoFar, long totalBytesExpected);
}
