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
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.challengehandlers.ChallengeHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by vitalym on 7/16/15.
 */

/**
 * AuthorizationRequestAgent builds and sends requests to authorization server. It also handles
 * authorization challenges and re-sends the requests as necessary.
 */
public class AuthorizationRequestAgent implements ResponseListener {
    private static Logger logger = Logger.getInstance("AuthorizationRequestAgent");
    /**
     * Parts of the path to authorization endpoint.
     */
    private final static String AUTH_SERVER_NAME = "imf-authserver";
    private final static String AUTH_PATH = "authorization/v1/apps/";

    /**
     * The name of "result" parameter returned from authorization endpoint.
     */
    private final static String WL_RESULT = "wl_result";

    /**
     * Name of rewrite domain header. This header is added to authorization requests.
     */
    private final static String REWRITE_DOMAIN_HEADER_NAME = "X-REWRITE-DOMAIN";

    /**
     * Name of location header.
     */
    private final static String LOCATION_HEADER_NAME = "Location";

    /**
     * Name of the standard "www-authenticate" header.
     */
    private final static String AUTHENTICATE_HEADER_NAME = "WWW-Authenticate";

    /**
     * Name of "www-authenticate" header value.
     */
    private final static String AUTHENTICATE_HEADER_VALUE = "WL-Composite-Challenge";

    /**
     * Names of JSON values returned from the server.
     */
    private final static String AUTH_FAILURE_VALUE_NAME = "WL-Authentication-Failure";
    private final static String AUTH_SUCCESS_VALUE_NAME = "WL-Authentication-Success";
    private final static String CHALLENGES_VALUE_NAME = "challenges";

    /**
     * requestPath and requestOptions are cached to re-send a request after all challenges have been handled.
     */
    private String requestPath;
    private RequestOptions requestOptions;

    /**
     * Response listener specified by request sender.
     */
    private ResponseListener listener;

    /**
     * Contains challenge answers. Each answer is mapped to a realm.
     */
    private JSONObject answers;

    /**
     * Context is provided by the caller during initialization and passed to challenge handlers later.
     */
    private Context context;

    /**
     * The request options are specified by the caller and cached for subsequent requests.
     */
    static public class RequestOptions {
        public RequestOptions() {
            requestMethod = MFPRequest.GET;
        }

        public String requestMethod;
        public int timeout;

        public HashMap<String, String> headers;
        public HashMap<String, String> parameters;
    }

    /**
     * Initializes the request manager.
     *
     * @param context  Context to be cached and passed to challenge handlers later.
     * @param listener Response listener. Called when an authorization response has been processed.
     */
    public void initialize(Context context, ResponseListener listener) {
        this.context = context;
        this.listener = listener;
        logger.debug("AuthorizationRequestAgent is initialized.");
    }

    /**
     * Assembles the request path from root and path to authorization endpoint and sends the request.
     *
     * @param path    Path to authorization endpoint
     * @param options Request options
     * @throws IOException
     * @throws JSONException
     */
    public void sendRequest(String path, RequestOptions options) throws IOException, JSONException {
        String rootUrl;

        if (path == null) {
            logger.error("'path' parameter can't be null.");
            if (listener != null) {
                listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, null), null);
            }

