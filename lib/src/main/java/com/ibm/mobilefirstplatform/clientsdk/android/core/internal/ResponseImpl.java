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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.squareup.okhttp.internal.Util.UTF_8;

public class ResponseImpl implements Response {
    private static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + ResponseImpl.class.getSimpleName());
    private com.squareup.okhttp.Response okHttpResponse;
    private Headers headers;
    private MediaType contentType;
    private byte bodyBytes[];

    public ResponseImpl(com.squareup.okhttp.Response response) {
        okHttpResponse = response;

        if (okHttpResponse != null) {
            headers = okHttpResponse.headers();

            contentType = okHttpResponse.body().contentType();
        }
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
     * This method parses the response body as a String.
     *
     * @return The body of the response as a String. Empty string if there is no body.
     * @throws RuntimeException if the response text can not be parsed to a valid string or response
     * bytes can not be obtained.
     */
    public String getResponseText() {

        Charset charset = contentType != null ? contentType.charset(UTF_8) : UTF_8;
        try {
            bodyBytes = okHttpResponse.body().bytes();
            return new String(bodyBytes, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch(IOException i){
            throw new RuntimeException(i);
        }
    }

    /**
     * This method parses the response body as a JSONObject.
     *
     * @return The body of the response as a JSONObject.
     * @throws RuntimeException if response text can not be parsed to a valid string or if response text is not a valid JSON object.
     */
    public JSONObject getResponseJSON() {
        String responseText = getResponseText();

        if(responseText == null || responseText.length() == 0){
            return null;
        }

        try {
            return new JSONObject(responseText);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method gets the bytes of the response body.
     *
     * @return the bytes of the response body. Will be null if there is no body.
     * @throws RuntimeException if response bytes can not be obtained.
     */
    public byte[] getResponseBytes() {
        try {
            bodyBytes = okHttpResponse.body().bytes();
        } catch(IOException e){
            throw new RuntimeException(e);
        }

        return bodyBytes;
    }

    /** Returns true if this response redirects to another resource. */
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
     * Get the InputStream of the response body. If getResponseText or getBytes is called the InputStream will
     * be closed.
     *
     * @return An InputStream of the response body
     * @throws RuntimeException if the body can not obtain a byte stream
     */

    @Override
    public InputStream getResponseStream() {
        InputStream is;

        try {
            is = okHttpResponse.body().byteStream();
        } catch(IOException e){
            throw new RuntimeException(e);
        }

        return is;

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

    protected com.squareup.okhttp.Response getInternalResponse(){
        return okHttpResponse;
    }
}
