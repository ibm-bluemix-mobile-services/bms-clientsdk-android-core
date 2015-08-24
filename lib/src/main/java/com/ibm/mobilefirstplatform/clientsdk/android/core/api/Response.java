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

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;

import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.squareup.okhttp.internal.Util.UTF_8;

/**
 * This class has methods to get more details from the Response to the ResourceRequest.
 */
public class Response {
    private com.squareup.okhttp.Response okHttpResponse;
    private Headers headers;
    private MediaType contentType;
    private byte bodyBytes[];

    protected Response(com.squareup.okhttp.Response response) {
        okHttpResponse = response;

        if (okHttpResponse != null) {
            headers = okHttpResponse.headers();

            try {
                bodyBytes = okHttpResponse.body().bytes();
            } catch (Exception e) {
                bodyBytes = null;
            }

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
     * @return The body of the response as a String. Null if there is no body or exception occurred when building the response string.
     */
    public String getResponseText() {
        if (bodyBytes == null) {
            return null;
        }

        try {
            Charset charset = contentType != null ? contentType.charset(UTF_8) : UTF_8;
            return new String(bodyBytes, charset.name());
        } catch (Exception e) {
           return null;
        }
    }

    /**
     * This method parses the response body as a JSONObject.
     *
     * @return The body of the response as a JSONObject. Null if there is no body or if it is not a valid JSONObject.
     */
    public JSONObject getResponseJSON() {
        String responseText = getResponseText();

        if (responseText == null) {
            return null;
        }

        try {
            return new JSONObject(getResponseText());
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * This method gets the bytes of the response body.
     *
     * @return the bytes of the response body. Will be null if there is no body.
     */
    public byte[] getResponseBytes() {
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
    public Map<String, List<String>> getResponseHeaders() {
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
    public List<String> getResponseHeader(String name) {
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
    public String getFirstResponseHeader(String name) {
        List<String> headerValues = getResponseHeader(name);

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
        return "Response: Status=" + getStatus() + ", Response Text: " + getResponseText();
    }

    protected com.squareup.okhttp.Response getInternalResponse(){
        return okHttpResponse;
    }
}
