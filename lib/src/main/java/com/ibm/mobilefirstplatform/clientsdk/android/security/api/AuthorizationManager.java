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
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.identity.AppIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.identity.DeviceIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.identity.UserIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.AuthorizationHeaderHelper;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.AuthorizationProcessManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.preferences.AuthorizationManagerPreferences;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;


public class AuthorizationManager {

    /**
     * That state of the persistence policy to use during authorization.
     * if the value set to ALWAYS the authorization data will be saved on local storage.
     * if the value set to NEVER the authorization data will be valid only during the runtime.
     */
    public enum PersistencePolicy {ALWAYS, NEVER}

    private static AuthorizationManager instance;
    private AuthorizationManagerPreferences preferences;
    private AuthorizationProcessManager authorizationProcessManager;
    private AuthorizationManager(Context context) {
        this.preferences = new AuthorizationManagerPreferences(context);
        this.authorizationProcessManager = new AuthorizationProcessManager(context, preferences);

        //init generic data, like device data and application data
        if (preferences.deviceIdentity.get() == null) {
            preferences.deviceIdentity.set(new DeviceIdentity(context));
        }

        if (preferences.appIdentity.get() == null) {
            preferences.appIdentity.set(new AppIdentity(context));
        }
    }

    /**
     * Init singleton instance with context
     * @param context Application context
     * @return The singleton instance
     */
    public static synchronized AuthorizationManager createInstance(Context context) {
        if (instance == null) {
            instance = new AuthorizationManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * @return The singleton instance
     */
    public static AuthorizationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("getInstance can't be called before createInstance");
        }
        return instance;
    }

    /**
     * Invoke process for obtaining authorization header. during this process
     * @param context Android Activity that will handle the authorization (like facebook or google)
     * @param listener Response listener
     */
    public synchronized void obtainAuthorizationHeader(Context context, ResponseListener listener) {
        authorizationProcessManager.startAuthorizationProcess(context, listener);
    }

    /**
     * Check if the params came from response that requires authorization
     * @param statusCode of the response
     * @param responseAuthorizationHeader 'WWW-Authenticate' header
     * @return true if status is 401 or 403 and The value of the header contains 'Bearer'
     */
    public boolean isAuthorizationRequired(int statusCode, String responseAuthorizationHeader) {
        return AuthorizationHeaderHelper.isAuthorizationRequired(statusCode, responseAuthorizationHeader);
    }

    /**
     * A response is an OAuth error response only if,
     * 1. it's status is 401 or 403
     * 2. The value of the "WWW-Authenticate" header contains 'Bearer'
     *
     * @param urlConnection connection to check the authorization conditions for.
     * @return true if the response satisfies both conditions
     * @throws IOException in case connection doesn't contains response code.
     */
    public boolean isAuthorizationRequired(HttpURLConnection urlConnection) throws IOException {
        return AuthorizationHeaderHelper.isAuthorizationRequired(urlConnection);
    }

    /**
     * Clear the local stored authorization data
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

    /**
     * @return Current authorization persistence policy
     */
    public PersistencePolicy getAuthorizationPersistencePolicy() {
        return preferences.persistencePolicy.get();
    }

    /**
     * Change the sate of the current authorization persistence policy
     * @param policy new policy to use
     */
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

    /**
     * @return the stored ClientId value or null if the registration process didn't finished
     */
    public String getClientId() {
        return preferences.clientId.get();
    }

    /**
     * @return the locally stored authorization header or null if the value is not exist.
     */
    public synchronized String getCachedAuthorizationHeader() {
        String accessToken = preferences.accessToken.get();
        String idToken = preferences.idToken.get();

        if (accessToken != null && idToken != null) {
            return AuthorizationHeaderHelper.BEARER + " " + accessToken + " " + idToken;
        }
        return null;
    }

    /**
     * @return authorized user identity
     */
    public UserIdentity getUserIdentity() {
        return new UserIdentity(preferences.userIdentity.getAsMap());
    }

    /**
     * @return device identity
     */
    public DeviceIdentity getDeviceIdentity() {
        return new DeviceIdentity(preferences.deviceIdentity.getAsMap());
    }

    /**
     *
     * @return application identity
     */
    public AppIdentity getAppIdentity() {
        return new AppIdentity(preferences.appIdentity.getAsMap());
    }




}