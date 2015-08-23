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

package com.ibm.mobilefirstplatform.clientsdk.android.security.api;

import android.content.Context;

/**
 * Created by cirilla on 7/16/15.
 */

public class AuthorizationManagerPreferences extends SharedPreferencesManager {


    public PolicyPreference persistencePolicy = new PolicyPreference("persistencePolicy", AuthorizationManager.PersistencePolicy.ALWAYS);
    public StringPreference clientId = new StringPreference("clientId");
    public TokenPreference accessToken = new TokenPreference("accessToken");
    public TokenPreference idToken = new TokenPreference("idToken");

    public JSONPreference userIdentity = new JSONPreference("userIdentity");
    public JSONPreference deviceIdentity = new JSONPreference("deviceIdentity");
    public JSONPreference appIdentity = new JSONPreference("appIdentity");

    //public IdentityPreference userIdentity = new IdentityPreference("imf.user");
    //public IdentityPreference deviceIdentity = new IdentityPreference("imf.device");
    //public IdentityPreference appIdentity = new IdentityPreference("imf.application");
    //public StringPreference deviceId = new StringPreference("deviceId");

    public AuthorizationManagerPreferences(Context context) {
        super(context, "AuthorizationManagerPreferences" , Context.MODE_PRIVATE);
    }

    public class PolicyPreference{

        private AuthorizationManager.PersistencePolicy value;
        private String prefName;

        public PolicyPreference(String prefName, AuthorizationManager.PersistencePolicy defaultValue){
            this.prefName = prefName;
            value = AuthorizationManager.PersistencePolicy.valueOf(sharedPreferences.getString(prefName,defaultValue.toString()));
        }

        public AuthorizationManager.PersistencePolicy get(){
            return value;
        }

        public void set(AuthorizationManager.PersistencePolicy value){
            this.value = value;
            editor.putString(prefName, value.toString());
            editor.commit();
        }
    }

    public class TokenPreference{

        String runtimeValue;
        StringPreference savedValue;

        public TokenPreference(String prefName) {
            savedValue = new StringPreference(prefName);
        }

        public void set(String value){
            runtimeValue = value;
            if (persistencePolicy.get() == AuthorizationManager.PersistencePolicy.ALWAYS){
                savedValue.set(value);
            }
            else{
                savedValue.clear();
            }
        }

        public String get(){
            if (runtimeValue == null && persistencePolicy.get() == AuthorizationManager.PersistencePolicy.ALWAYS){
                return savedValue.get();
            }
            return runtimeValue;
        }

        public void updateStateByPolicy(){
            if (persistencePolicy.get() == AuthorizationManager.PersistencePolicy.ALWAYS){
                savedValue.set(runtimeValue);
            }

            else{
                savedValue.clear();
            }
        }
    }

    /*
    class IdentityPreference{

        String objectId;

        public IdentityPreference(String objectId) {
            this.objectId = objectId;
        }

        public JSONObject get(){
            JSONObject object = null;
            try {
                JSONObject idTokenJSON = getIdTokenAsJSON();
                if (idTokenJSON!=null){
                    object = idTokenJSON.getJSONObject(objectId);
                }
            } catch (JSONException e) {}
            return object;
        }

        private JSONObject getIdTokenAsJSON(){
            JSONObject idTokenJSON = null;
            if (idToken.get()!=null){
                try{
                    String[] idTokenData = idToken.get().split("\\.");
                    byte[] decodedIdTokenData = Base64.decode(idTokenData[1], Base64.DEFAULT);
                    String decodedIdTokenString = new String(decodedIdTokenData);
                    idTokenJSON = new JSONObject(decodedIdTokenString);
                } catch(JSONException e){}
            }
            return idTokenJSON;
        }
    }
    */

}
