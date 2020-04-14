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

package com.ibm.mobilefirstplatform.clientsdk.android.core.internal;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.Headers;
import okhttp3.MediaType;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Contains response information from a {@link Request}.
 */
public class ResponseImpl implements Response {

    private okhttp3.Response okHttpResponse;
    private String requestURL;
    private Headers headers;
    private MediaType contentType;
    private InputStream responseByteStream;
    private byte[] bodyBytes;

    private static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + ResponseImpl.class.getSimpleName());

    // Convert OkHttp response into a BMSCore Response
    public ResponseImpl(okhttp3.Response response) {
        okHttpResponse = response;

        if (okHttpResponse != null) {
            requestURL = okHttpResponse.request().toString();
            headers = okHttpResponse.headers();
            try
            {
                contentType = okHttpResponse.body().contentType();
                responseByteStream = okHttpResponse.body().byteStream();
                this.bodyBytes = this.okHttpResponse.body().bytes();
            }
            catch (NullPointerException | IOException e)
            {
                logger.error("Response body bytes can't be read: " + e.getLocalizedMessage());
                this.bodyBytes = null;
            }
        }
    }

    /**
     * Returns the URL that the request was made to.
     *
     * @return The URL of the request.
     */
    public String getRequestURL() {
        return okHttpResponse.request().toString();
    }

    /**
     * This method gets the HTTP status of the response.
     *
     * @return The HTTP status of the response. Will be 0 when there was no response.
     */
    public int getStatus() {
        if (okHttpResponse == null) {
            return 0;
        }

        return okHttpResponse.code();
    }

    /**
     * This method gets the Content-Length of the response body.
     *
     * @return The content length of the response.
     */
    public long getContentLength() {
        try {
            return getInternalResponse().body().contentLength();
        } catch (NullPointerException e){
            logger.error("Failed to get the response content length from " + getRequestURL() + ". Error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * This method parses the response body as a String.
     * If this method is called, then subsequent calls to {@link #getResponseByteStream()} or {@link #getResponseBytes()}
     * will return null unless the {@link Request} was made using a <code>download()</code> method.
     *
     * @return The body of the response as a String. Empty string if there is no body.
     */
    public String getResponseText() {
        if (this.bodyBytes == null) {
            return "";
        }

        Charset charset = contentType != null ? contentType.charset(Charset.forName("UTF-8"))
            : Charset.forName("UTF-8");
        try {
            return new String(this.bodyBytes, charset.name());
        } catch (UnsupportedEncodingException e) {
            logger.warn("Failed to extract text from response body. Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * This method parses the response body as a JSONObject.
     * If this method is called, then subsequent calls to {@link #getResponseByteStream()} or {@link #getResponseBytes()}
     * will return null unless the {@link Request} was made using a <code>download()</code> method.
     *
     * @return The body of the response as a JSONObject.
     */
    public JSONObject getResponseJSON() {
        String responseText = getResponseText();

        if(responseText == null || responseText.length() == 0){
            return null;
        }

        try {
            return new JSONObject(responseText);
        } catch (JSONException e) {
            logger.warn("Failed to extract JSON from response body. Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * This method gets the bytes of the response body.
     * If this method is called, then subsequent calls to {@link #getResponseByteStream()} or {@link #getResponseBytes()}
     * will return null unless the {@link Request} was made using a <code>download()</code> method.
     *
     * @return the bytes of the response body. Will be null if there is no body.
     */
    public byte[] getResponseBytes() {
        if (responseByteStream != null) {
            try {
                return IOUtils.toByteArray(responseByteStream);
            }
            catch (IOException e) {
                logger.warn("Failed to extract byte array from response body. Error: " + e.getMessage());
                return null;
            }
        }
        return this.bodyBytes;
    }

    protected void setResponseBytes(byte[] responseBytes) {
        this.bodyBytes = responseBytes;
    }

    /**
     * This method gets the response body as an input stream.
     *
     * <p>
     * <b>Important: </b>This method may not be used for requests made with any of the {@link Request} download() methods,
     * since the stream will already be closed. Use {@link Response#getResponseBytes()} instead.
     * </p>
     *
     * @return The input stream representing the response body. Will be null if there is no body.
     */
    public InputStream getResponseByteStream() {
        return this.responseByteStream;
    }

    /**
     * Returns true if this response redirects to another resource.
     */
    public boolean isRedirect() {
        if (okHttpResponse == null) {
            return false;
        }
        return okHttpResponse.isRedirect();
    }

    /**
     * Returns true if the code is in [200..300), which means the request was
     * successfully received, understood, and accepted.
     */
    public boolean isSuccessful() {
        if (okHttpResponse == null) {
            return false;
        }
        return okHttpResponse.isSuccessful();
    }

    /**
     * Get the HTTP headers from the response.
     *
     * @return A map with all the headers, and the corresponding values for each one.
     */
    public Map<String, List<String>> getHeaders() {
        if (headers == null) {
            return null;
        }
        return headers.toMultimap();
    }

    /**
     * Get the header values for the given header name, if it exists. There can be more than one value
     * for a given header name.
     *
     * @param name the name of the header to get
     * @return the values of the given header name
     */
    public List<String> getHeader(String name) {
        if (headers == null) {
            return null;
        }
        return headers.values(name);
    }

    /**
     * Get the first header value for the given header name, if it exists.
     *
     * @param name the name of the header to get
     * @return the first value of the given header name
     */
    public String getFirstHeader(String name) {
        List<String> headerValues = getHeader(name);

        if (headerValues == null || headerValues.size() == 0) {
            return null;
        }

        return headerValues.get(0);
    }

    /**
     * Get the names of all the HTTP headers in the response.
     *
     * @return The names of all the headers in the response
     */
    public Set<String> getResponseHeadersNames() {
        if (headers == null) {
            return null;
        }
        return headers.names();
    }

    @Override
    public String toString() {
        try {
            return "Response: Status=" + getStatus() + ", Response Text: " + getResponseText();
        } catch (RuntimeException e) {
            return "Response: Status=" + getStatus() + ", Exception occurred when constructing response text string: " + e.getLocalizedMessage();
        }
    }

    protected okhttp3.Response getInternalResponse(){
        return okHttpResponse;
    }
}
