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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.FailResponse;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.MFPRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResourceRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.challengehandlers.ChallengeHandler;
import com.squareup.okhttp.HttpUrl;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by vitalym on 7/16/15.
 */

public class AuthorizationRequestManager implements ResponseListener {

    private final static String PACKAGE_NAME = "com.ibm.mobilefirstplatform.clientsdk.android.security.internal";
    private final static String AUTH_SERVER_NAME = "imf-authserver";
    private final static String WL_RESULT = "wl_result";
    private final static String AUTH_PATH = "authorization/v1/apps/";
    private final static String REWRITE_DOMAIN_HEADER_NAME = "X-REWRITE-DOMAIN";

    private String requestPath;
    private RequestOptions requestOptions;

    private ResponseListener listener;
    private JSONObject answers;
    private Context context;

    static public class RequestOptions {
        public String requestMethod;
        public int timeout;

        public HashMap<String, String> headers;
        public HashMap<String, String> parameters;
    }

    public void initialize(Context context, ResponseListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void sendRequest(String path, RequestOptions options) throws IOException, JSONException {
        String rootUrl = null;

        if (path.indexOf(BMSClient.HTTP_SCHEME) == 0 && path.contains(":")) {
            // request using full path, split the URL to root and path
            URL url = new URL(path);
            path = url.getPath();
            rootUrl = url.toString().replace(path, "");
        } else {
            // "path" is a relative
            String backendRoute = BMSClient.getInstance().getBackendRoute();
            rootUrl = backendRoute.charAt(backendRoute.length() - 1) == '/' ? backendRoute.concat(AUTH_SERVER_NAME) :
                    backendRoute.concat("/" + AUTH_SERVER_NAME);

            String pathWithTenantId = AUTH_PATH + BMSClient.getInstance().getBackendGUID();
            rootUrl = rootUrl.concat("/" + pathWithTenantId);
        }

        sendRequestInternal(rootUrl, path, options);
    }

    public void resendRequest() throws IOException, JSONException {
        sendRequest(requestPath, requestOptions);
    }

    private void sendRequestInternal(String rootUrl, String path, RequestOptions options) throws IOException, JSONException {
        Logger.getInstance(PACKAGE_NAME).debug("Sending request to root: " + requestPath + " with path: " + path);

        HttpUrl root = HttpUrl.parse(rootUrl);
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme(root.scheme())
                .host(root.host())
                .port(root.port());

        for (String segment : root.pathSegments()) {
            urlBuilder.addPathSegment(segment);
        }

        String segments[] = path.split("/");
        for (int i = 0 ; i < segments.length; i++) {
            urlBuilder.addPathSegment(segments[i]);
        }

        HttpUrl url = urlBuilder.build();

        // used to resend request
        this.requestPath = url.toString();
        this.requestOptions = options;

        MFPRequest request = new MFPRequest(this.requestPath, options.requestMethod);

        if (options.timeout != 0) {
            request.setTimeout(options.timeout);
        } else {
            request.setTimeout(BMSClient.getInstance().getDefaultTimeout());
        }

        if (options.headers != null) {
            for (Map.Entry<String, String> entry : options.headers.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }

        if (answers != null) {
            String answer = answers.toString(0);

            String authorizationHeaderValue = String.format("Bearer %s", answer.replace("\n", ""));
            request.addHeader("Authorization", authorizationHeaderValue);
            Logger.getInstance(PACKAGE_NAME).debug("Added authorization header to request: " + authorizationHeaderValue);
        }

        String rewriteDomainHeaderValue = BMSClient.getInstance().getRewriteDomain();
        request.addHeader(REWRITE_DOMAIN_HEADER_NAME, rewriteDomainHeaderValue);

        // TODO add user agent

        request.setFollowRedirects(false);

        if (options.requestMethod.compareTo(ResourceRequest.GET) == 0) {
            request.setQueryParameters(options.parameters);
            request.send(this);
        } else {
            request.send(options.parameters, this);
        }
    }

    public void setExpectedAnswers(ArrayList<String> realms) {
        if (answers == null) {
            return;
        }

        for (String realm : realms) {
            try {
                answers.put(realm, "");
            } catch (JSONException t) {
                Logger.getInstance(PACKAGE_NAME).error("setExpectedAnswers failed with exception: " + t.getLocalizedMessage(), t);
            }
        }
    }

    public void removeExpectedAnswer(String realm) {
        if (answers != null) {
            answers.remove(realm);
        }

        try {
            if (isAnswersFilled()) {
                resendRequest();
            }
        } catch (Throwable t) {
            Logger.getInstance(PACKAGE_NAME).error("removeExpectedAnswer failed with exception: " + t.getLocalizedMessage(), t);
        }
    }

    public void submitAnswer(JSONObject answer, String realm) {
        if (answers == null) {
            answers = new JSONObject();
        }

        try {
            answers.put(realm, answer);
            if (isAnswersFilled()) {
                resendRequest();
            }
        } catch (Throwable t) {
            Logger.getInstance(PACKAGE_NAME).error("removeExpectedAnswer failed with exception: " + t.getLocalizedMessage(), t);
        }
    }

    public boolean isAnswersFilled() throws JSONException {
        Iterator<String> it = answers.keys();
        while (it.hasNext()) {
            String key = it.next();
            Object value = answers.get(key);

            if ((value instanceof String) && ((String) value).compareTo("") == 0) {
                return false;
            }
        }

        return true;
    }

    private void processRedirectResponse(Response response) {
        java.util.List<String> locationHeaders = response.getResponseHeader("Location");

        if (locationHeaders.size() == 0) {
            listener.onSuccess(response);
            return;
        }

        String location = locationHeaders.get(0);

        try {
            URL url = new URL(location);
            String query = url.getQuery();

            if (query.contains(WL_RESULT)) {
                String result = Utils.getParameterValueFromQuery(query, WL_RESULT);
                JSONObject jsonResult = new JSONObject(result);

                JSONObject jsonFailures = jsonResult.optJSONObject("WL-Authentication-Failure");

                if (jsonFailures != null) {
                    processFailures(jsonFailures);
                    // TODO fill the resposne properly
                    listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, response), null);
                    return;
                }

                JSONObject jsonSuccesses = jsonResult.optJSONObject("WL-Authentication-Success");

                if (jsonSuccesses != null) {
                    processSuccesses(jsonSuccesses);
                }
            }

            listener.onSuccess(response);

        } catch (Throwable t) {
            Logger.getInstance(PACKAGE_NAME).error("processRedirectResponse failed with exception: " + t.getLocalizedMessage(), t);
            listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, response), t);
        }


    }

    private void processResponse(Response response) {
        JSONObject jsonResponse = Utils.extractSecureJson(response);
        JSONObject jsonChallenges = jsonResponse == null ? null : jsonResponse.optJSONObject("challenges");

        if (jsonChallenges != null) {
            startHandleChallenges(jsonChallenges, response);
        } else {
            listener.onSuccess(response);
        }
    }

    private void startHandleChallenges(JSONObject jsonChallenges, Response response) {
        ArrayList<String> challenges = getRealmsFromJson(jsonChallenges);

        if (is401(response)) {
            setExpectedAnswers(challenges);
        }

        for (String realm : challenges) {
            ChallengeHandler handler = BMSClient.getInstance().getChallengeHandler(realm);
            if (handler != null) {
                JSONObject challenge = jsonChallenges.optJSONObject(realm);
                handler.handleChallenge(this, challenge, context);
            } else {
                listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, response), null);
            }
        }
    }

    private boolean is401(Response response) {
        if (response.getStatus() == 401) {
            String challengesHeader = response.getFirstResponseHeader("WWW-Authenticate");

            if (challengesHeader != null && challengesHeader.compareTo("WL-Composite-Challenge") == 0) {
                return true;
            }
        }

        return false;
    }

    private void processFailures(JSONObject jsonFailures) {
        if (jsonFailures == null) {
            return;
        }

        ArrayList<String> challenges = getRealmsFromJson(jsonFailures);
        for (String realm : challenges) {
            ChallengeHandler handler = BMSClient.getInstance().getChallengeHandler(realm);
            if (handler != null) {
                JSONObject challenge = jsonFailures.optJSONObject(realm);
                handler.handleFailure(challenge);
            } else {
                // TODO Log - challenge handler does not exist
            }
        }
    }

    private void processSuccesses(JSONObject jsonSuccesses) {
        if (jsonSuccesses == null) {
            return;
        }

        ArrayList<String> challenges = getRealmsFromJson(jsonSuccesses);
        for (String realm : challenges) {
            ChallengeHandler handler = BMSClient.getInstance().getChallengeHandler(realm);
            if (handler != null) {
                JSONObject challenge = jsonSuccesses.optJSONObject(realm);
                handler.handleSuccess(challenge);
            } else {
                // TODO Log - challenge handler does not exist
            }
        }
    }

    public void requestFailed(JSONObject info) {
        if (listener != null) {
            Throwable t = null;
            if (info != null) {
                t = new RuntimeException(info.toString());
            }
            listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, null), t);
        }
    }

    private ArrayList<String> getRealmsFromJson(JSONObject jsonChallenges) {
        Iterator<String> challengesIterator = jsonChallenges.keys();
        ArrayList<String> challenges = new ArrayList<String>();

        while (challengesIterator.hasNext()) {
            challenges.add(challengesIterator.next());
        }

        return challenges;
    }

    @Override
    public void onSuccess(Response response) {
        if (response.isRedirect()) {
            processRedirectResponse(response);
        } else {
            processResponse(response);
        }
    }

    @Override
    public void onFailure(FailResponse response, Throwable t) {
        if (is401(response)) {
            processResponse(response);
        } else {
            listener.onFailure(response, t);
        }
    }
}
