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

import java.util.List;
import java.util.Map;

/**
 * This class has methods to get more details from the Response to the BaseRequest.
 */
public interface  Response {

    /**
     * This method gets the HTTP status of the response.
     *
     * @return The HTTP status of the response. Will be 0 when there was no response.
     */
    int getStatus();

    /**
     * This method parses the response body as a String.
     *
     * @return The body of the response as a String. Empty string if there is no body.
     * @throws RuntimeException if the response text can not be parsed to a valid string.
     */
    String getResponseText();

    /**
     * This method gets the bytes of the response body.
     *
     * @return the bytes of the response body. Will be null if there is no body.
     */
    byte[] getResponseBytes();

    /**
     * Get the HTTP headers from the response.
     *
     * @return A map with all the headers, and the corresponding values for each one.
     */
    Map<String, List<String>> getHeaders();

}
