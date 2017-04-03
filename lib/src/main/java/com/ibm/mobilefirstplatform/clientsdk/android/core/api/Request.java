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

import com.ibm.mobilefirstplatform.clientsdk.android.core.internal.BaseRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.internal.ResponseImpl;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import okio.BufferedSink;

/**
 * This class is used to create and send a request. It allows to add all the parameters to the request
 * before sending it.
 */
public class Request extends BaseRequest {

    private int oauthFailCounter = 0;
    private RequestBody savedRequestBody;
    private Context context;

	/**
     * Constructs a new resource request with the specified URL, using the specified HTTP method.
     *
     * @param url    The resource URL, may be either relative or absolute.
     * @param method The HTTP method to use
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
     */
    public Request(String url, String method, int timeout) {
        super(url, method, timeout);
	}

    /**
     * Constructs a new resource request with the specified URL, using the specified HTTP method.
     * Additionally this constructor sets a custom timeout and number of times to automatically
     * retry failed requests.
     *
     * @param url           The resource URL
     * @param method        The HTTP method to use.
     * @param timeout       The timeout in milliseconds for this request.
     * @param autoRetries   The number of times to retry each request if it fails due to timeout or loss of network connection.
     */
    public Request(String url, String method, int timeout, int autoRetries) {
        super(url, method, timeout, autoRetries);
    }

    /**
     * Returns the URL for this resource request.
     *
     * @return String The URL representing the path for this resource request.
     */
    public String getUrl(){
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

    /**
     * Send this resource request asynchronously, without a request body.
     *
     * @param context The context that will be passed to authentication listener.
     * @param bytes The byte array containing the request body
     * @param BufferedSink The BufferedSink for the resource request.
     * @param listener The listener whose onSuccess or onFailure methods will be called when this request finishes.
     *
     */
    public void send(Context context, byte[] bytes, BufferedSink sink, ResponseListener listener){
        this.context = context;
        super.send(bytes, sink, listener);
    }

    @Override
    protected void sendRequest(final ResponseListener listener, final RequestBody requestBody) {
		AuthorizationManager authorizationManager = BMSClient.getInstance().getAuthorizationManager();
        String cachedAuthHeader = authorizationManager.getCachedAuthorizationHeader();

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
                if (numberOfRetries > 0) {
                    numberOfRetries--;
                    sendOKHttpRequest(request, getCallback(listener));
                } else {
                    if (listener != null) {
                        listener.onFailure(null, e, null);
                    }
                }
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                if (listener == null) {
                    return;
                }

				AuthorizationManager authorizationManager = BMSClient.getInstance().getAuthorizationManager();
				int responseCode = response.code();
				Map<String, List<String>> responseHeaders = response.headers().toMultimap();
				boolean isAuthorizationRequired = authorizationManager.isAuthorizationRequired(responseCode, responseHeaders);

                if (isAuthorizationRequired) {
                    if (oauthFailCounter++ < 2) {
                        authorizationManager.obtainAuthorization(
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
                    } else if (numberOfRetries > 0 && response.code() == 504) {
                        numberOfRetries--;
                        sendOKHttpRequest(response.request(), getCallback(listener));
                    } else {
                        listener.onFailure(new ResponseImpl(response), null, null);
                    }
                }
             //   response.body().close();
            }
        };
    }

    protected int getNumberOfRetries() {
        return numberOfRetries;
    }
}
