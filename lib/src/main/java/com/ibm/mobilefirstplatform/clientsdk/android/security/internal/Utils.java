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

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import org.json.JSONObject;
/**
 * Created by vitalym on 7/21/15.
 */
public class Utils {
    private final static String SECURE_PATTERN_START = "/*-secure-\n";
    private final static String SECURE_PATTERN_END = "*/";

    public static String getParameterValueFromQuery(String query, String paramName) {
        String[] components = query.split("&");

        for(String keyValuePair : components) {
            String[] pairComponents = keyValuePair.split("=");

            if (pairComponents.length == 2) {
                try {
                    String key = URLDecoder.decode(pairComponents[0], "utf-8");
                    if (key.compareTo(paramName) == 0) {
                        return URLDecoder.decode(pairComponents[1], "utf-8");
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
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
            return null;
        }
    }
    
}
