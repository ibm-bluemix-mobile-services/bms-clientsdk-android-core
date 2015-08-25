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
import android.util.Base64;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.FailResponse;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.MFPRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManagerPreferences;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.CertificateAuxiliary;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.CertificateStore;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.DefaultJSONSigner;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.KeyPairAuxiliary;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.data.ApplicationData;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.data.DeviceData;

import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * AuthorizationProcessManager
 * Created by cirilla on 8/3/15.
 */
public class AuthorizationProcessManager {


    private static final String HTTP_LOCALHOST = "http://localhost";

    private AuthorizationManagerPreferences preferences;
    private boolean isAuthorizationInProgress;
    private AuthorizationQueue authorizationQueue;
    private final String defaultScope = "defaultScore";
    private KeyPair registrationKeyPair;
    private DefaultJSONSigner jsonSigner;

    private CertificateStore certificateStore;

    private String sessionId;

    public AuthorizationProcessManager(Context context, AuthorizationManagerPreferences preferences) {

        this.preferences = preferences;
        this.authorizationQueue = new AuthorizationQueue();
        this.jsonSigner = new DefaultJSONSigner();

        File keyStoreFile = new File(context.getFilesDir().getAbsolutePath(), "mfp.keystore");
        certificateStore = new CertificateStore(keyStoreFile, context.getPackageName().toCharArray());

        //case where the shared preferences were deleted but the certificate is saved in the keystore
        if (preferences.clientId.get() == null && certificateStore.isContainsCertificate()) {
            try {
                X509Certificate certificate = certificateStore.getCertificate();
                preferences.clientId.set(CertificateAuxiliary.getClientIdFromCertificate(certificate));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //generate new random session id
        sessionId = UUID.randomUUID().toString();
    }

    public void startAuthorizationProcess(final Context context, ResponseListener listener) {
        authorizationQueue.addListener(defaultScope, listener);

        if (!isAuthorizationInProgress) {
            isAuthorizationInProgress = true;

            try {
                if (preferences.clientId.get() == null) {
                    invokeInstanceRegistrationRequest(context);
                } else {
                    invokeAuthorizationRequest(context);
                }
            } catch (Throwable t) {
                handleAuthorizationFailure(t);
            }
        }
    }

    private HashMap<String, String> createRegistrationParams() {
        registrationKeyPair = KeyPairAuxiliary.generateRandomKeyPair();

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

            params = new HashMap<>();
            params.put("CSR", csrValue);

            return params;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create registration params", e);
        }
    }

    private HashMap<String, String> createRegistrationHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        addSessionIdHeader(headers);
        headers.put("X-WL-Auth", "0");

        return headers;
    }

    private HashMap<String, String> createTokenRequestParams(String grantCode) {

        HashMap<String, String> params = new HashMap<>();

        params.put("code", grantCode);
        params.put("client_id", preferences.clientId.get());
        params.put("grant_type", "authorization_code");
        params.put("redirect_uri", HTTP_LOCALHOST);

        return params;
    }

    private HashMap<String, String> createTokenRequestHeaders(String grantCode) {
        JSONObject payload = new JSONObject();
        HashMap<String, String> headers;
        try {
            payload.put("code", grantCode);

            headers = new HashMap<>();

            KeyPair keyPair = certificateStore.getStoredKeyPair();
            String jws = jsonSigner.sign(keyPair, payload);

            headers.put("X-WL-Authenticate", jws);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create token request headers", e);
        }

        return headers;
    }

    private void addSessionIdHeader(HashMap<String, String> headers) {
        headers.put("X-WL-Session", sessionId);
    }

    private HashMap<String, String> createAuthorizationParams() {

        HashMap<String, String> params = new HashMap<>();
        params.put("response_type", "code");
        params.put("client_id", preferences.clientId.get());
        params.put("redirect_uri", HTTP_LOCALHOST);
        params.put("PARAM_SCOPE_KEY", defaultScope);

        return params;
    }

    private void invokeInstanceRegistrationRequest(final Context context) {

        AuthorizationRequestManager.RequestOptions options = new AuthorizationRequestManager.RequestOptions();

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
            X509Certificate certificate = CertificateAuxiliary.base64StringToCertificate(certificateString);

            CertificateAuxiliary.checkValidityWithPublicKey(certificate, registrationKeyPair.getPublic());

            certificateStore.saveCertificate(registrationKeyPair, certificate);

            //save the clientID separately
            preferences.clientId.set(CertificateAuxiliary.getClientIdFromCertificate(certificate));

        } catch (Exception e) {
            throw new RuntimeException("Failed to save certificate from response", e);
        }
    }

    private void invokeAuthorizationRequest(Context context) {

        AuthorizationRequestManager.RequestOptions options = new AuthorizationRequestManager.RequestOptions();

        options.parameters = createAuthorizationParams();
        options.headers = new HashMap<>();
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
        return location.get(0);
    }

    //TODO: maybe use getParameterValueFromQuery in Utils
    private String extractGrantCode(String url) throws URISyntaxException {

        //http://localhost?code=3IIXxqIKad4Zjq5VyhdlbnG0__KW5KaIIgpfub3I64qpxLPn4YMdPFysxBUp-swd3SFc8aVKsPzLKGYMpzZctv3PDonYZgf-UMalerjRLlsaCd21A2xfHMvfwJy_kY31wXWngzYQauyDp6-xI58nPu3sDsl2J_6Ce6nxyJHXUwrRk47_XmY4w3GqJVGJ3rKs&wl_result=%7B%7D
        HashMap<String, String> queryParams = getQueryParams(url);
        for (String name : queryParams.keySet()) {
            if (name.contains("code")) {
                return queryParams.get(name);
            }
        }

        throw new RuntimeException("Failed to extract grant code from url");
    }

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

    private void authorizationRequestSend(final Context context, String path, AuthorizationRequestManager.RequestOptions options, ResponseListener listener) {
        try {
            AuthorizationRequestManager tempAuthorizationRequestManager = new AuthorizationRequestManager();
            tempAuthorizationRequestManager.initialize(context, listener);
            tempAuthorizationRequestManager.sendRequest(path, options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send authorization request", e);
        }
    }

    private void invokeTokenRequest(String grantCode) {

        AuthorizationRequestManager.RequestOptions options = new AuthorizationRequestManager.RequestOptions();

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

        //iterate over the queue and call onFailure
        List<ResponseListener> listenersByScope = authorizationQueue.getListenersByScope(defaultScope);

        for (ResponseListener listener : listenersByScope) {
            listener.onFailure(request, t);
        }

        authorizationQueue.clearListenersByScope(defaultScope);
    }

    private void handleAuthorizationSuccess(Response response) {

        //iterate over the queue and call onSuccess
        List<ResponseListener> listenersByScope = authorizationQueue.getListenersByScope(defaultScope);

        for (ResponseListener listener : listenersByScope) {
            listener.onSuccess(response);
        }

        authorizationQueue.clearListenersByScope(defaultScope);
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
