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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * This class has methods to get more details from the Response to the BaseRequest.
 */
public interface  Response {

    /**
     * Returns the URL that the request was made to.
     *
     * @return The URL of the request.
     */
    String getRequestURL();

    /**
     * This method gets the HTTP status of the response.
     *
     * @return The HTTP status of the response. Will be 0 when there was no response.
     */
    int getStatus();

    /**
     * <p>
     * This method parses the response body as a String.
     * If this method is called, then subsequent calls to {@link #getResponseByteStream()} or {@link #getResponseBytes()}
     * will return null.
     * </p>
     *
     * <p>
     * <b>Important: </b>This method may not be used for requests made with any of the {@link Request} download() methods.
     * </p>
     *
     * @return The body of the response as a String. Empty string if there is no body.
     * @throws RuntimeException if the response text can not be parsed to a valid string.
     */
    String getResponseText();

    /**
     * <p>
     * This method gets the bytes of the response body.
     * If this method is called, then subsequent calls to {@link #getResponseByteStream()} or {@link #getResponseText()}
     * will return null.
     * </p>
     *
     * <p>
     * <b>Important: </b>This method may not be used for requests made with any of the {@link Request} download() methods.
     * </p>
     *
     * @return the bytes of the response body. Will be null if there is no body.
     */
    byte[] getResponseBytes();

    /**
     * <p>
     * This method gets the response body as an input stream.
     * </p>
     *
     * <p>
     * <b>Important: </b>This method may not be used for requests made with any of the {@link Request} download() methods.
     * </p>
     *
     * @return The input stream representing the response body. Will be null if there is no body.
     */
    InputStream getResponseByteStream();

    /**
     * This method gets the Content-Length of the response body.
     *
     * @return The content length of the response.
     */
    long getContentLength();

    /**
     * Get the HTTP headers from the response.
     *
     * @return A map with all the headers, and the corresponding values for each one.
     */
    Map<String, List<String>> getHeaders();
}
