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

import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.AuthorizationHeaderHelper;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * This class is used to create and send a request. It allows to add all the parameters to the request
 * before sending it.
 */
public class ResourceRequest extends MFPRequest {

    private int oauthFailCounter = 0;
    private RequestBody savedRequestBody;
    private Context context;

    /**
     * Constructs a new resource request with the specified URL, using the specified HTTP method.
     *
     * @param url    The resource URL, may be either relative or absolute.
     * @param method The HTTP method to use
     * @throws IllegalArgumentException if the method name is not one of the valid HTTP method names.
     * @throws MalformedURLException    if the URL is not a valid URL
     */
    public ResourceRequest(Context context, String url, String method) {
        super(url, method, DEFAULT_TIMEOUT);
        this.context = context;
    }

    /**
     * Constructs a new resource request with the specified URL, using the specified HTTP method.
     * Additionally this constructor sets a custom timeout.
     *
     * @param url     The resource URL
     * @param method  The HTTP method to use.
     * @param timeout The timeout in milliseconds for this request.
     * @throws IllegalArgumentException if the method name is not one of the valid HTTP method names.
     * @throws MalformedURLException    if the URL is not a valid URL
     */
    public ResourceRequest(String url, String method, int timeout) {
        super(url, method, timeout);
	}

    @Override
    protected void sendRequest(final ResponseListener listener, final RequestBody requestBody) {
        String cachedAuthHeader = AuthorizationManager.getInstance().getCachedAuthorizationHeader();

        if (cachedAuthHeader != null) {
            addHeader("Authorization", cachedAuthHeader);
        }

        savedRequestBody = requestBody;
        super.sendRequest(listener, requestBody);
    }

    @Override
    protected Callback getCallback(final ResponseListener listener) {
        final RequestBody requestBody = savedRequestBody;
        final ResourceRequest request = this;
        final Context ctx = this.context;

        return new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                if (listener != null) {
                    listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT), e);
                }
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                if (listener == null) {
                    return;
                }

                if (AuthorizationHeaderHelper.isAuthorizationRequired(response)) {
                    if (oauthFailCounter++ < 2) {
                        AuthorizationManager.getInstance().obtainAuthorizationHeader(
                                ctx,
                                new ResponseListener() {
                                    @Override
                                    public void onSuccess(Response response) {
                                        // this will take the auth hader that has been cached by obtainAuthorizationHeader
                                        request.sendRequest(listener, requestBody);
                                    }

                                    @Override
                                    public void onFailure(FailResponse response, Throwable t) {
                                        listener.onFailure(response, t);
                                    }
                                }
                        );
                    } else {
                        listener.onFailure(new FailResponse(FailResponse.ErrorCode.SERVER_ERROR, response), null);
                    }
                } else {
                    if (response.isSuccessful() || response.isRedirect()) {
                        listener.onSuccess(new Response(response));
                    } else if (!response.isRedirect()) {
                        listener.onFailure(new FailResponse(FailResponse.ErrorCode.SERVER_ERROR, response), null);
                    }
                }
            }
        };
    }
}
