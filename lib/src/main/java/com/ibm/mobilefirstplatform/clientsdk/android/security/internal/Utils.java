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


import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONObject;

/**
 * Created by vitalym on 7/21/15.
 */

/**
 * Internal helper class with various utilities.
 */
public class Utils {
    private static Logger logger = Logger.getInstance("Utils");
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



    /*
    //TODO: check if we can diffrent implementation
    public static String getDeviceUUID(Context context) {
        String deviceUuid;

        String macAddr = null;
        PackageManager packageManager = context.getPackageManager();
        if (packageManager.hasSystemFeature (PackageManager.FEATURE_WIFI)) {
            WifiManager wfManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiinfo = wfManager.getConnectionInfo();
            macAddr = wifiinfo.getMacAddress();
        }
        String uuid = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        if (macAddr != null)  uuid += macAddr;
        // Use a hashed UUID not exposing the device ANDROID_ID/Mac Address
        deviceUuid = UUID.nameUUIDFromBytes(uuid.getBytes()).toString () ;

        return deviceUuid;


    }
    */

    /**
     * Extracts a JSON object from server response with secured string.
     *
     * @param response Server response
     * @return Extracted secured JSON or null.
     */
    public static JSONObject extractSecureJson(Response response) {
        String responseText = response.getResponseText();

        if (responseText == null || !responseText.startsWith(SECURE_PATTERN_START) || !responseText.endsWith(SECURE_PATTERN_END)) {
            return null;
        }

        int startIndex = responseText.indexOf(SECURE_PATTERN_START);
        int endIndex = responseText.indexOf(SECURE_PATTERN_END, responseText.length() - SECURE_PATTERN_END.length() - 1);

        String jsonString = responseText.substring(startIndex + SECURE_PATTERN_START.length(), endIndex);
        try {
            return new JSONObject(jsonString);
        } catch (Throwable t) {
            logger.error("extractSecureJson failed with exception: " + t.getLocalizedMessage(), t);
            return null;
        }
    }

    /**
     * Builds rewrite domain from backend route url.
     *
     * @param backendRoute Backend route.
     * @param subzone      Subzone
     * @return Rewrite domain.
     * @throws MalformedURLException if backendRoute parameter has invalid format.
     */
    public static String buildRewriteDomain(String backendRoute, String subzone) throws MalformedURLException {
        if (backendRoute == null || backendRoute.isEmpty()) {
            logger.error("Backend route can't be null.");
            return null;
        }

        String applicationRoute = backendRoute;

        if (!applicationRoute.startsWith(BMSClient.HTTP_SCHEME)) {
            applicationRoute = String.format("%s://%s", BMSClient.HTTPS_SCHEME, applicationRoute);
        } else if (!applicationRoute.startsWith(BMSClient.HTTPS_SCHEME) && applicationRoute.contains(BLUEMIX_NAME)) {
            applicationRoute = applicationRoute.replace(BMSClient.HTTP_SCHEME, BMSClient.HTTPS_SCHEME);
        }

        URL url = new URL(applicationRoute);

        String host = url.getHost();
        String rewriteDomain;
        String regionInDomain = "ng";
        int port = url.getPort();

        String serviceUrl = String.format("%s://%s", url.getProtocol(), host);

        if (port != 0) {
            serviceUrl += ":" + String.valueOf(port);
        }

        String[] hostElements = host.split(".");

        if (!serviceUrl.contains(STAGE1_NAME)) {
            // Multi-region: myApp.eu-gb.mybluemix.net
            // US: myApp.mybluemix.net
            if (hostElements.length == 4) {
                regionInDomain = hostElements[hostElements.length - 3];
            }

            // this is production, because STAGE1 is not found
            // Multi-Region Eg: eu-gb.bluemix.net
            // US Eg: ng.bluemix.net
            rewriteDomain = String.format("%s.%s", regionInDomain, BLUEMIX_DOMAIN);
        } else {
            // Multi-region: myApp.stage1.eu-gb.mybluemix.net
            // US: myApp.stage1.mybluemix.net
            if (hostElements.length == 5) {
                regionInDomain = hostElements[hostElements.length - 3];
            }

            if (subzone != null && !subzone.isEmpty()) {
                // Multi-region Dev subzone Eg: stage1-Dev.eu-gb.bluemix.net
                // US Dev subzone Eg: stage1-Dev.ng.bluemix.net
                rewriteDomain = String.format("%s-%s.%s.%s", STAGE1_NAME, subzone, regionInDomain, BLUEMIX_DOMAIN);
            } else {
                // Multi-region Eg: stage1.eu-gb.bluemix.net
                // US  Eg: stage1.ng.bluemix.net
                rewriteDomain = String.format("%s.%s.%s", STAGE1_NAME, regionInDomain, BLUEMIX_DOMAIN);
            }
        }

        return rewriteDomain;
    }

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
