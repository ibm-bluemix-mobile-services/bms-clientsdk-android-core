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

package com.ibm.mobilefirstplatform.clientsdk.android.security.api.identity;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Holds the application identity json
 */
public class AppIdentity extends JSONObject {

    final static String ID = "id";
    final static String VERSION = "version";

    public AppIdentity(Map asMap) {
        super(asMap);
    }

    public AppIdentity(Context context) {
        try {
            put(ID,context.getPackageName());
            put(VERSION,"1.0");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getId() {
        return optString(ID);
    }

    public String getVersion() {
        return optString(VERSION);
    }
}
