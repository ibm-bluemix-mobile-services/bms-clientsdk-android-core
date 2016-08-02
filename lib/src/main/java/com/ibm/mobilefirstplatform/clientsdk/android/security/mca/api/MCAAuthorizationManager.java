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

package com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api;

import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AppIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.DeviceIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.UserIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.identity.BaseAppIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.identity.BaseDeviceIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.identity.BaseUserIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.AuthorizationHeaderHelper;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.AuthorizationProcessManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.AuthorizationRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.challengehandlers.ChallengeHandler;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.preferences.AuthorizationManagerPreferences;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.preferences.SharedPreferencesManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MCAAuthorizationManager implements AuthorizationManager {

    /**
     * That state of the persistence policy to use during authorization.
     * if the value set to ALWAYS the authorization data will be saved on local storage.
     * if the value set to NEVER the authorization data will be valid only during the runtime.
     */
    public enum PersistencePolicy {ALWAYS, NEVER}

    private static MCAAuthorizationManager instance;
    private AuthorizationManagerPreferences preferences;
    private AuthorizationProcessManager authorizationProcessManager;
    private HashMap<String, ChallengeHandler> challengeHandlers = new HashMap<>();
    private String tenantId = null;
    private String bluemixRegionSuffix = null;


    private MCAAuthorizationManager (Context context) {
        this.preferences = new AuthorizationManagerPreferences(context);
        this.authorizationProcessManager = new AuthorizationProcessManager(context, preferences);

        //init generic data, like device data and application data
        if (preferences.deviceIdentity.get() == null) {
            preferences.deviceIdentity.set(new BaseDeviceIdentity(context));
        }

        if (preferences.appIdentity.get() == null) {
            preferences.appIdentity.set(new BaseAppIdentity(context));
        }
    }

    /**
     * Init singleton instance with context
     * @param context Application context
     * @return The singleton instance
     */
    public static synchronized MCAAuthorizationManager createInstance(Context context) {
        if (instance == null) {
            instance = new MCAAuthorizationManager(context.getApplicationContext());

            instance.bluemixRegionSuffix = BMSClient.getInstance().getBluemixRegionSuffix();
            instance.tenantId = BMSClient.getInstance().getBluemixAppGUID();

            AuthorizationRequest.setup();
        }
        return instance;
    }

    /**
     * Init singleton instance with context and tenantId
     * @param context Application context
     * @param tenantId the unique tenant id of the MCA service instance that the application connects to.
     * @return The singleton instance
     */
    public static synchronized MCAAuthorizationManager createInstance(Context context, String tenantId) {
        instance = createInstance(context);
        instance.tenantId = tenantId;
        return instance;

    }

    /**
     * Init singleton instance with context, tenantId and Bluemix region.
     * @param context Application context
     * @param tenantId the unique tenant id of the MCA service instance that the application connects to.
     * @param bluemixRegion Specifies the Bluemix deployment to use.
     * @return The singleton instance
     */
    public static synchronized MCAAuthorizationManager createInstance(Context context, String tenantId, String bluemixRegion) {
        instance = createInstance(context);
        instance.tenantId = tenantId;
        instance.bluemixRegionSuffix = bluemixRegion;
        return instance;
    }

    /**
     * @return The singleton instance
     */
    public static MCAAuthorizationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("getInstance can't be called before createInstance");
        }
        return instance;
    }

    /**
     * @return The MCA instance tenantId
     */
    public String getTenantId(){
        return tenantId;
    }

    /**
     * @return Bluemix region suffix ,use to build URLs
     */
    public String getBluemixRegionSuffix(){
        return bluemixRegionSuffix;
    }

    /**
     * Invoke process for obtaining authorization header. during this process
     * @param context Android Activity that will handle the authorization (like facebook or google)
     * @param listener Response listener
     */
    public synchronized void obtainAuthorization(Context context, ResponseListener listener, Object... params) {
        authorizationProcessManager.startAuthorizationProcess(context, listener);
    }

    /**
     * Check if the params came from response that requires authorization
     * @param statusCode of the response
     * @param headers response headers
     * @return true if status is 401 or 403 and The value of the header contains 'Bearer'
     */
    public boolean isAuthorizationRequired(int statusCode, Map<String, List<String>> headers) {

        if (headers.containsKey(WWW_AUTHENTICATE_HEADER_NAME)){
            String authHeader = headers.get(WWW_AUTHENTICATE_HEADER_NAME).get(0);
            return AuthorizationHeaderHelper.isAuthorizationRequired(statusCode, authHeader);
        } else {
            return false;
        }
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
     * @return authorized user identity. Will return null if user is not yet authorized
     */
    public UserIdentity getUserIdentity() {
        Map map = preferences.userIdentity.getAsMap();
        return (map == null) ? null : new BaseUserIdentity(map);
    }

    /**
     * @return device identity
     */
    public DeviceIdentity getDeviceIdentity() {
        return new BaseDeviceIdentity(preferences.deviceIdentity.getAsMap());
    }

    /**
     *
     * @return application identity
     */
    public AppIdentity getAppIdentity() {
        return new BaseAppIdentity(preferences.appIdentity.getAsMap());
    }

    /**
     * Registers authentication listener for specified realm.
     *
     * @param realm    authentication realm.
     * @param listener authentication listener.
     */
    public void registerAuthenticationListener(String realm, AuthenticationListener listener) {
        if (realm == null || realm.isEmpty()) {
            throw new InvalidParameterException("The realm name can't be null or empty.");
        }

        if (listener == null) {
            throw new InvalidParameterException("The authentication listener object can't be null.");
        }

        ChallengeHandler handler = new ChallengeHandler();
        handler.initialize(realm, listener);
        challengeHandlers.put(realm, handler);
    }

    /**
     * Unregisters authentication listener
     *
     * @param realm the realm the listener was registered for
     */
    public void unregisterAuthenticationListener(String realm) {
        if (realm != null && !realm.isEmpty()) {
            challengeHandlers.remove(realm);
        }
    }

    /**
     * @exclude
     *
     * @param realm authentication realm
     * @return challenge handler for specified realm
     */
    public ChallengeHandler getChallengeHandler(String realm) {
        return challengeHandlers.get(realm);
    }

    /**
     * logs out user
     * @param context Android Activity that will handle the authorization (like facebook or google)
     * @param listener Response listener
     */

    public void logout(Context context, ResponseListener listener){
        clearAuthorizationData();
        authorizationProcessManager.logout(context, listener);
    }

}