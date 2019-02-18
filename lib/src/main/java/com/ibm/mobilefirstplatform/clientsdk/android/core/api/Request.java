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
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import okhttp3.Call;
import okhttp3.RequestBody;
import okhttp3.Callback;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * This class is used to create and send network requests.
 */
public class Request extends BaseRequest {

    private static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + Request.class.getSimpleName());

    private int oauthFailCounter = 0; // Number of times the request failed authentication
    private RequestBody savedRequestBody; // Used to resend the original request after successful authentication
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


    //region Send

    /**
     * Send this resource request asynchronously, without a request body.
     *
     * @param context   The context that will be passed to authentication listener.
     * @param listener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void send(Context context, ResponseListener listener) {
        setContext(context);
        super.send(listener);
    }

    /**
     * Send this resource request asynchronously, with the given string as the request body.
     * If no Content-Type header was set, this method will set it to "text/plain".
     *
     * @param context   The context that will be passed to authentication listener.
     * @param text      The text to put in the request body
     * @param listener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void send(Context context, String text, ResponseListener listener) {
        setContext(context);
        super.send(text, listener);
    }

    /**
     * Send this resource request asynchronously, with the given form parameters as the request body.
     * If no Content-Type header was set, this method will set it to "application/x-www-form-urlencoded".
     *
     * @param context           The context that will be passed to authentication listener.
     * @param formParameters    The parameters to put in the request body
     * @param listener          The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void send(Context context, Map<String, String> formParameters, ResponseListener listener) {
        setContext(context);
        super.send(formParameters, listener);
    }

    /**
     * Send this resource request asynchronously, with the given JSON object as the request body.
     * If no Content-Type header was set, this method will set it to "application/json".
     *
     * @param context   The context that will be passed to authentication listener.
     * @param json      The JSON object to put in the request body
     * @param listener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void send(Context context, JSONObject json, ResponseListener listener) {
        setContext(context);
        super.send(json, listener);
    }

    /**
     * Send this resource request asynchronously, with the given byte array as the request body.
     * This method does not set any Content-Type header; if such a header is required, it must be set before calling this method.
     *
     * @param context   The context that will be passed to authentication listener.
     * @param data      The byte array to put in the request body
     * @param listener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void send(Context context, byte[] data, ResponseListener listener) {
        setContext(context);
        super.send(data, listener);
    }

    //endregion


    // region Download

    /**
     * <p>
     * Download this resource asynchronously, without a request body.
     * The download progress will be monitored with a {@link ProgressListener}.
     * </p>
     *
     * <p>
     * <b>Note: </b>This method consumes the <code>InputStream</code> from the response and closes it,
     * so the {@link Response#getResponseByteStream()} method will always return null for this request.
     * </p>
     *
     * @param context           The context that will be passed to authentication listener.
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void download(Context context, ProgressListener progressListener, ResponseListener responseListener) {
        setContext(context);
        super.download(progressListener, responseListener);
    }

    /**
     * <p>
     * Download this resource asynchronously, with the given string as the request body.
     * The download progress will be monitored with a {@link ProgressListener}.
     * If no Content-Type header was set, this method will set it to "text/plain".
     * </p>
     *
     * <p>
     * <b>Note: </b>This method consumes the <code>InputStream</code> from the response and closes it,
     * so the {@link Response#getResponseByteStream()} method will always return null for this request.
     * </p>
     *
     * @param context           The context that will be passed to authentication listener.
     * @param requestBody       The text to put in the request body
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void download(Context context, final String requestBody, ProgressListener progressListener, final ResponseListener responseListener) {
        setContext(context);
        super.download(requestBody, progressListener, responseListener);
    }

    /**
     * <p>
     * Download this resource asynchronously, with the given form parameters as the request body.
     * The download progress will be monitored with a {@link ProgressListener}.
     * If no Content-Type header was set, this method will set it to "application/x-www-form-urlencoded".
     * </p>
     *
     * <p>
     * <b>Note: </b>This method consumes the <code>InputStream</code> from the response and closes it,
     * so the {@link Response#getResponseByteStream()} method will always return null for this request.
     * </p>
     *
     * @param context           The context that will be passed to authentication listener.
     * @param formParameters    The parameters to put in the request body
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void download(Context context, Map<String, String> formParameters, ProgressListener progressListener, ResponseListener responseListener) {
        setContext(context);
        super.download(formParameters, progressListener, responseListener);
    }

    /**
     * <p>
     * Download this resource asynchronously, with the given JSON object as the request body.
     * The download progress will be monitored with a {@link ProgressListener}.
     * If no Content-Type header was set, this method will set it to "application/json".
     * </p>
     *
     * <p>
     * <b>Note: </b>This method consumes the <code>InputStream</code> from the response and closes it,
     * so the {@link Response#getResponseByteStream()} method will always return null for this request.
     * </p>
     *
     * @param context           The context that will be passed to authentication listener.
     * @param json              The JSON object to put in the request body
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void download(Context context, JSONObject json, ProgressListener progressListener, ResponseListener responseListener) {
        setContext(context);
        super.download(json, progressListener, responseListener);
    }

    /**
     * <p>
     * Download this resource asynchronously, with the given byte array as the request body.
     * The download progress will be monitored with a {@link ProgressListener}.
     * This method does not set any Content-Type header; if such a header is required, it must be set before calling this method.
     * </p>
     *
     * <p>
     * <b>Note: </b>This method consumes the <code>InputStream</code> from the response and closes it,
     * so the {@link Response#getResponseByteStream()} method will always return null for this request.
     * </p>
     *
     * @param context           The context that will be passed to authentication listener.
     * @param data              The byte array to put in the request body
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void download(Context context, byte[] data, ProgressListener progressListener, ResponseListener responseListener) {
        setContext(context);
        super.download(data, progressListener, responseListener);
    }

    // endregion


    // region Upload

    /**
     * Upload text asynchronously.
     * If no Content-Type header was set, this method will set it to "text/plain".
     *
     * @param context           The context that will be passed to authentication listener.
     * @param text              The text to upload
     * @param progressListener  The listener that monitors the upload progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void upload(Context context, final String text, final ProgressListener progressListener, ResponseListener responseListener) {
        setContext(context);
        super.upload(text, progressListener, responseListener);
    }

    /**
     * Upload a byte array asynchronously.
     * This method does not set any Content-Type header; if such a header is required, it must be set before calling this method.
     *
     * @param context           The context that will be passed to authentication listener.
     * @param data              The byte array to upload
     * @param progressListener  The listener that monitors the upload progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void upload(Context context, final byte[] data, final ProgressListener progressListener, ResponseListener responseListener) {
        setContext(context);
        super.upload(data, progressListener, responseListener);
    }

    /**
     * Upload a file asynchronously.
     * This method does not set any Content-Type header; if such a header is required, it must be set before calling this method.
     *
     * @param context           The context that will be passed to authentication listener.
     * @param file              The file to upload
     * @param progressListener  The listener that monitors the upload progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    public void upload(Context context, final File file, final ProgressListener progressListener, ResponseListener responseListener) {
        setContext(context);
        super.upload(file, progressListener, responseListener);
    }


    void setContext(Context context) {
        this.context = context;
    }

    // endregion


    @Override
    protected void sendRequest(final ProgressListener progressListener, final ResponseListener listener, final RequestBody requestBody) {
        // Add authorization header if this request is being made to a protected resource
        AuthorizationManager authorizationManager = BMSClient.getInstance().getAuthorizationManager();
        String cachedAuthHeader = authorizationManager.getCachedAuthorizationHeader();
        if (cachedAuthHeader != null) {
            removeHeaders("Authorization");
            addHeader("Authorization", cachedAuthHeader);
        }

        savedRequestBody = requestBody;
        super.sendRequest(progressListener, listener, requestBody);
    }

    @Override
    protected Callback getCallback(final ProgressListener progressListener, final ResponseListener responseListener) {
        final RequestBody requestBody = savedRequestBody;
        final Request request = this;
        final Context ctx = this.context;

        return new Callback() {


            // The request failed to complete, so no response was received from the server.
            @Override
            public void onFailure(Call call, IOException e){
                // If auto-retries are enabled, and the request hasn't run out of retry attempts,
                // then try to send the same request again. Otherwise, delegate to the user's ResponseListener.
                // Note that we also retry requests that receive 504 responses, as seen in the onResponse() method.
                if (numberOfRetries > 0) {
                    numberOfRetries--;
                    logger.debug("Resending " + call.request().method() +  " request to " + call.request().toString());
                    sendOKHttpRequest(call.request(), getCallback(progressListener, responseListener));
                } else {
                    if (responseListener != null) {
                        responseListener.onFailure(null, e, null);
                    }
                }
            }

            // If this method is reached, a response (of any type) has been received from the server.
            // This does not always indicate a successful response.
            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (responseListener == null || response == null) {
                    return;
                }

                // If the request is made to a protected endpoint, see if we need to use AuthorizationManager
                // to authenticate by resending the request with the correct authorization header.
                AuthorizationManager authorizationManager = BMSClient.getInstance().getAuthorizationManager();

                int responseCode = response.code();
                Map<String, List<String>> responseHeaders = response.headers().toMultimap();
                boolean isAuthorizationRequired = authorizationManager.isAuthorizationRequired(responseCode, responseHeaders);

                if (isAuthorizationRequired) {

                    // The first oauthFailCounter gets triggered by a 401 (the server is requesting authentication)
                    // If the oauthFailCounter gets incremented again, then authentication has failed.
                    if (oauthFailCounter++ < 2) {
                        authorizationManager.obtainAuthorization(
                                ctx,
                                new ResponseListener() {
                                    @Override
                                    public void onSuccess(Response response) {
                                        // this will take the auth hader that has been cached by obtainAuthorizationHeader
                                        request.sendRequest(progressListener, responseListener, requestBody);
                                    }

                                    @Override
                                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                                        responseListener.onFailure(response, t, extendedInfo);
                                    }
                                }
                        );
                    } else {
                        responseListener.onFailure(new ResponseImpl(response), null, null);
                    }
                } else {

                    // If the response is successful, delegate to the user's
                    //      1) ResponseListener
                    //      2) ProgressListener (if applicable)
                    if (response.isSuccessful() || response.isRedirect()) {
                        Response bmsResponse = new ResponseImpl(response);
                        if (progressListener != null) {
                            updateProgressListener(progressListener, bmsResponse);
                        }
                        responseListener.onSuccess(bmsResponse);

                        // If auto-retries are enabled, and the request hasn't run out of retry attempts,
                        // then try to send the same request again. Otherwise, delegate to the user's ResponseListener.
                    } else if (numberOfRetries > 0 && response.code() == 504) {
                        numberOfRetries--;
                        logger.debug("Resending " + request.getMethod() +  " request to " + request.getUrl());
                        sendOKHttpRequest(response.request(), getCallback(progressListener, responseListener));
                    } else {
                        responseListener.onFailure(new ResponseImpl(response), null, null);
                    }
                }
                response.body().close();
            }
        };
    }

    protected int getNumberOfRetries() {
        return numberOfRetries;
    }
}
