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

package com.ibm.mobilefirstplatform.clientsdk.android.security.identity;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.ibm.mobilefirstplatform.clientsdk.android.security.api.DeviceIdentity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

/**
 * Holds the device identity json
 */
public class BaseDeviceIdentity extends JSONObject implements DeviceIdentity {

    /**
     * Init the data using map
     * @param asMap hold the device data
     */
    public BaseDeviceIdentity (Map asMap) {
        super(asMap);
    }

    /**
     * Init the data using context
     * @param context android application context
     */
    public BaseDeviceIdentity (Context context) {
        try {
            put(ID, getDeviceUUID(context));
            put(OS, Build.VERSION.RELEASE);
            put(MODEL, Build.MODEL);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return device unique id
     */
    public String getId() {
        return optString(ID);
    }

    /**
     * @return device OS
     */
    public String getOS() {
        return optString(OS);
    }

    /**
     * @return device model
     */
    public String getModel() {
        return optString(MODEL);
    }

    /**
     * @param context android application context
     * @return device unique id
     */
    private String getDeviceUUID(Context context) {
        String uuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return UUID.nameUUIDFromBytes(uuid.getBytes()).toString();
    }
}
