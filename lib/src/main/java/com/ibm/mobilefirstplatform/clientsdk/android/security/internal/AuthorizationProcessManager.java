/*
 *     Copyright 2015 IBM Corp.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManagerPreferences;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.CertificateAuxiliary;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.CertificateStore;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.DefaultJSONSigner;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate.KeyPairAuxiliary;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.data.ApplicationData;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.data.DeviceData;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
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

            if (preferences.clientId.get() == null) {
                invokeInstanceRegistrationRequest(context);
            } else {
                invokeAuthorizationRequest(context);
            }
        }
    }

    private HashMap<String, String> createRegistrationParams() {
        registrationKeyPair = KeyPairAuxiliary.generateRandomKeyPair();

        JSONObject csrJSON = new JSONObject();
        HashMap<String, String> params = null;

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

            params = new HashMap<String, String>();
            params.put("CSR", csrValue);

        } catch (Exception e) { //TODO: handle the exception in other place
            e.printStackTrace();
        }

        return params;
    }

    private HashMap<String, String> createTokenRequestParams(String grantCode) {

        HashMap<String, String> params = new HashMap<String, String>();

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

            headers = new HashMap<String, String>();

            KeyPair keyPair = certificateStore.getStoredKeyPair();
            String jws = jsonSigner.sign(keyPair, payload);

            headers.put("X-WL-Authenticate", jws);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


        return headers;
    }

    private void addSessionIdHeader(HashMap<String, String> headers) {
        headers.put("X-WL-Session", sessionId);
    }

    private HashMap<String, String> createAuthorizationParams() {

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("response_type", "code");
        params.put("client_id", preferences.clientId.get());
        params.put("redirect_uri", HTTP_LOCALHOST);
        params.put("PARAM_SCOPE_KEY", defaultScope);

        return params;
    }

    private void invokeInstanceRegistrationRequest(final Context context) {

        AuthorizationRequestManager.RequestOptions options = new AuthorizationRequestManager.RequestOptions();

        options.parameters = createRegistrationParams();
        options.headers = new HashMap<String, String>();
        addSessionIdHeader(options.headers);
        options.headers.put("X-WL-Auth", "0");
        options.requestMethod = "POST";

        InnerAuthorizationResponseListener listener = new InnerAuthorizationResponseListener() {
            @Override
            public void handleAuthorizationSuccessResponse(Response response) throws Exception {
                saveCertificateFromResponse(response);
                invokeAuthorizationRequest(context);
            }
        };

        authorizationRequestSend("clients/instance", options, listener);

        /*
        ResponseListener listener = new ResponseListener() {
            @Override
            public void onSuccess(com.ibm.bms.clientsdk.android.core.api.Response response) {
                saveCertificateFromResponse(response);
                invokeAuthorizationRequest();
            }

            @Override
            public void onFailure(FailResponse response, Throwable t) {
                handleAuthorizationFailure(response, t);
            }
        };
        */
    }


    private void saveCertificateFromResponse(com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response response) throws JSONException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {

        //try {
        String responseBody = response.getResponseText();
        JSONObject jsonResponse = new JSONObject(responseBody);

        // handle certificate
        String certificateString = jsonResponse.getString("certificate");
        X509Certificate certificate = CertificateAuxiliary.base64StringToCertificate(certificateString);

        if (CertificateAuxiliary.checkValidityWithPublicKey(certificate, registrationKeyPair.getPublic())) {
            certificateStore.saveCertificate(registrationKeyPair, certificate);

            //save the clientID separately
            preferences.clientId.set(CertificateAuxiliary.getClientIdFromCertificate(certificate));
        } else {
            throw new RuntimeException("Invalid Certificate Key");
            //handleAuthorizationFailure("Invalid Certificate Key");
        }

        /*
        } catch (Exception e) {
            handleAuthorizationFailure(e);
        }
        */
    }

    private void invokeAuthorizationRequest(Context context) {

        AuthorizationRequestManager.RequestOptions options = new AuthorizationRequestManager.RequestOptions();

        options.parameters = createAuthorizationParams();
        options.headers = new HashMap<String, String>();
        addSessionIdHeader(options.headers);
        options.requestMethod = "GET";

        InnerAuthorizationResponseListener listener = new InnerAuthorizationResponseListener() {
            @Override
            public void handleAuthorizationSuccessResponse(Response response) throws Exception {
                List<String> location = response.getResponseHeaders().get("Location");

                if (location == null) {
                    throw new RuntimeException("Can't find 'Location' Header");
                }

                String grantCode = extractGrantCode(location.get(0));
                invokeTokenRequest(grantCode);
            }
        };

        authorizationRequestSend("authorization", options, listener);
        /*
        ResponseListener listener = new ResponseListener() {
            @Override
            public void onSuccess(com.ibm.bms.clientsdk.android.core.api.Response response) {

                List<String> location = response.getResponseHeaders().get("Location");

                if (location == null) {
                    handleAuthorizationFailure("Can't find 'Location' Header");
                } else {
                    String grantCode = extractGrantCode(location.get(0)); //TODO: check if we should handle all the list
                    invokeTokenRequest(grantCode);
                }
            }

            @Override
            public void onFailure(FailResponse response, Throwable t) {
                handleAuthorizationFailure(response, t);
            }
        };
        */

    }

    public String extractGrantCode(String url) throws URISyntaxException {
        List<NameValuePair> pp = URLEncodedUtils.parse(new URI(url), "UTF8");
        String result = null;
        for (NameValuePair pair : pp) {
            if (pair.getName().equals("code")){
                result = pair.getValue();
                break;
            }
        }
        return result;
    }

    private void authorizationRequestSend(String path, AuthorizationRequestManager.RequestOptions options, ResponseListener listener) {
        try {
            AuthorizationRequestManager tempAuthorizationRequestManager = new AuthorizationRequestManager();
            tempAuthorizationRequestManager.initialize(null, listener);
            tempAuthorizationRequestManager.sendRequest(path, options);
        } catch (Exception e) {
            handleAuthorizationFailure(e);
        }
    }

    private void invokeTokenRequest(String grantCode) {

        AuthorizationRequestManager.RequestOptions options = new AuthorizationRequestManager.RequestOptions();

        HashMap<String, String> tokenRequestHeaders = createTokenRequestHeaders(grantCode);
        HashMap<String, String> tokenRequestParams = createTokenRequestParams(grantCode);

        options.parameters = tokenRequestParams;
        options.headers = tokenRequestHeaders;
        addSessionIdHeader(options.headers);
        options.requestMethod = "POST";

        InnerAuthorizationResponseListener listener = new InnerAuthorizationResponseListener() {
            @Override
            public void handleAuthorizationSuccessResponse(Response response) throws Exception {
                saveTokenFromResponse(response);
                handleAuthorizationSuccess(response);
            }
        };

        authorizationRequestSend("token", options, listener);
        /*
        ResponseListener listener = new ResponseListener() {
            @Override
            public void onSuccess(com.ibm.bms.clientsdk.android.core.api.Response response) {
                saveTokenFromResponse(response); //TOOD: not good! if there is exception the handleSucess will be called anyway
                handleAuthorizationSuccess(response);
            }

            @Override
            public void onFailure(FailResponse response, Throwable t) {
                handleAuthorizationFailure(response, t);
            }
        };
        */
    }

    private void saveTokenFromResponse(com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response response) throws JSONException {

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
    }

    private void handleAuthorizationFailure(Exception e) {
        handleAuthorizationFailure(null, e);
    }

//    private void handleAuthorizationFailure(String message) {
//        handleAuthorizationFailure(null, new Error(message));
//    }

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

    private void handleAuthorizationSuccess(com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response response) {

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
