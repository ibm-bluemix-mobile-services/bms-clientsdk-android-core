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

package com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.encryption.StringEncryption;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * General shared preferences
 * Created by cirilla on 7/16/15.
 */
public class SharedPreferencesManager {

    protected SharedPreferences sharedPreferences;
    protected SharedPreferences.Editor editor;
    protected StringEncryption stringEncryption;

    public SharedPreferencesManager(Context context, String name, int mode) {
        this.sharedPreferences = context.getSharedPreferences(name, mode);
        this.editor = sharedPreferences.edit();
    }

    /**
     * Set encryption method for the using to save/load preferences
     * @param stringEncryption the method of encryption to use
     */
    public void setStringEncryption(StringEncryption stringEncryption) {
        this.stringEncryption = stringEncryption;
    }

    /**
     * Holds single string preference value
     */
    public class StringPreference {

        String prefName;
        String value;

        StringPreference(String prefName) {
            this(prefName, null);
        }

        StringPreference(String prefName, String defaultValue) {
            this.prefName = prefName;
            this.value = sharedPreferences.getString(prefName, defaultValue);
        }

        public String get() {
            return value == null ? null : stringEncryption.decrypt(value);
        }

        public void set(String value) {
            this.value = value == null ? null : stringEncryption.encrypt(value);
            commit();
        }

        public void clear() {
            this.value = null;
            commit();
        }

        private void commit() {
            editor.putString(prefName, value);
            editor.commit();
        }
    }


    /**
     * Holds single JSON preference value
     */
    public class JSONPreference extends StringPreference {

        JSONPreference(String prefName) {
            super(prefName);
        }

        public void set(JSONObject json) {
            set(json.toString());
        }

        public Map getAsMap() {
            try {
                JSONObject json = new JSONObject(get());

                Map<String, Object> asMap = new HashMap<>();
                Iterator<String> keys = json.keys();

                while (keys.hasNext()) {
                    String element = keys.next();
                    asMap.put(element, json.get(element));
                }
                return asMap;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
