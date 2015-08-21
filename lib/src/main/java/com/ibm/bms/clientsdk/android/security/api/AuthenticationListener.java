package com.ibm.bms.clientsdk.android.security.api;

import android.content.Context;

import org.json.JSONObject;

public interface AuthenticationListener {
	void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context);
	void onAuthenticationSuccess(JSONObject info);
	void onAuthenticationFailure(JSONObject info);

}
