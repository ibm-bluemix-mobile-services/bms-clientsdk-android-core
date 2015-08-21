package com.ibm.bms.clientsdk.android.security.api;

import android.content.Context;

import com.ibm.bms.clientsdk.android.core.api.ResponseListener;
import com.ibm.bms.clientsdk.android.security.internal.AuthorizationProcessManager;
import com.ibm.bms.clientsdk.android.security.internal.data.ApplicationData;
import com.ibm.bms.clientsdk.android.security.internal.data.DeviceData;
import com.squareup.okhttp.Response;

import org.apache.http.HttpRequest;
import org.json.JSONObject;

import java.net.URLConnection;


public class AuthorizationManager {

    public enum PersistencePolicy {ALWAYS, NEVER}

    private static AuthorizationManager instance;
    private AuthorizationManagerPreferences preferences;
    private AuthorizationProcessManager authorizationProcessManager;

    public static synchronized void createInstance(Context context) {
        if (instance == null) {
            instance = new AuthorizationManager(context.getApplicationContext());
        }
    }

    public static AuthorizationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("getInstance can't be called before createInstance");
        }
        return instance;
    }

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

    public synchronized void obtainAuthorizationHeader(Context context, ResponseListener listener) {
        authorizationProcessManager.startAuthorizationProcess(context, listener);
    }

    public boolean isAuthorizationRequired(int statusCode, String responseAuthorizationHeader) {
        return AuthorizationHeaderHelper.isAuthorizationRequired(statusCode, responseAuthorizationHeader);
    }

    public boolean isAuthorizationRequired(Response response) {
        return AuthorizationHeaderHelper.isAuthorizationRequired(response);
    }

    public void addCachedAuthorizationHeader(HttpRequest connection) {
        AuthorizationHeaderHelper.addAuthorizationHeader(connection, getCachedAuthorizationHeader());
    }

    /**
     * Adds the cached authorization header to the given URL connection object.
     *
     * @param urlConnection The URL connection to add the header to.
     */
    public void addCachedAuthorizationHeader(URLConnection urlConnection) {
        AuthorizationHeaderHelper.addAuthorizationHeader(urlConnection, getCachedAuthorizationHeader());
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

    public PersistencePolicy getAuthorizationPersistencePolicy() {
        return preferences.persistencePolicy.get();
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
}