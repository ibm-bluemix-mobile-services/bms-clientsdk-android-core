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

package com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.internal.BaseRequest;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.util.Map;

/**
 * Created by vitalym on 10/14/15.
 */

/**
 * AuthorizationRequest is used internally to send authorization requests.
 */
public class AuthorizationRequest extends BaseRequest {

    private static OkHttpClient httpClient = new OkHttpClient();

    /**
     * Constructs the authorization request
     * @param url request url
     * @param method request method
     * @throws MalformedURLException if url is not valid
     */
    public AuthorizationRequest(String url, String method) throws MalformedURLException {
        super(url, method);

        // we want to handle redirects in-place
        httpClient.setFollowRedirects(false);
    }

    /**
     * Override the base getter to return authrization http client
     * @return internal http client
     */
    protected OkHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Setup network interceptor.
     */
    public static void setup(){
        httpClient.setFollowRedirects(false);
    }

    @Override
    public void send(final ResponseListener listener) {
        super.send(listener);
    }

    @Override
    public void send(Map<String, String> formParameters, ResponseListener listener) {
        super.send(formParameters, listener);
    }


}
