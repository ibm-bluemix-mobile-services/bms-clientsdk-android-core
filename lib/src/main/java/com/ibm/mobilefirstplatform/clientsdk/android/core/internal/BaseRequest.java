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
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

/**
 * This class is used to create and send a request. It allows to add all the parameters to the request
 * before sending it.
 */
public class BaseRequest {

    public static final int DEFAULT_TIMEOUT = 60000;
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String TEXT_PLAIN = "text/plain";

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

    private String url = null;
    private String method = null;
    private int timeout;

    private Map<String, String> queryParameters;
    private Headers.Builder headers = new Headers.Builder();

    private static final OkHttpClient httpClient = new OkHttpClient();

    static {
        SSLSocketFactory tlsEnabledSSLSocketFactory;
        try {
            tlsEnabledSSLSocketFactory = new TLSEnabledSSLSocketFactory();
            httpClient.setSslSocketFactory(tlsEnabledSSLSocketFactory);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructs a new request with the specified URL, using the specified HTTP method.
     *
     * @param url    The resource URL, may be either relative or absolute.
     * @param method The HTTP method to use
     */
    public BaseRequest(String url, String method) {
        this(url, method, DEFAULT_TIMEOUT);
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
        this.url = url;
        this.method = method;

        if(url != null && url.startsWith("/")) {
            this.url = convertRelativeURLToBluemixAbsolute(url);
        }

        setTimeout(timeout);
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

        OkHttpClient client = getHttpClient();

        client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
        client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Send this resource request asynchronously, without a request body.
     *
     * @param listener The listener whose onSuccess or onFailure methods will be called when this request finishes.
     */
    protected void send(ResponseListener listener) {
        send("", listener);
    }

    /**
     * Send this resource request asynchronously, with the given string as the request body.
     * If no content type header was set, this method will set it to "text/plain".
     *
     * @param requestBody The request body text
     * @param listener    The listener whose onSuccess or onFailure methods will be called when this request finishes.
     */
    protected void send(final String requestBody, final ResponseListener listener) {
        String contentType = headers.get(CONTENT_TYPE);

        if (contentType == null) {
            contentType = TEXT_PLAIN;
        }

        RequestBody body = RequestBody.create(MediaType.parse(contentType), requestBody);

        sendRequest(listener, body);
    }

    /**
     * Send this resource request asynchronously, with the given form parameters as the request body.
     * This method will set the content type header to "application/x-www-form-urlencoded".
     *
     * @param formParameters The parameters to put in the request body
     * @param listener       The listener whose onSuccess or onFailure methods will be called when this request finishes.
     */
    protected void send(Map<String, String> formParameters, ResponseListener listener) {
        FormEncodingBuilder formBuilder = new FormEncodingBuilder();

        for (Map.Entry<String, String> param : formParameters.entrySet()) {
            formBuilder.add(param.getKey(), param.getValue());
        }

        RequestBody body = formBuilder.build();

        sendRequest(listener, body);
    }

    /**
     * Send this resource request asynchronously, with the given JSON object as the request body.
     * If no content type header was set, this method will set it to "application/json".
     *
     * @param json     The JSON object to put in the request body
     * @param listener The listener whose onSuccess or onFailure methods will be called when this request finishes.
     */
    protected void send(JSONObject json, ResponseListener listener) {
        String contentType = headers.get(CONTENT_TYPE);

        if (contentType == null) {
            contentType = JSON_CONTENT_TYPE;
        }

        RequestBody body = RequestBody.create(MediaType.parse(contentType), json.toString());

        sendRequest(listener, body);
    }

    /**
     * Send this resource request asynchronously, with the content of the given byte array as the request body.
     * Note that this method does not set any content type header, if such a header is required it must be set before calling this method.
     *
     * @param data     The byte array containing the request body
     * @param listener The listener whose onSuccess or onFailure methods will be called when this request finishes.
     */
    protected void send(byte[] data, ResponseListener listener) {
        RequestBody body = RequestBody.create(MediaType.parse(headers.get(CONTENT_TYPE)), data);

        sendRequest(listener, body);
    }

    /** Configure this client to follow redirects.
     * If unset, redirects be followed.
     */
    public void setFollowRedirects(boolean followRedirects) {
        getHttpClient().setFollowRedirects(followRedirects);
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

    protected void sendRequest(final ResponseListener listener, final RequestBody requestBody) {
        if(method == null || !isValidMethod(method)){
            listener.onFailure(null, new IllegalArgumentException("Method is not valid: " + method), null);
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
            listener.onFailure(null, e, null);
            return;
        }

        //A GET or HEAD request cannot have a body in OKHTTP
        if(method.equalsIgnoreCase(BaseRequest.GET)) {
            requestBuilder.get();
        }
        else if(!method.equalsIgnoreCase(BaseRequest.HEAD)){
            requestBuilder.head();
        }
        else {
            requestBuilder.method(method, requestBody);
        }

        Request request = requestBuilder.build();
        OkHttpClient client = getHttpClient();
        client.newCall(request).enqueue(getCallback(listener));
        client.newCall(request);

    }

    protected Callback getCallback(final ResponseListener listener) {
        return new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                listener.onFailure(null, e, null);
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                if (response.isSuccessful() || response.isRedirect()) {
                    listener.onSuccess(new ResponseImpl(response));
                } else {
                    listener.onFailure(new ResponseImpl(response), null, null);
                }
            }
        };
    }

    protected OkHttpClient getHttpClient(){
        return httpClient;
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
	public static void setCookieManager(CookieManager cookieManager){
		httpClient.setCookieHandler(cookieManager);
	}
}
