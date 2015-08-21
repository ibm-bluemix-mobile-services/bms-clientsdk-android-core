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

package com.ibm.bms.clientsdk.android.security.internal;

import com.ibm.bms.clientsdk.android.core.api.ResponseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by cirilla on 8/19/15.
 */
public class AuthorizationQueue {
    private HashMap<String, ArrayList<ResponseListener>> authorizationHash;

    public AuthorizationQueue() {
        authorizationHash = new HashMap<String, ArrayList<ResponseListener>>();
    }

    public void addListener(String scope, ResponseListener listener) {
        if (scope == null) {
            scope = "";
        }

        ArrayList<ResponseListener> array = authorizationHash.get(scope);
        if (array == null) {
            array = new ArrayList<ResponseListener>();
            authorizationHash.put(scope, array);
        }

        array.add(listener);
    }

    public List<ResponseListener> getListenersByScope(String scope) {
        return authorizationHash.get(scope);
    }

    public void clearListenersByScope(String scope) {
        authorizationHash.get(scope).clear();
    }
}
