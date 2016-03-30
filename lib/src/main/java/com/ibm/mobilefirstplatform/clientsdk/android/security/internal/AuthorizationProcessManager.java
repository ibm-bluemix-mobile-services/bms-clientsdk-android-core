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

package com.ibm.mobilefirstplatform.clientsdk.android.security.internal;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.internal.BaseRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.internal.ResponseImpl;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.identity.AppIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.identity.DeviceIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.CertificateStore;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.CertificatesUtility;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.DefaultJSONSigner;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.KeyPairUtility;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.preferences.AuthorizationManagerPreferences;

import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles the complete authorization process cycle
 * Created by cirilla on 8/3/15.
 */
public class AuthorizationProcessManager {

    private static final String HTTP_LOCALHOST = "http://localhost";
    private AuthorizationManagerPreferences preferences;
    private ConcurrentLinkedQueue<ResponseListener> authorizationQueue;
    private KeyPair registrationKeyPair;
    private DefaultJSONSigner jsonSigner;

    private CertificateStore certificateStore;
    private Logger logger;
    private String sessionId;

    public AuthorizationProcessManager(Context context, AuthorizationManagerPreferences preferences) {
        this.logger = Logger.getInstance(Logger.INTERNAL_PREFIX + AuthorizationProcessManager.class.getSimpleName());

        this.preferences = preferences;
        this.authorizationQueue = new ConcurrentLinkedQueue<>();
        this.jsonSigner = new DefaultJSONSigner();

        File keyStoreFile = new File(context.getFilesDir().getAbsolutePath(), "mfp.keystore");
        String uuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        certificateStore = new CertificateStore(keyStoreFile, uuid);

        //case where the shared preferences were deleted but the certificate is saved in the keystore
        if (preferences.clientId.get() == null && certificateStore.isCertificateStored()) {
            try {
                X509Certificate certificate = certificateStore.getCertificate();
                preferences.clientId.set(CertificatesUtility.getClientIdFromCertificate(certificate));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //generate new random session id
        sessionId = UUID.randomUUID().toString();
    }

    /**
     * Main method to start authorization process
     * @param context android context
     * @param listener response listener that will get the result of the process
     */
    public void startAuthorizationProcess(final Context context, ResponseListener listener) {
        authorizationQueue.add(listener);

        //start the authorization process only if this is the first time we ask for authorization
        if (authorizationQueue.size() == 1) {
            try {
                if (preferences.clientId.get() == null) {
                    logger.info("starting registration process");
                    invokeInstanceRegistrationRequest(context);
                } else {
                    logger.info("starting authorization process");
                    invokeAuthorizationRequest(context);
                }
            } catch (Throwable t) {
                handleAuthorizationFailure(t);
            }
        }

        else{
            logger.info("authorization process already running, adding response listener to the queue");
            logger.debug(String.format("authorization process currently handling %d requests", authorizationQueue.size()));
        }
    }

    /**
     * logs out user
     * @param context Android Activity that will handle the authorization (like facebook or google)
     * @param listener Response listener
     */
    public void logout(Context context, ResponseListener listener) {
        AuthorizationRequestAgent.RequestOptions options = new AuthorizationRequestAgent.RequestOptions();
        options.parameters = new HashMap<String,String>(1);
        options.parameters.put("client_id", preferences.clientId.get());
        options.headers = new HashMap<>(1);
        addSessionIdHeader(options.headers);
        options.requestMethod = Request.GET;
        try {
            authorizationRequestSend(context,"logout", options, listener);
        } catch (Exception e) {
            logger.debug("Could not log out");
        }
    }
    /**
     * Invoke request for registration, the result of the request should contain ClientId.
     * @param context android context
     */
    private void invokeInstanceRegistrationRequest(final Context context) {

        AuthorizationRequestAgent.RequestOptions options = new AuthorizationRequestAgent.RequestOptions();

        options.parameters = createRegistrationParams();
        options.headers = createRegistrationHeaders();
        options.requestMethod = Request.POST;

        InnerAuthorizationResponseListener listener = new InnerAuthorizationResponseListener() {
            @Override
            public void handleAuthorizationSuccessResponse(Response response) throws Exception {
                saveCertificateFromResponse(response);
                invokeAuthorizationRequest(context);
            }
        };

        authorizationRequestSend(null, "clients/instance", options, listener);
    }

    /**
     * Generate the params that will be used during the registration phase
     * @return Map with all the parameters
     */
    private HashMap<String, String> createRegistrationParams() {
        registrationKeyPair = KeyPairUtility.generateRandomKeyPair();

        JSONObject csrJSON = new JSONObject();
        HashMap<String, String> params;

        try {
            DeviceIdentity deviceData = new DeviceIdentity(preferences.deviceIdentity.getAsMap());
            AppIdentity applicationData = new AppIdentity(preferences.appIdentity.getAsMap());

            csrJSON.put("deviceId", deviceData.getId());
            csrJSON.put("deviceOs", "" + deviceData.getOS());
            csrJSON.put("deviceModel", deviceData.getModel());
            csrJSON.put("applicationId", applicationData.getId());
            csrJSON.put("applicationVersion", applicationData.getVersion());
            csrJSON.put("environment", "android");

            String csrValue = jsonSigner.sign(registrationKeyPair, csrJSON);

            params = new HashMap<>(1);
            params.put("CSR", csrValue);

            return params;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create registration params", e);
        }
    }

    /**
     * Generate the headers that will be used during the registration phase
     * @return Map with all the headers
     */
    private HashMap<String, String> createRegistrationHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        addSessionIdHeader(headers);

        return headers;
    }

    /**
     * Generate the params that will be used during the token request phase
     * @param grantCode from the authorization phase
     * @return Map with all the headers
     */
    private HashMap<String, String> createTokenRequestParams(String grantCode) {

        HashMap<String, String> params = new HashMap<>();

        params.put("code", grantCode);
        params.put("client_id", preferences.clientId.get());
        params.put("grant_type", "authorization_code");
        params.put("redirect_uri", HTTP_LOCALHOST);

        return params;
    }

    /**
     * Extract the certificate data from response and save it on local storage
     * @param response contains the certificate data
     */
    private void saveCertificateFromResponse(Response response) {
        try {
            String responseBody = response.getResponseText();
            JSONObject jsonResponse = new JSONObject(responseBody);

            //handle certificate
            String certificateString = jsonResponse.getString("certificate");
            X509Certificate certificate = CertificatesUtility.base64StringToCertificate(certificateString);

            CertificatesUtility.checkValidityWithPublicKey(certificate, registrationKeyPair.getPublic());

            certificateStore.saveCertificate(registrationKeyPair, certificate);

            //save the clientId separately
            preferences.clientId.set(jsonResponse.getString("clientId"));

        } catch (Exception e) {
            throw new RuntimeException("Failed to save certificate from response", e);
        }

        logger.debug("certificate successfully saved");
    }


    /**
     * Invoke the authorization request, the result of the request should be a grant code
     * @param context android activity that will handle authentication (facebook, google)
     */
    private void invokeAuthorizationRequest(Context context) {

        AuthorizationRequestAgent.RequestOptions options = new AuthorizationRequestAgent.RequestOptions();

        options.parameters = createAuthorizationParams();
        options.headers = new HashMap<>(1);
        addSessionIdHeader(options.headers);
        options.requestMethod = Request.GET;

        InnerAuthorizationResponseListener listener = new InnerAuthorizationResponseListener() {
            @Override
            public void handleAuthorizationSuccessResponse(Response response) throws Exception {

                String location = extractLocationHeader(response);
                String grantCode = extractGrantCode(location);
                invokeTokenRequest(grantCode);
            }
        };

        authorizationRequestSend(context, "authorization", options, listener);
    }


    /**
     * Adding current session id value to the headers map
     * @param headers map of headers to add the new header
     */
    private void addSessionIdHeader(HashMap<String, String> headers) {
        headers.put("X-WL-Session", sessionId);
    }


    /**
     * Generate the params that will be used during the authorization phase
     * @return Map with all the params
     */
    private HashMap<String, String> createAuthorizationParams() {

        HashMap<String, String> params = new HashMap<>(3);
        params.put("response_type", "code");
        params.put("client_id", preferences.clientId.get());
        params.put("redirect_uri", HTTP_LOCALHOST);

        return params;
    }

    /**
     * @param response response with location header
     * @return the extracted location header
     */
    private String extractLocationHeader(Response response) {
        List<String> location = response.getHeaders().get("Location");

        if (location == null) {
            throw new RuntimeException("Failed to find 'Location' header");
        }

        logger.debug("Location header extracted successfully");
        return location.get(0);
    }

    /**
     * Extract grant code from url string
     * @param urlString url that contain the grant code
     * @return grant code
     * @throws MalformedURLException in case of illegal url format
     */
    private String extractGrantCode(String urlString) throws MalformedURLException {

        URL url = new URL(urlString);
        String code = Utils.getParameterValueFromQuery(url.getQuery(), "code");

        if (code == null){
            throw new RuntimeException("Failed to extract grant code from url");
        }

        logger.debug("Grant code extracted successfully");
        return code;
    }

    /**
     * Invoke request to get token, the result of the response should be a valid token
     * @param grantCode grant code that will be used during the request
     */
    private void invokeTokenRequest(String grantCode) {

        AuthorizationRequestAgent.RequestOptions options = new AuthorizationRequestAgent.RequestOptions();

        options.parameters = createTokenRequestParams(grantCode);
        options.headers = createTokenRequestHeaders(grantCode);
        addSessionIdHeader(options.headers);
        options.requestMethod = Request.POST;

        InnerAuthorizationResponseListener listener = new InnerAuthorizationResponseListener() {
            @Override
            public void handleAuthorizationSuccessResponse(Response response) throws Exception {
                saveTokenFromResponse(response);
                handleAuthorizationSuccess(response);
            }
        };

        authorizationRequestSend(null, "token", options, listener);
    }


    /**
     * Generate the headers that will be used during the token request phase
     * @param grantCode from the authorization phase
     * @return Map with all the headers
     */
    private HashMap<String, String> createTokenRequestHeaders(String grantCode) {
        JSONObject payload = new JSONObject();
        HashMap<String, String> headers;
        try {
            payload.put("code", grantCode);

            KeyPair keyPair = certificateStore.getStoredKeyPair();
            String jws = jsonSigner.sign(keyPair, payload);

            headers = new HashMap<>(1);
            headers.put("X-WL-Authenticate", jws);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create token request headers", e);
        }

        return headers;
    }

    /**
     * Extract token from response and save it locally
     * @param response response that contain the token
     */
    private void saveTokenFromResponse(Response response) {
        try {
            JSONObject responseJSON = ((ResponseImpl)response).getResponseJSON();

            String accessToken = responseJSON.getString("access_token");
            String idToken = responseJSON.getString("id_token");

            //save the tokens
            preferences.accessToken.set(accessToken);
            preferences.idToken.set(idToken);

            //save the user identity separately
            String[] idTokenData = idToken.split("\\.");
            byte[] decodedIdTokenData = Base64.decode(idTokenData[1], Base64.DEFAULT);
            String decodedIdTokenString = new String(decodedIdTokenData);
            JSONObject idTokenJSON = new JSONObject(decodedIdTokenString);

            if (idTokenJSON.has("imf.user")) {
                preferences.userIdentity.set(idTokenJSON.getJSONObject("imf.user"));
            }

            logger.debug("token successfully saved");
        } catch (Exception e) {
            throw new RuntimeException("Failed to save token from response", e);
        }
    }

    /**
     * Use authorization request agent for sending the request
     * @param context android activity that will handle authentication (facebook, google)
     * @param path path to the server
     * @param options send options
     * @param listener response listener
     */
    private void authorizationRequestSend(final Context context, String path, AuthorizationRequestAgent.RequestOptions options, ResponseListener listener) {
        try {
            AuthorizationRequestAgent authorizationRequestManager = new AuthorizationRequestAgent();
            authorizationRequestManager.initialize(context, listener);
            authorizationRequestManager.sendRequest(path, options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send authorization request", e);
        }
    }

    /**
     * Handle failure in the authorization process. All the response listeners will be updated with
     * failure
     * @param t the reason of failure
     */
    private void handleAuthorizationFailure(Throwable t) {
        handleAuthorizationFailure(null, t, null);
    }

    /**
     * Handle failure in the authorization process. All the response listeners will be updated with
     * failure
     * @param response response that caused to failure
     * @param t additional info about the failure
     */
    private void handleAuthorizationFailure(Response response, Throwable t, JSONObject extendedInfo) {
        logger.error("authorization process failed");

        if (t != null) {
            t.printStackTrace();
        }

        Iterator<ResponseListener> iterator = authorizationQueue.iterator();

        while(iterator.hasNext()) {
            ResponseListener next = iterator.next();
            next.onFailure(response, t, extendedInfo);
            iterator.remove();
        }
    }

    /**
     * Handle success in the authorization process. All the response listeners will be updated with
     * success
     * @param response final success response from the server
     */
    private void handleAuthorizationSuccess(Response response) {

        Iterator<ResponseListener> iterator = authorizationQueue.iterator();

        while(iterator.hasNext()) {
            ResponseListener next = iterator.next();
            next.onSuccess(response);
            iterator.remove();
        }
    }

    /**
     * Inner response listener that is used during the authorization requests.
     * this listener handles all types of exception and calls to handleAuthorizationFailure in that
     * case
     */
    private abstract class InnerAuthorizationResponseListener implements ResponseListener {

        abstract public void handleAuthorizationSuccessResponse(Response response) throws Exception;

        @Override
        public void onSuccess(Response response) {
            try {
                handleAuthorizationSuccessResponse(response);
            } catch (Exception e) {
                handleAuthorizationFailure(e);
            }
        }

        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            handleAuthorizationFailure(response, t, extendedInfo);
        }
    }
}