            return;
        }

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

    /**
     * Re-sends an authorization request after all challenges have been handled.
     *
     * @throws IOException
     * @throws JSONException
     */
    public void resendRequest() throws IOException, JSONException {
        sendRequest(requestPath, requestOptions);
    }

    /**
     * Builds an authorization request and sends it. It also caches the request url and request options in
     * order to be able to re-send the request when authorization challenges have been handled.
     *
     * @param rootUrl Root of authorization server.
     * @param path    Path to authorization endpoint.
     * @param options Request options.
     * @throws IOException
     * @throws JSONException
     */
    private void sendRequestInternal(String rootUrl, String path, RequestOptions options) throws IOException, JSONException {
        logger.debug("Sending request to root: " + requestPath + " with path: " + path);

        // create default options object with GET request method.
        if (options == null) {
            options = new RequestOptions();
        }

        // used to resend request
        this.requestPath = Utils.concatenateUrls(rootUrl, path);
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
            // 0 means no spaces in the generated string
            String answer = answers.toString(0);

            String authorizationHeaderValue = String.format("Bearer %s", answer.replace("\n", ""));
            request.addHeader("Authorization", authorizationHeaderValue);
            logger.debug("Added authorization header to request: " + authorizationHeaderValue);
        }

        String rewriteDomainHeaderValue = BMSClient.getInstance().getRewriteDomain();
        request.addHeader(REWRITE_DOMAIN_HEADER_NAME, rewriteDomainHeaderValue);

        // we want to handle redirects in-place
        request.setFollowRedirects(false);

        if (MFPRequest.GET.equalsIgnoreCase(options.requestMethod)) {
            request.setQueryParameters(options.parameters);
            request.send(this);
        } else {
            request.send(options.parameters, this);
        }
    }

    /**
     * Initializes the collection of expected challenge answers.
     *
     * @param realms List of realms
     */
    private void setExpectedAnswers(ArrayList<String> realms) {
        if (answers == null) {
            return;
        }

        for (String realm : realms) {
            try {
                answers.put(realm, "");
            } catch (JSONException t) {
                logger.error("setExpectedAnswers failed with exception: " + t.getLocalizedMessage(), t);
            }
        }
    }

    /**
     * Removes an expected challenge answer from collection.
     *
     * @param realm Realm of the answer to remove.
     */
    public void removeExpectedAnswer(String realm) {
        if (answers != null) {
            answers.remove(realm);
        }

        try {
            if (isAnswersFilled()) {
                resendRequest();
            }
        } catch (Throwable t) {
            logger.error("removeExpectedAnswer failed with exception: " + t.getLocalizedMessage(), t);
        }
    }

    /**
     * Adds an expected challenge answer to collection of answers.
     *
     * @param answer Answer to add.
     * @param realm  Authentication realm for the answer.
     */
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
            logger.error("removeExpectedAnswer failed with exception: " + t.getLocalizedMessage(), t);
        }
    }

    /**
     * Verifies whether all expected challenges have been answered, or not.
     *
     * @return <code>true</code> if all answers have been submitted, otherwise <code>false</code>.
     * @throws JSONException
     */
    public boolean isAnswersFilled() throws JSONException {
        if (answers == null) {
            return true;
        }

        Iterator<String> it = answers.keys();
        while (it.hasNext()) {
            String key = it.next();
            Object value = answers.get(key);

            if ((value instanceof String) && ((String) value).equals("")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Processes redirect response from authorization endpoint.
     *
     * @param response Response from the server.
     */
    private void processRedirectResponse(Response response) {
        // a valid redirect response must contain the Location header.
        List<String> locationHeaders = response.getResponseHeader(LOCATION_HEADER_NAME);

        if (locationHeaders == null || locationHeaders.size() == 0) {
            if (listener != null) {
                logger.error("Redirect response does not contain location.");
                listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, response), null);
            }
            return;
        }

        String location = locationHeaders.get(0);

        try {
            // the redirect location url should contain "wl_result" value in query parameters.
            URL url = new URL(location);
            String query = url.getQuery();

            if (query.contains(WL_RESULT)) {
                String result = Utils.getParameterValueFromQuery(query, WL_RESULT);
                JSONObject jsonResult = new JSONObject(result);

                // process failures if any
                JSONObject jsonFailures = jsonResult.optJSONObject(AUTH_FAILURE_VALUE_NAME);

                if (jsonFailures != null) {
                    processFailures(jsonFailures);
                    if (listener != null) {
                        listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, response), null);
                    }
                    return;
                }

                // process successes if any
                JSONObject jsonSuccesses = jsonResult.optJSONObject(AUTH_SUCCESS_VALUE_NAME);

                if (jsonSuccesses != null) {
                    processSuccesses(jsonSuccesses);
                }
            }

            if (listener != null) {
                // the rest is handles by the caller
                listener.onSuccess(response);
            }

        } catch (Throwable t) {
            logger.error("processRedirectResponse failed with exception: " + t.getLocalizedMessage(), t);
            if (listener != null) {
                listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, response), t);
            }
        }
    }

    /**
     * Process a response from the server.
     *
     * @param response Server response.
     */
    private void processResponse(Response response) {
        // at this point a server response should contain a secure JSON with challenges
        JSONObject jsonResponse = Utils.extractSecureJson(response);
        JSONObject jsonChallenges = (jsonResponse == null) ? null : jsonResponse.optJSONObject(CHALLENGES_VALUE_NAME);

        if (jsonChallenges != null) {
            startHandleChallenges(jsonChallenges, response);
        } else if (listener != null) {
            listener.onSuccess(response);
        }
    }

    /**
     * Handles authentication challenges.
     *
     * @param jsonChallenges Collection of challenges.
     * @param response       Server response.
     */
    private void startHandleChallenges(JSONObject jsonChallenges, Response response) {
        ArrayList<String> challenges = getRealmsFromJson(jsonChallenges);

        if (isAuthorizationRequired(response)) {
            setExpectedAnswers(challenges);
        }

        for (String realm : challenges) {
            ChallengeHandler handler = BMSClient.getInstance().getChallengeHandler(realm);
            if (handler != null) {
                JSONObject challenge = jsonChallenges.optJSONObject(realm);
                handler.handleChallenge(this, challenge, context);
            } else {
                logger.error("Challenge handler for realm is not found: " + realm);
                if (listener != null) {
                    listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, response), null);
                }
            }
        }
    }

    /**
     * Checks server response for MFP 401 error. This kind of response should contain MFP authentication challenges.
     *
     * @param response Server response.
     * @return <code>true</code> if the server response contains 401 status code along with MFP challenges.
     */
    private boolean isAuthorizationRequired(Response response) {
        if (response.getStatus() == 401) {
            String challengesHeader = response.getFirstResponseHeader(AUTHENTICATE_HEADER_NAME);

            if (AUTHENTICATE_HEADER_VALUE.equalsIgnoreCase(challengesHeader)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Processes authentication failures.
     *
     * @param jsonFailures Collection of authentication failures.
     */
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
                logger.error("Challenge handler for realm is not found: " + realm);
            }
        }
    }

    /**
     * Processes authentication successes.
     *
     * @param jsonSuccesses Collection of authentication successes.
     */
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
                logger.error("Challenge handler for realm is not found: " + realm);
            }
        }
    }

    /**
     * Called when a request to authorization server failed.
     *
     * @param info Extended information about the failure.
     */
    public void requestFailed(JSONObject info) {
        logger.error("Request failed with info: " + (info == null ? "info is null" : info.toString()));

        if (listener != null) {
            Throwable t = null;
            if (info != null) {
                t = new RuntimeException(info.toString());
            }
            listener.onFailure(new FailResponse(FailResponse.ErrorCode.UNABLE_TO_CONNECT, null), t);
        }
    }

    /**
     * Iterates a JSON object containing authorization challenges and builds a list of reals.
     *
     * @param jsonChallenges Collection of challenges.
     * @return Array with realms.
     */
    private ArrayList<String> getRealmsFromJson(JSONObject jsonChallenges) {
        Iterator<String> challengesIterator = jsonChallenges.keys();
        ArrayList<String> challenges = new ArrayList<>();

        while (challengesIterator.hasNext()) {
            challenges.add(challengesIterator.next());
        }

        return challenges;
    }

    /**
     * Called when request succeeds.
     *
     * @param response the server response
     */
    @Override
    public void onSuccess(Response response) {
        if (response.isRedirect()) {
            processRedirectResponse(response);
        } else {
            processResponse(response);
        }
    }

    /**
     * Called when request fails.
     *
     * @param response Contains detail regarding why the request failed
     * @param t        Exception that could have caused the request to fail. Null if no Exception thrown.
     */
    @Override
    public void onFailure(FailResponse response, Throwable t) {
        if (isAuthorizationRequired(response)) {
            processResponse(response);
        } else if (listener != null) {
            listener.onFailure(response, t);
        }
    }
}
