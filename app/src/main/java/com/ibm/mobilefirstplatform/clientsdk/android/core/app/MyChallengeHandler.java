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


import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api.AuthenticationListener;

import org.json.JSONException;
import org.json.JSONObject;


public class MyChallengeHandler implements AuthenticationListener {

    @Override
    public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context) {
        try {
            // provide your custom credentials here
            // you can display a dialog here to obtain user name and password
            JSONObject answer = new JSONObject("{\"userName\":\"asaf\",\"password\":\"123\"}");

            // submit the credentials obtained from the user
            authContext.submitAuthenticationChallengeAnswer(answer);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAuthenticationSuccess(Context context, JSONObject info) {
        // Respond to successful authentication
    }

    @Override
    public void onAuthenticationFailure(Context context, JSONObject info) {
        // Respond to failed authentication
    }
}
