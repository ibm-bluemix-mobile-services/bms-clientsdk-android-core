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

package com.ibm.mobilefirstplatform.clientsdk.android.security.internal.challengehandlers;

import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.AuthorizationRequestAgent;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by vitalym on 7/16/15.
 */
public class ChallengeHandler implements AuthenticationContext {

    private String realm;
    private volatile AuthenticationListener listener;
    private volatile ArrayList<AuthorizationRequestAgent> waitingRequests = new ArrayList<AuthorizationRequestAgent>();
    private volatile AuthorizationRequestAgent activeRequest;

    public void initialize(String realm, AuthenticationListener listener) {
        this.realm = realm;
        this.listener = listener;
    }

    @Override
    public synchronized void submitAuthenticationChallengeAnswer(JSONObject answer) {
        if (activeRequest == null) {
            return;
        }

        if (answer != null) {
            activeRequest.submitAnswer(answer, realm);
        } else {
            activeRequest.removeExpectedAnswer(realm);
        }

        setActiveRequest(null);
    }

    @Override
    public synchronized void submitAuthenticationSuccess () {
        if (activeRequest != null) {
            activeRequest.removeExpectedAnswer(realm);
            setActiveRequest(null);
        }

        releaseWaitingList();
    }

    @Override
    public synchronized void submitAuthenticationFailure (JSONObject info) {
        if (activeRequest != null) {
            activeRequest.requestFailed(info);
            setActiveRequest(null);
        }

        releaseWaitingList();
    }

    public synchronized void handleChallenge(AuthorizationRequestAgent request, JSONObject challenge, Context context) {
        if (activeRequest == null) {
            setActiveRequest(request);
            if (listener != null) {
                listener.onAuthenticationChallengeReceived(this, challenge, context);
            }
        } else {
            waitingRequests.add(request);
        }
    }

    //TODO - should it accept Context like handleChallenge???
    public synchronized void handleSuccess(JSONObject success) {
        if (listener != null) {
            listener.onAuthenticationSuccess(success);
        }
        releaseWaitingList();
        setActiveRequest(null);
    }

    //TODO - should it accept Context like handleChallenge???
    public synchronized void handleFailure(JSONObject failure) {
        if (listener != null) {
            listener.onAuthenticationFailure(failure);
        }
        clearWaitingList();
        setActiveRequest(null);
    }

    private synchronized void setActiveRequest(AuthorizationRequestAgent request) {
        activeRequest = request;
    }

    private synchronized void releaseWaitingList() {
        for (AuthorizationRequestAgent request : waitingRequests) {
            request.removeExpectedAnswer(realm);
        }

        clearWaitingList();
    }

    private synchronized void clearWaitingList() {
        waitingRequests.clear();
    }
}
