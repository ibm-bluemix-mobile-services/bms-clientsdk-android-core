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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by cirilla on 8/18/15.
 */
public class ApplicationData extends JSONObject {

    final static String ID = "id";
    final static String VERSION = "version";

    public ApplicationData(JSONObject json) throws JSONException {
        super(json.toString());
    }

    public ApplicationData(Context context) {
        try {
            put(ID,context.getPackageName());
            put(VERSION,"1.0");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getId() throws JSONException {
        return getString(ID);
    }

    public String getVersion() throws JSONException {
        return getString(VERSION);
    }
}
