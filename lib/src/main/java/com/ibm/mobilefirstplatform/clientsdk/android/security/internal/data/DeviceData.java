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

package com.ibm.mobilefirstplatform.clientsdk.android.security.internal.data;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by cirilla on 8/18/15.
 */
public class DeviceData extends JSONObject {

    final static String ID = "id";
    final static String OS = "platform";
    final static String MODEL = "model";

    public DeviceData(JSONObject json) throws JSONException {
        super(json.toString());
    }

    public DeviceData(Context context) {
        try {
            put(ID,getDeviceUUID(context));
            put(OS, Build.VERSION.RELEASE);
            put(MODEL,Build.MODEL);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getId() throws JSONException {
        return getString(ID);
    }

    public String getOS() throws JSONException {
        return getString(OS);
    }

    public String getModel() throws JSONException {
        return getString(MODEL);
    }

    //TODO: remove the unneded code fafter code review
    public static String getDeviceUUID(Context context) {

        String deviceUuid;

        /*
        String macAddr = null;
        PackageManager packageManager = context.getPackageManager();
        if (packageManager.hasSystemFeature (PackageManager.FEATURE_WIFI)) {
            WifiManager wfManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiinfo = wfManager.getConnectionInfo();
            macAddr = wifiinfo.getMacAddress();
        }
        */

        String uuid = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        //if (macAddr != null)  uuid += macAddr;
        // Use a hashed UUID not exposing the device ANDROID_ID/Mac Address
        deviceUuid = UUID.nameUUIDFromBytes(uuid.getBytes()).toString ();

        return deviceUuid;
    }


    //{"id":"24553670-294C-4E81-B103-93852D42C8B5","platform":"iOS","model":"iPhone Simulator","osVersion":"8.4"}}



//    csrJSON.put("deviceId", preferences.deviceId.get());
//    csrJSON.put("deviceOs", ""  + android.os.Build.VERSION.RELEASE);
//    csrJSON.put("deviceModel", Build.MODEL);
//    csrJSON.put("applicationId", "nothing");
//    csrJSON.put("applicationVersion", "1.0");
//    csrJSON.put("environment", "ANDROID");

}
