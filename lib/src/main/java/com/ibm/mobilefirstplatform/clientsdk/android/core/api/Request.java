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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.internal.BaseRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.internal.ResponseImpl;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.AuthorizationHeaderHelper;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

/**
 * This class is used to create and send a request. It allows to add all the parameters to the request
 * before sending it.
 */
public class Request extends BaseRequest {

    private int oauthFailCounter = 0;
    private RequestBody savedRequestBody;
    private Context context;

    /**
     * The string constant for the GET HTTP method verb.
     */
    public final static String GET = "GET";
    /**
     * The string constant for the POST HTTP method verb.
     */
    public final static String POST = "POST";
    /**
     * The string constant for the PUT HTTP method verb.
     */
    public final static String PUT = "PUT";
    /**
     * The string constant for the DELETE HTTP method verb.
     */
    public final static String DELETE = "DELETE";
    /**
     * The string constant for the TRACE HTTP method verb.
     */
    public final static String TRACE = "TRACE";
    /**
     * The string constant for the HEAD HTTP method verb.
     */
    public final static String HEAD = "HEAD";
    /**
     * The string constant for the OPTIONS HTTP method verb.
     */
    public final static String OPTIONS = "OPTIONS";

    /**
     * Constructs a new resource request with the specified URL, using the specified HTTP method.
     *
     * @param url    The resource URL, may be either relative or absolute.
     * @param method The HTTP method to use
     * @throws IllegalArgumentException if the method name is not one of the valid HTTP method names.
     * @throws MalformedURLException    if the URL is not a valid URL
     */
    public Request(String url, String method) {
        super(url, method, DEFAULT_TIMEOUT);
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
    public Request(String url, String method, int timeout) {
        super(url, method, timeout);
	}

    /**
     * Returns the URL for this resource request.
     *
     * @return String The URL representing the path for this resource request.
     */
    public String getUrl() throws MalformedURLException{
        return super.getUrl();
    }

    /**
     * Returns the HTTP method for this resource request.
     *
     * @return A string containing the name of the HTTP method.
     */
    public String getMethod() {
        return super.getMethod();
    }

    /**
     * Returns the timeout for this resource request.
     *
     * @return the timeout for this resource request
     */
    public int getTimeout() {
        return super.getTimeout();
    }

    /**
     * Returns the query parameters set for this resource request.
     *
     * @return A Map containing the query parameters
     */
    public Map<String, String> getQueryParameters() {
        return super.getQueryParameters();
    }

    /**
     * Sets the query parameters for this resource request.
     *
     * @param parameters A HashMap containing the query parameters
     */
    public void setQueryParameters(Map<String, String> parameters) {
        super.setQueryParameters(parameters);
    }

    /**
     * Returns all the headers that were set for this resource request.
     *
     * @return An array of Headers
     */
    public Map<String, List<String>> getHeaders() {
        return super.getAllHeaders();
    }

    /**
     * Sets headers for this resource request. Overrides all headers previously added.
     *
     * @param headers A multimap containing the header names and corresponding values
     */
    public void setHeaders(Map<String, List<String>> headers) {
        super.setHeaders(headers);
    }

    /**
     * Send this resource request asynchronously, without a request body.
     *
     * @param context The context that will be passed to authentication listener.
     * @param listener The listener whose onSuccess or onFailure methods will be called when this request finishes.
     */
    public void send(Context context, ResponseListener listener) {
        this.context = context;
        super.send(listener);
    }

    /**
     * Send this resource request asynchronously, without a request body.
     *
     * @param context The context that will be passed to authentication listener.
     * @param text The request body text
     * @param listener The listener whose onSuccess or onFailure methods will be called when this request finishes.
     */
    public void send(Context context, String text, ResponseListener listener) {
        this.context = context;
        super.send(text, listener);
    }

    /**
     * Send this resource request asynchronously, without a request body.
     *
     * @param context The context that will be passed to authentication listener.
     * @param bytes     The byte array containing the request body
     * @param listener The listener whose onSuccess or onFailure methods will be called when this request finishes.
     */
    public void send(Context context, byte[] bytes, ResponseListener listener) {
        this.context = context;
        super.send(bytes, listener);
    }

    @Override
    protected void sendRequest(final ResponseListener listener, final RequestBody requestBody) {
        String cachedAuthHeader = AuthorizationManager.getInstance().getCachedAuthorizationHeader();

        if (cachedAuthHeader != null) {
            removeHeaders("Authorization");
            addHeader("Authorization", cachedAuthHeader);
        }

        savedRequestBody = requestBody;
        super.sendRequest(listener, requestBody);
    }

    @Override
    protected Callback getCallback(final ResponseListener listener) {
        final RequestBody requestBody = savedRequestBody;
        final Request request = this;
        final Context ctx = this.context;

        return new Callback() {
            @Override
            public void onFailure(com.squareup.okhttp.Request request, IOException e) {
                if (listener != null) {
                    listener.onFailure(null, e, null);
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
                                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                                        listener.onFailure(response, t, extendedInfo);
                                    }
                                }
                        );
                    } else {
                        listener.onFailure(new ResponseImpl(response), null, null);
                    }
                } else {
                    if (response.isSuccessful() || response.isRedirect()) {
                        listener.onSuccess(new ResponseImpl(response));
                    } else if (!response.isRedirect()) {
                        listener.onFailure(new ResponseImpl(response), null, null);
                    }
                }
            }
        };
    }
}
