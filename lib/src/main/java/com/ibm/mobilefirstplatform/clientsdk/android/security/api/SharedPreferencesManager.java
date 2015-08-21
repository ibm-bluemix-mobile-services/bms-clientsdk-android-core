package com.ibm.mobilefirstplatform.clientsdk.android.security.api;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by cirilla on 7/16/15.
 * General Shared Preferences Manager
 */
public class SharedPreferencesManager {

    protected SharedPreferences sharedPreferences;
    protected SharedPreferences.Editor editor;

    public SharedPreferencesManager(Context context, String name, int mode){
        this.sharedPreferences = context.getSharedPreferences(name, mode);
        this.editor = sharedPreferences.edit();
    }

    public class StringPreference{

        String prefName;
        String value;

        StringPreference(String prefName){
            this(prefName,null);
        }

        StringPreference(String prefName,String defaultValue){
            this.prefName = prefName;
            this.value = sharedPreferences.getString(prefName,defaultValue);
        }

        public void set(String value){
            this.value = value;
            commit();
        }

        public void clear(){
            this.value = null;
            commit();
        }

        private void commit(){
            editor.putString(prefName, value);
            editor.commit();
        }

        public String get(){
            return value;
        }
    }

    public class JSONPreference extends StringPreference{

        JSONPreference(String prefName) {
            super(prefName);
        }

        public void set(JSONObject json){
            set(json.toString());
        }

        public JSONObject getAsJSON(){
            try {
                return new JSONObject(get());
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
