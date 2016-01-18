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

package com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api.identity;

import org.json.JSONObject;

import java.util.Map;

/**
 * Holds the user identity json
 */
public class UserIdentity extends JSONObject {

    final static String ID = "id";
    final static String AUTH_BY = "authBy";
    final static String DISPLAY_NAME = "displayName";

    public UserIdentity(Map asMap) {
        super(asMap);
    }

    /**
     * @return user id
     */
    public String getId() {
        return optString(ID);
    }

    /**
     * @return the auth type that used to authenticate the use
     */
    public String getAuthBy() {
        return optString(AUTH_BY);
    }

    /**
     * @return user display name
     */
    public String getDisplayName() {
        return optString(DISPLAY_NAME);
    }
}
