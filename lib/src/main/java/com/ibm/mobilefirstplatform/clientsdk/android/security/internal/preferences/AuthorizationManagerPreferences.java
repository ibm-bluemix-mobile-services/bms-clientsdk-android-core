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

package com.ibm.mobilefirstplatform.clientsdk.android.security.internal.preferences;

import android.content.Context;
import android.provider.Settings;

import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.encryption.AESStringEncryption;

/**
 * Shared preferences that are used for authorization
 */

public class AuthorizationManagerPreferences extends SharedPreferencesManager {

    public PolicyPreference persistencePolicy = new PolicyPreference("persistencePolicy", AuthorizationManager.PersistencePolicy.ALWAYS);
    public StringPreference clientId = new StringPreference("clientId");
    public TokenPreference accessToken = new TokenPreference("accessToken");
    public TokenPreference idToken = new TokenPreference("idToken");

    public JSONPreference userIdentity = new JSONPreference("userIdentity");
    public JSONPreference deviceIdentity = new JSONPreference("deviceIdentity");
    public JSONPreference appIdentity = new JSONPreference("appIdentity");

    public AuthorizationManagerPreferences(Context context) {
        super(context, "AuthorizationManagerPreferences", Context.MODE_PRIVATE);

        String uuid = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        setStringEncryption(new AESStringEncryption(uuid));
    }

    public class PolicyPreference {

        private AuthorizationManager.PersistencePolicy value;
        private String prefName;

        public PolicyPreference(String prefName, AuthorizationManager.PersistencePolicy defaultValue) {
            this.prefName = prefName;
            value = AuthorizationManager.PersistencePolicy.valueOf(sharedPreferences.getString(prefName, defaultValue.toString()));
        }

        public AuthorizationManager.PersistencePolicy get() {
            return value;
        }

        public void set(AuthorizationManager.PersistencePolicy value) {
            this.value = value;
            editor.putString(prefName, value.toString());
            editor.commit();
        }
    }

    public class TokenPreference {

        String runtimeValue;
        StringPreference savedValue;

        public TokenPreference(String prefName) {
            savedValue = new StringPreference(prefName);
        }

        public void set(String value) {
            runtimeValue = value;
            if (persistencePolicy.get() == AuthorizationManager.PersistencePolicy.ALWAYS) {
                savedValue.set(value);
            } else {
                savedValue.clear();
            }
        }

        public String get() {
            if (runtimeValue == null && persistencePolicy.get() == AuthorizationManager.PersistencePolicy.ALWAYS) {
                return savedValue.get();
            }
            return runtimeValue;
        }

        public void updateStateByPolicy() {
            if (persistencePolicy.get() == AuthorizationManager.PersistencePolicy.ALWAYS) {
                savedValue.set(runtimeValue);
            } else {
                savedValue.clear();
            }
        }

        public void clear() {
            savedValue.clear();
            runtimeValue = null;
        }
    }
}
