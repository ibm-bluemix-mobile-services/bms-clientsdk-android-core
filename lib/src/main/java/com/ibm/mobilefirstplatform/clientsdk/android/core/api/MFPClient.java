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

package com.ibm.mobilefirstplatform.clientsdk.android.core.api;

import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.challengehandlers.ChallengeHandler;

import java.security.InvalidParameterException;
import java.util.HashMap;

/**
 * Created by vitalym on 9/16/15.
 */
abstract class MFPClient {

    protected static MFPClient instance = null;
    private int defaultTimeout = 20000;
    private HashMap<String, ChallengeHandler> challengeHandlers = new HashMap<>();

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
     * @return default timeout, in milliseconds
     */
    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    /**
     * Sets the default timeout. The SDK's default value is 20000 milliseconds.
     *
     * @param timeout specifies the new timeout, in milliseconds
     */
    public void setDefaultTimeout(int timeout) {
        defaultTimeout = timeout;
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
}
