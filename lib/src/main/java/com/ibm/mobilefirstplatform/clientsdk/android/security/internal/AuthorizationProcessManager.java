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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.FailResponse;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.MFPRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.CertificateStore;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.CertificatesUtility;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.DefaultJSONSigner;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.KeyPairUtility;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.data.ApplicationData;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.data.DeviceData;
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
    private final String defaultScope = "defaultScope";
    private AuthorizationManagerPreferences preferences;
    private ConcurrentLinkedQueue<ResponseListener> authorizationQueue;
    private KeyPair registrationKeyPair;
    private DefaultJSONSigner jsonSigner;

    private CertificateStore certificateStore;
    private Logger logger;
    private String sessionId;

    public AuthorizationProcessManager(Context context, AuthorizationManagerPreferences preferences) {
        this.logger = Logger.getInstance(AuthorizationProcessManager.class.getSimpleName());

        this.preferences = preferences;
        this.authorizationQueue = new ConcurrentLinkedQueue<>();
        this.jsonSigner = new DefaultJSONSigner();

        File keyStoreFile = new File(context.getFilesDir().getAbsolutePath(), "mfp.keystore");
        String uuid = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
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
     * Generate the params that will be used during the registration phase
     * @return Map with all the parameters
     */
    private HashMap<String, String> createRegistrationParams() {
        registrationKeyPair = KeyPairUtility.generateRandomKeyPair();

        JSONObject csrJSON = new JSONObject();
        HashMap<String, String> params;

        try {
            DeviceData deviceData = new DeviceData(preferences.deviceIdentity.getAsJSON());
            ApplicationData applicationData = new ApplicationData(preferences.appIdentity.getAsJSON());

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
        //headers.put("X-WL-Auth", "0"); //TODO: remove this one later

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
        //params.put("PARAM_SCOPE_KEY", defaultScope);  //TODO: remove it later after automation testing

        return params;
    }


    /**
     * Invoke request for registration, the result of the request should contain ClientId.
     * @param context android context
     */
    private void invokeInstanceRegistrationRequest(final Context context) {

        AuthorizationRequestAgent.RequestOptions options = new AuthorizationRequestAgent.RequestOptions();

        options.parameters = createRegistrationParams();
        options.headers = createRegistrationHeaders();
        options.requestMethod = MFPRequest.POST;

        InnerAuthorizationResponseListener listener = new InnerAuthorizationResponseListener() {
            @Override
            public void handleAuthorizationSuccessResponse(Response response) throws Exception {
                saveCertificateFromResponse(response);
                invokeAuthorizationRequest(context);
            }
        };

        authorizationRequestSend(null, "clients/instance", options, listener);
    }


    private void saveCertificateFromResponse(com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response response) {
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

        logger.info("certificate successfully saved");
    }

    private void invokeAuthorizationRequest(Context context) {

        AuthorizationRequestAgent.RequestOptions options = new AuthorizationRequestAgent.RequestOptions();

        options.parameters = createAuthorizationParams();
        options.headers = new HashMap<>(1);
        addSessionIdHeader(options.headers);
        options.requestMethod = MFPRequest.GET;

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


    private String extractLocationHeader(Response response) {
        List<String> location = response.getResponseHeaders().get("Location");

        if (location == null) {
            throw new RuntimeException("Failed to find 'Location' header");
        }

        logger.debug("Location header extracted successfully");
        return location.get(0);
    }

    private String extractGrantCode(String urlString) throws MalformedURLException {

        //http://localhost?code=3IIXxqIKad4Zjq5VyhdlbnG0__KW5KaIIgpfub3I64qpxLPn4YMdPFysxBUp-swd3SFc8aVKsPzLKGYMpzZctv3PDonYZgf-UMalerjRLlsaCd21A2xfHMvfwJy_kY31wXWngzYQauyDp6-xI58nPu3sDsl2J_6Ce6nxyJHXUwrRk47_XmY4w3GqJVGJ3rKs&wl_result=%7B%7D
        URL url = new URL(urlString);
        String code = Utils.getParameterValueFromQuery(url.getQuery(), "code");

        if (code == null){
            throw new RuntimeException("Failed to extract grant code from url");
        }

        logger.debug("Grant code extracted successfully");
        return code;
    }

    /*
    private HashMap<String, String> getQueryParams(String query) {
        String[] params = query.split("&");
        HashMap<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }
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

    private void invokeTokenRequest(String grantCode) {

        AuthorizationRequestAgent.RequestOptions options = new AuthorizationRequestAgent.RequestOptions();

        options.parameters = createTokenRequestParams(grantCode);
        options.headers = createTokenRequestHeaders(grantCode);
        addSessionIdHeader(options.headers);
        options.requestMethod = MFPRequest.POST;

        InnerAuthorizationResponseListener listener = new InnerAuthorizationResponseListener() {
            @Override
            public void handleAuthorizationSuccessResponse(Response response) throws Exception {
                saveTokenFromResponse(response);
                handleAuthorizationSuccess(response);
            }
        };

        authorizationRequestSend(null, "token", options, listener);
    }

    private void saveTokenFromResponse(com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response response) {
        try {
            JSONObject responseJSON = response.getResponseJSON();

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

            preferences.userIdentity.set(idTokenJSON);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save token from response", e);
        }
    }

    private void handleAuthorizationFailure(Throwable t) {
        handleAuthorizationFailure(null, t);
    }

    //General failure for authorization
    private void handleAuthorizationFailure(FailResponse request, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }

        Iterator<ResponseListener> iterator = authorizationQueue.iterator();

        while(iterator.hasNext()) {
            ResponseListener next = iterator.next();
            next.onFailure(request,t);
            iterator.remove();
        }
    }

    private void handleAuthorizationSuccess(Response response) {

        Iterator<ResponseListener> iterator = authorizationQueue.iterator();

        while(iterator.hasNext()) {
            ResponseListener next = iterator.next();
            next.onSuccess(response);
            iterator.remove();
        }
    }

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
        public void onFailure(FailResponse response, Throwable t) {
            handleAuthorizationFailure(response, t);
        }
    }
}
