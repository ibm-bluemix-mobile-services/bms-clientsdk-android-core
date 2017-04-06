/*
 *     Copyright 2017 IBM Corp.
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

package com.ibm.mobilefirstplatform.clientsdk.android.core.app;


import android.util.Log;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;

import org.json.JSONObject;

public class MyResponseListener implements ResponseListener {

    @Override
    public void onSuccess(Response response) {
        // here we handle authentication success
        Log.i("BMSCore", "Request succeeded!");
        if (response != null && response.getResponseText() != null) {
            Log.i("BMSCore", "Response: " + response.getResponseText());
        }
    }

    @Override
    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
        // handle auth failure
        Log.i("BMSCore", "Request failed!");
        if (response != null && response.getResponseText() != null) {
            Log.i("BMSCore", "Response: " + response.getResponseText());
        }
        if (t != null && t.getMessage() != null) {
            Log.i("BMSCore", "Error: " + t.getMessage());
        }
        if (extendedInfo != null && extendedInfo.toString() != null) {
            Log.i("BMSCore", "Extended info: " + extendedInfo.toString());
        }
    }
}
