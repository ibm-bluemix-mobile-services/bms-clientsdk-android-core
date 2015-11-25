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

package com.ibm.mobilefirstplatform.clientsdk.android.security.internal;


import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Created by vitalym on 7/21/15.
 */

/**
 * Internal helper class with various utilities.
 */
public class Utils {
    private static Logger logger = Logger.getInstance(Logger.INTERNAL_PREFIX + "Utils");
    private final static String SECURE_PATTERN_START = "/*-secure-\n";
    private final static String SECURE_PATTERN_END = "*/";

    private final static String BLUEMIX_NAME = "bluemix";
    private final static String BLUEMIX_DOMAIN = "bluemix.net";
    private final static String STAGE1_NAME = "stage1";

    /**
     * Obtains a parameter with specified name from from query string. The query should be in format
     * param=value&param=value ...
     *
     * @param query     Queery in "url" format.
     * @param paramName Parameter name.
     * @return Parameter value, or null.
     */
    public static String getParameterValueFromQuery(String query, String paramName) {
        String[] components = query.split("&");

        for (String keyValuePair : components) {
            String[] pairComponents = keyValuePair.split("=");

            if (pairComponents.length == 2) {
                try {
                    String key = URLDecoder.decode(pairComponents[0], "utf-8");
                    if (key.compareTo(paramName) == 0) {
                        return URLDecoder.decode(pairComponents[1], "utf-8");
                    }
                } catch (UnsupportedEncodingException e) {
                    logger.error("getParameterValueFromQuery failed with exception: " + e.getLocalizedMessage(), e);
                }

            }
        }

        return null;
    }


    /**
     * Extracts a JSON object from server response with secured string.
     *
     * @param response Server response
     * @return Extracted secured JSON or null.
     */
    public static JSONObject extractSecureJson(Response response) {
        try {
            String responseText = response.getResponseText();

            if (!responseText.startsWith(SECURE_PATTERN_START) || !responseText.endsWith(SECURE_PATTERN_END)) {
                return null;
            }

            int startIndex = responseText.indexOf(SECURE_PATTERN_START);
            int endIndex = responseText.indexOf(SECURE_PATTERN_END, responseText.length() - SECURE_PATTERN_END.length() - 1);

            String jsonString = responseText.substring(startIndex + SECURE_PATTERN_START.length(), endIndex);

            return new JSONObject(jsonString);
        } catch (Throwable t) {
            logger.error("extractSecureJson failed with exception: " + t.getLocalizedMessage(), t);
            return null;
        }
    }

    /**
     * Concatenates two URLs. The function checks for trailing and preceding slashes in rootUrl and path.
     * @param rootUrl first part of the url
     * @param path second part of the url
     * @return Concatenated string containing rootUrl and path.
     */
    public static String concatenateUrls(String rootUrl, String path) {
        if (rootUrl == null || rootUrl.isEmpty()) {
            return path;
        }

        if (path == null || path.isEmpty()) {
            return rootUrl;
        }

        String finalUrl;

        if (rootUrl.charAt(rootUrl.length() - 1) == '/' && path.charAt(0) == '/') {
            finalUrl = rootUrl.substring(0, rootUrl.length() - 2) + path;
        } else if (rootUrl.charAt(rootUrl.length() - 1) != '/' && path.charAt(0) != '/') {
            finalUrl = rootUrl + "/" + path;
        } else {
            finalUrl = rootUrl + path;
        }

        return finalUrl;
    }

}
