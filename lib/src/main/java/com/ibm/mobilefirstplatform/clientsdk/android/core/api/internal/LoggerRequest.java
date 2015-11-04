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

package com.ibm.mobilefirstplatform.clientsdk.android.core.api.internal;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;

import java.net.MalformedURLException;

/**
 * Created by iklein on 04/11/15.
 */
public class LoggerRequest extends BaseRequest {

    /**
     * Constructs a new request with the specified URL, using the specified HTTP method.
     *
     * @param url    The resource URL, may be either relative or absolute.
     * @param method The HTTP method to use
     * @throws IllegalArgumentException if the method name is not one of the valid HTTP method names.
     * @throws MalformedURLException    if the URL is not a valid URL
     */
    public LoggerRequest(String url, String method) throws MalformedURLException {
        super(url, method, DEFAULT_TIMEOUT);
    }

    /**
     * Send this resource request asynchronously, with the content of the given byte array as the request body.
     * Note that this method does not set any content type header, if such a header is required it must be set before calling this method.
     *
     * @param data     The byte array containing the request body
     * @param listener The listener whose onSuccess or onFailure methods will be called when this request finishes.
     */
    public void send(byte[] data, ResponseListener listener) {
        super.send(data, listener);
    }
}
