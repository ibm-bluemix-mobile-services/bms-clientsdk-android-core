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

package com.ibm.mobilefirstplatform.clientsdk.android.core.internal;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ProgressListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * This class is used to create and send a request. It allows to add all the parameters to the request
 * before sending it.
 */
public class BaseRequest {

    public static final int DEFAULT_TIMEOUT = 60000;

    // Header key
    public static final String CONTENT_TYPE = "Content-Type";

    // Header values
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String BINARY_CONTENT_TYPE = "application/octet-stream";
    public static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";


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

    // The number of times the user wants a request to automatically resend if it fails
    protected int numberOfRetries;

    private static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + BaseRequest.class.getSimpleName());

    private String url = null;
    private String method = null;
    private int timeout;

    private Map<String, String> queryParameters;
    private Headers.Builder headers = new Headers.Builder();

    private static final OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    static {
        SSLSocketFactory tlsEnabledSSLSocketFactory;
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };
            tlsEnabledSSLSocketFactory = new TLSEnabledSSLSocketFactory();
            httpClient.sslSocketFactory(tlsEnabledSSLSocketFactory, (X509TrustManager)trustAllCerts[0]);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Constructs a new request with the specified URL, using the specified HTTP method.
     *
     * @param url    The resource URL, may be either relative or absolute.
     * @param method The HTTP method to use
     */
    public BaseRequest(String url, String method) {
        this(url, method, DEFAULT_TIMEOUT, 0);
    }

    /**
     * Constructs a new request with the specified URL, using the specified HTTP method.
     * Additionally this constructor sets a custom timeout.
     *
     * @param url     The resource URL
     * @param method  The HTTP method to use.
     * @param timeout The timeout in milliseconds for this request.
     */
    public BaseRequest(String url, String method, int timeout) {
        this(url, method, timeout, 0);
    }

    /**
     * Constructs a new request with the specified URL, using the specified HTTP method.
     * Additionally this constructor sets a custom timeout and number of times to automatically
     * retry failed requests.
     *
     * @param url           The resource URL
     * @param method        The HTTP method to use.
     * @param timeout       The timeout in milliseconds for this request.
     * @param autoRetries   The number of times to retry each request if it fails due to timeout or loss of network connection.
     */
    public BaseRequest(String url, String method, int timeout, int autoRetries) {
        this.url = url;
        this.method = method;
        this.numberOfRetries = autoRetries;

        if(url != null && url.startsWith("/")) {
            this.url = convertRelativeURLToBluemixAbsolute(url);
        }

        removeTrailingSlash();

        setTimeout(timeout);
    }

    protected void removeTrailingSlash() {
        if(this.url != null && this.url.endsWith("/")){
            this.url = this.url.substring(0, this.url.length() - 1);
        }
    }

    private String convertRelativeURLToBluemixAbsolute(String url) {
        String appRoute = BMSClient.getInstance().getBluemixAppRoute();

        return appRoute + url;
    }

    private Map<String, String> getQueryParamsMap() {
        if (queryParameters == null) {
            queryParameters = new HashMap<String, String>();
        }

        return queryParameters;
    }

    /**
     * Returns the URL for this resource request.
     *
     * @return String The URL representing the path for this resource request.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the HTTP method for this resource request.
     *
     * @return A string containing the name of the HTTP method.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the query parameters for this resource request.
     *
     * @param parameters A HashMap containing the query parameters
     */
    public void setQueryParameters(Map<String, String> parameters) {
        queryParameters = parameters;
    }

    /**
     * Returns the query parameters set for this resource request.
     *
     * @return A Map containing the query parameters
     */
    public Map<String, String> getQueryParameters() {
        return getQueryParamsMap();
    }

    /**
     * Sets the value of the given query parameter name to the given value.
     *
     * @param name  The name of the parameter to set
     * @param value The value of the parameter to set
     */
    public void setQueryParameter(String name, String value) {
        getQueryParamsMap().put(name, value);
    }

    /**
     * Returns all the headers that were set for this resource request.
     *
     * @return An array of Headers
     */
    public Map<String, List<String>> getAllHeaders() {
        return headers.build().toMultimap();
    }

    /**
     * Returns all the headers for this resource request that have the given name.
     *
     * @param headerName The name of the headers to return
     * @return A String array with all the values for the given header
     */
    public List<String> getHeaders(String headerName) {
        return headers.build().values(headerName);
    }

    /**
     * Removes all the headers for this resource request with the given name.
     *
     * @param headerName The name of the headers to remove
     */
    public void removeHeaders(String headerName) {
        headers.removeAll(headerName);
    }

    /**
     * Sets headers for this resource request. Overrides all headers previously added.
     *
     * @param headerMap A multimap containing the header names and corresponding values
     */
    public void setHeaders(Map<String, List<String>> headerMap) {
        headers = new Headers.Builder();

        for (Map.Entry<String, List<String>> e : headerMap.entrySet()) {
            for (String headerValue : e.getValue()) {
                addHeader(e.getKey(), headerValue);
            }
        }
    }

    /**
     * Adds a header to this resource request. This method allows request headers to have multiple values.
     *
     * @param name  The name of the header to add
     * @param value The value of the header to add
     */
    public void addHeader(String name, String value) {
        headers.add(name, value);
    }

    /**
     * Returns the timeout for this resource request.
     *
     * @return the timeout for this resource request
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout for this resource request.
     *
     * @param timeout The timeout for this request procedure
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;



        httpClient.connectTimeout(timeout, TimeUnit.MILLISECONDS);
        httpClient.readTimeout(timeout, TimeUnit.MILLISECONDS);
        httpClient.writeTimeout(timeout, TimeUnit.MILLISECONDS);

    }

    /**
     * Send this resource request asynchronously, without a request body.
     *
     * @param listener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void send(ResponseListener listener) {
        send("", listener);
    }

    /**
     * Send this resource request asynchronously, with the given string as the request body.
     * If the Content-Type header was not previously set, this method will set it to "text/plain".
     *
     * @param requestBody   The text to put in the request body
     * @param listener      The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void send(final String requestBody, final ResponseListener listener) {
        String contentType = headers.get(CONTENT_TYPE);

        if (contentType == null) {
            contentType = TEXT_PLAIN_CONTENT_TYPE;
        }

        // If the request body is an empty string, it should be treated as null
        RequestBody body = null;
        if (requestBody != null && requestBody.length() > 0) {
            body = RequestBody.create(MediaType.parse(contentType), requestBody);
        }

        sendRequest(null, listener, body);
    }

    /**
     * Send this resource request asynchronously, with the given form parameters as the request body.
     * If the Content-Type header was not previously set, this method will set it to "application/x-www-form-urlencoded".
     *
     * @param formParameters    The form parameters to put in the request body
     * @param listener          The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void send(Map<String, String> formParameters, ResponseListener listener) {

        FormBody.Builder formBuilder = new FormBody.Builder();

        for ( Map.Entry<String, String> entry : formParameters.entrySet() ) {
            formBuilder.add( entry.getKey(), entry.getValue() );
        }

        RequestBody body = formBuilder.build();

        sendRequest(null, listener, body);
    }

    /**
     * Send this resource request asynchronously, with the given JSON object as the request body.
     * If the Content-Type header was not previously set, this method will set it to "application/json".
     *
     * @param json      The JSON object to put in the request body
     * @param listener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void send(JSONObject json, ResponseListener listener) {
        String contentType = headers.get(CONTENT_TYPE);

        if (contentType == null) {
            contentType = JSON_CONTENT_TYPE;
        }

        RequestBody body = RequestBody.create(MediaType.parse(contentType), json.toString());

        sendRequest(null, listener, body);
    }

    /**
     * Send this resource request asynchronously, with the given byte array as the request body.
     * If the Content-Type header was not previously set, this method will set it to "application/octet-stream".
     *
     * @param data      The byte array to put in the request body
     * @param listener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void send(byte[] data, ResponseListener listener) {
        String contentTypeHeader = headers.get(CONTENT_TYPE);
        final String contentType = contentTypeHeader != null ? contentTypeHeader : BINARY_CONTENT_TYPE;

        RequestBody body = RequestBody.create(MediaType.parse(contentType), data);

        sendRequest(null, listener, body);
    }

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
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void download(final ProgressListener progressListener, ResponseListener responseListener) {
        download("", progressListener, responseListener);
    }

    /**
     * <p>
     * Download this resource asynchronously, with the given string as the request body.
     * The download progress will be monitored with a {@link ProgressListener}.
     * If the Content-Type header was not previously set, this method will set it to "text/plain".
     * </p>
     *
     * <p>
     * <b>Note: </b>This method consumes the <code>InputStream</code> from the response and closes it,
     * so the {@link Response#getResponseByteStream()} method will always return null for this request.
     * </p>
     *
     * @param requestBody       The text to put in the request body
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void download(final String requestBody, final ProgressListener progressListener, final ResponseListener responseListener) {
        String contentType = headers.get(CONTENT_TYPE);

        if (contentType == null) {
            contentType = TEXT_PLAIN_CONTENT_TYPE;
        }

        // If the request body is an empty string, it should be treated as an null
        RequestBody body = null;
        if (requestBody != null && !requestBody.equals("")) {
            body = RequestBody.create(MediaType.parse(contentType), requestBody);
        }

        sendRequest(progressListener, responseListener, body);
    }

    /**
     * <p>
     * Download this resource asynchronously, with the given form parameters as the request body.
     * The download progress will be monitored with a {@link ProgressListener}.
     * If the Content-Type header was not previously set, this method will set it to "application/x-www-form-urlencoded".
     * </p>
     *
     * <p>
     * <b>Note: </b>This method consumes the <code>InputStream</code> from the response and closes it,
     * so the {@link Response#getResponseByteStream()} method will always return null for this request.
     * </p>
     *
     * @param formParameters    The parameters to put in the request body
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void download(Map<String, String> formParameters, final ProgressListener progressListener, ResponseListener responseListener) {
        FormBody.Builder formBuilder = new FormBody.Builder();

        for ( Map.Entry<String, String> entry : formParameters.entrySet() ) {
            formBuilder.add( entry.getKey(), entry.getValue() );
        }

        RequestBody body = formBuilder.build();

        sendRequest(progressListener, responseListener, body);
    }

    /**
     * <p>
     * Download this resource asynchronously, with the given JSON object as the request body.
     * The download progress will be monitored with a {@link ProgressListener}.
     * If the Content-Type header was not previously set, this method will set it to "application/json".
     * </p>
     *
     * <p>
     * <b>Note: </b>This method consumes the <code>InputStream</code> from the response and closes it,
     * so the {@link Response#getResponseByteStream()} method will always return null for this request.
     * </p>
     *
     * @param json              The JSON object to put in the request body
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void download(JSONObject json, final ProgressListener progressListener, ResponseListener responseListener) {
        String contentType = headers.get(CONTENT_TYPE);

        if (contentType == null) {
            contentType = JSON_CONTENT_TYPE;
        }

        RequestBody body = RequestBody.create(MediaType.parse(contentType), json.toString());

        sendRequest(progressListener, responseListener, body);
    }

    /**
     * <p>
     * Download this resource asynchronously, with the given byte array as the request body.
     * The download progress will be monitored with a {@link ProgressListener}.
     * If the Content-Type header was not previously set, this method will set it to "application/octet-stream".
     * </p>
     *
     * <p>
     * <b>Note: </b>This method consumes the <code>InputStream</code> from the response and closes it,
     * so the {@link Response#getResponseByteStream()} method will always return null for this request.
     * </p>
     *
     * @param data              The byte array to put in the request body
     * @param progressListener  The listener that monitors the download progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void download(byte[] data, final ProgressListener progressListener, ResponseListener responseListener) {
        String contentTypeHeader = headers.get(CONTENT_TYPE);
        final String contentType = contentTypeHeader != null ? contentTypeHeader : BINARY_CONTENT_TYPE;

        RequestBody body = RequestBody.create(MediaType.parse(contentType), data);

        sendRequest(progressListener, responseListener, body);
    }

    /**
     * Upload text asynchronously.
     * If the Content-Type header was not previously set, this method will set it to "text/plain".
     *
     * @param text              The text to upload
     * @param progressListener  The listener that monitors the upload progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void upload(final String text, final ProgressListener progressListener, ResponseListener responseListener) {
        if (text == null || text.length() == 0) {
            if (responseListener != null) {
                responseListener.onFailure(null, new IllegalArgumentException("Tried to upload empty text"), null);
            }
            return;
        }

        String contentTypeHeader = headers.get(CONTENT_TYPE);
        final String contentType = contentTypeHeader != null ? contentTypeHeader : TEXT_PLAIN_CONTENT_TYPE;

        RequestBody body = RequestBody.create(MediaType.parse(contentType), text);


        // Custom RequestBody wrapper that monitors the progress of the upload

        ProgressRequestBody progressBody = new ProgressRequestBody(text, body, progressListener);

        sendRequest(null, responseListener, progressBody);
    }

    /**
     * Upload a byte array asynchronously.
     * If the Content-Type header was not previously set, this method will set it to "application/octet-stream".
     *
     * @param data              The byte array to upload
     * @param progressListener  The listener that monitors the upload progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void upload(final byte[] data, final ProgressListener progressListener, ResponseListener responseListener) {
        if (data == null || data.length == 0) {
            if (responseListener != null) {
                responseListener.onFailure(null, new IllegalArgumentException("Tried to upload empty byte array"), null);
            }
            return;
        }

        String contentTypeHeader = headers.get(CONTENT_TYPE);
        final String contentType = contentTypeHeader != null ? contentTypeHeader : BINARY_CONTENT_TYPE;

        RequestBody body = RequestBody.create(MediaType.parse(contentType), data);
        // Custom RequestBody wrapper that monitors the progress of the upload
        ProgressRequestBody progressBody = new ProgressRequestBody(data, body, progressListener);

        sendRequest(null, responseListener, progressBody);
    }

    /**
     * Upload a file asynchronously.
     * If the Content-Type header was not previously set, this method will set it to "application/octet-stream".
     *
     * @param file              The file to upload
     * @param progressListener  The listener that monitors the upload progress
     * @param responseListener  The listener whose onSuccess or onFailure methods will be called when this request finishes
     */
    protected void upload(final File file, final ProgressListener progressListener, ResponseListener responseListener) {
        String contentTypeHeader = headers.get(CONTENT_TYPE);
        final String contentType = contentTypeHeader != null ? contentTypeHeader : BINARY_CONTENT_TYPE;

        RequestBody body = RequestBody.create(MediaType.parse(contentType), file);
        // Custom RequestBody wrapper that monitors the progress of the upload

        ProgressRequestBody progressBody = new ProgressRequestBody(file, body, progressListener);

        sendRequest(null, responseListener, progressBody);
    }

    /**
     * Configure this request to follow redirects.
     * If unset, redirects be followed by default.
     */
    public void setFollowRedirects(boolean followRedirects) {

        httpClient.followSslRedirects(followRedirects);
    }

    protected URL getURLWithQueryParameters(String url, Map<String, String> queryParameters) throws MalformedURLException {
        if (url == null || queryParameters == null || queryParameters.size() == 0) {
            return new URL(url);
        }

        String queryParamsURLFragment = "";

        int i = 0;

        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            if (i > 0) {
                queryParamsURLFragment += "&";
            }

            queryParamsURLFragment += entry.getKey() + "=" + entry.getValue();

            i++;
        }

        String newURL = url;

        if (!urlContainsURIPath(newURL)) {
            newURL += "/";
        }

        if (!newURL.contains("?")) {
            queryParamsURLFragment = "?" + queryParamsURLFragment;
        }

        return new URL(newURL + queryParamsURLFragment);
    }

    protected boolean urlContainsURIPath(String url) {
        int slashCount = 0;
        int lastIndex = 0;

        while (lastIndex != -1) {

            lastIndex = url.indexOf("/", lastIndex);

            if (lastIndex != -1) {
                slashCount++;
                lastIndex += 1;
            }
        }
        return slashCount >= 3;
    }

    private boolean isValidMethod(String method) {
        return method.equalsIgnoreCase(PUT) ||
                method.equalsIgnoreCase(POST) ||
                method.equalsIgnoreCase(GET) ||
                method.equalsIgnoreCase(DELETE) ||
                method.equalsIgnoreCase(TRACE) ||
                method.equalsIgnoreCase(HEAD) ||
                method.equalsIgnoreCase(OPTIONS);
    }

    protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
        if(method == null || !isValidMethod(method)){
            if (responseListener != null) {
                responseListener.onFailure(null, new IllegalArgumentException("Method is not valid: " + method), null);
            }
            return;
        }

        Request.Builder requestBuilder = new Request.Builder();

        requestBuilder.headers(headers.build());

        try {
            if (getQueryParamsMap().size() == 0) {
                requestBuilder.url(url);
            } else {
                requestBuilder.url(getURLWithQueryParameters(url, getQueryParamsMap()));
            }
        } catch (MalformedURLException e) {
            if (responseListener != null) {
                responseListener.onFailure(null, e, null);
            }
            return;
        }

        //A GET or HEAD request cannot have a body in OKHTTP
        if(method.equalsIgnoreCase(BaseRequest.GET)) {
            if (requestBody != null) {
                logger.warn("Request body ignored for request to " + url + " because it is a GET request.");
            }
            requestBuilder.get();
        }
        else if(method.equalsIgnoreCase(BaseRequest.HEAD)){
            if (requestBody != null) {
                logger.warn("Request body ignored for request to " + url + " because it is a HEAD request.");
            }
            requestBuilder.head();
        }
        else {
            requestBuilder.method(method, requestBody);
        }

        Request request = requestBuilder.build();
        sendOKHttpRequest(request, getCallback(progressListener, responseListener));
    }

    // Hands off the request to OkHttp
    protected void sendOKHttpRequest(Request request, final Callback callback) {
        OkHttpClient client = httpClient.build();

        client.newCall(request).enqueue(callback);
    }

    protected Callback getCallback(final ProgressListener progressListener, final ResponseListener responseListener) {
        return new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

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

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {

                if (responseListener == null) {
                    return;
                }

                // If the response is successful, delegate to the user's
                //      1) ResponseListener
                //      2) ProgressListener (if applicable)
                if (response.isSuccessful() || response.isRedirect()) {
                    Response bmsResponse = new ResponseImpl(response);
                    if (progressListener != null) {
                        updateProgressListener(progressListener, bmsResponse);
                    }
                    responseListener.onSuccess(bmsResponse);
                } else {
                    responseListener.onFailure(new ResponseImpl(response), null, null);
                }
            }
        };
    }

    // As a download request progresses, periodically call the user's ProgressListener
    protected void updateProgressListener(ProgressListener progressListener, Response response) {
        InputStream responseStream = response.getResponseByteStream();

        int bytesDownloaded = 0;
        long totalBytesExpected = response.getContentLength();
        // Reading 2 KiB at a time to be consistent with the upload segment size in ProgressRequestBody
        final int segmentSize = 2048;

        byte[] responseBytes;
        if (totalBytesExpected > Integer.MAX_VALUE) {
            logger.warn("The response body for " + getUrl() + " is too large to hold in a byte array. Only the first 2 GiB will be available.");
            responseBytes = new byte[Integer.MAX_VALUE];
        }
        else {
            responseBytes = new byte[(int)totalBytesExpected];
        }

        // For every 2 KiB downloaded:
        //      1) Call the user's ProgressListener
        //      2) Append the downloaded bytes into a byte array
        int nextByte;
        try {
            while ((nextByte = responseStream.read()) != -1) {
                if (bytesDownloaded % segmentSize == 0) {
                    progressListener.onProgress(bytesDownloaded, totalBytesExpected);
                }
                responseBytes[bytesDownloaded] = (byte)nextByte;
                bytesDownloaded += 1;
            }
        }
        catch (IOException e) {
            logger.error("IO Exception: " + e.getMessage());
        }

        // Transfer the downloaded data to the Response object so that the user can later retrieve it
        if (response instanceof ResponseImpl) {
            ((ResponseImpl)response).setResponseBytes(responseBytes);
        }
    }

    /**
     * @exclude
     */
    public static void registerInterceptor(Interceptor interceptor) {
        if (interceptor != null) {
            httpClient.networkInterceptors().add(interceptor);
        }
    }

    /**
     * @exclude
     */
    public static void unregisterInterceptor(Interceptor interceptor) {
        if (interceptor != null) {
            httpClient.networkInterceptors().remove(interceptor);
        }
    }

    /**
     * @exclude
     */
    public static void setCookieManager(CookieManager CookieManager){
        httpClient.cookieJar(new JavaNetCookieJar(CookieManager));
    }
}
