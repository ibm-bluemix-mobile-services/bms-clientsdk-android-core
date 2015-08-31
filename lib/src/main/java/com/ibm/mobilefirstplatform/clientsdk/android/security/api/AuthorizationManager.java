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

package com.ibm.mobilefirstplatform.clientsdk.android.security.api;

import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.AuthorizationHeaderHelper;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.AuthorizationProcessManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.data.ApplicationData;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.data.DeviceData;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.preferences.AuthorizationManagerPreferences;

import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;


public class AuthorizationManager {

    private static AuthorizationManager instance;
    private AuthorizationManagerPreferences preferences;
    private AuthorizationProcessManager authorizationProcessManager;

    private AuthorizationManager(Context context) {
        this.preferences = new AuthorizationManagerPreferences(context);
        this.authorizationProcessManager = new AuthorizationProcessManager(context, preferences);

        //init generic data, like device data and application data
        if (preferences.deviceIdentity.get() == null) {
            preferences.deviceIdentity.set(new DeviceData(context));
        }

        if (preferences.appIdentity.get() == null) {
            preferences.appIdentity.set(new ApplicationData(context));
        }
    }

    public static synchronized AuthorizationManager createInstance(Context context) {
        if (instance == null) {
            instance = new AuthorizationManager(context.getApplicationContext());
        }
        return instance;
    }

    public static AuthorizationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("getInstance can't be called before createInstance");
        }
        return instance;
    }

    public synchronized void obtainAuthorizationHeader(Context context, ResponseListener listener) {
        authorizationProcessManager.startAuthorizationProcess(context, listener);
    }

    public boolean isAuthorizationRequired(int statusCode, String responseAuthorizationHeader) {
        return AuthorizationHeaderHelper.isAuthorizationRequired(statusCode, responseAuthorizationHeader);
    }

    public boolean isAuthorizationRequired(HttpURLConnection urlConnection) throws IOException {
        return AuthorizationHeaderHelper.isAuthorizationRequired(urlConnection);
    }

    /**
     * Clear the stored authorization data
     */
    public void clearAuthorizationData() {
        preferences.accessToken.clear();
        preferences.idToken.clear();
        preferences.userIdentity.clear();
    }

    /**
     * Adds the cached authorization header to the given URL connection object.
     * int the cached authorization header is equals to null then this operation has no effect.
     *
     * @param urlConnection The URL connection to add the header to.
     */
    public void addCachedAuthorizationHeader(URLConnection urlConnection) {
        AuthorizationHeaderHelper.addAuthorizationHeader(urlConnection, getCachedAuthorizationHeader());
    }

    public PersistencePolicy getAuthorizationPersistencePolicy() {
        return preferences.persistencePolicy.get();
    }

    public void setAuthorizationPersistencePolicy(PersistencePolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("The policy argument cannot be null");
        }

        if (preferences.persistencePolicy.get() != policy) {
            preferences.persistencePolicy.set(policy);

            preferences.accessToken.updateStateByPolicy();
            preferences.idToken.updateStateByPolicy();
        }
    }

    public String getClientId() {
        return preferences.clientId.get();
    }

    public synchronized String getCachedAuthorizationHeader() {
        String accessToken = preferences.accessToken.get();
        String idToken = preferences.idToken.get();

        if (accessToken != null && idToken != null) {
            return AuthorizationHeaderHelper.BEARER + " " + accessToken + " " + idToken;
        }
        return null;
    }

    public JSONObject getUserIdentity() {
        return preferences.userIdentity.getAsJSON();
    }

    public JSONObject getDeviceIdentity() {
        return preferences.deviceIdentity.getAsJSON();
    }

    public JSONObject getAppIdentity() {
        return preferences.appIdentity.getAsJSON();
    }

    public enum PersistencePolicy {ALWAYS, NEVER}
}