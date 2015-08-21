package com.ibm.mobilefirstplatform.clientsdk.android.security.api;

import org.json.JSONObject;

/**
 * Created on 7/14/15.
 */
public interface AuthenticationContext {

	void submitAuthenticationChallengeAnswer(JSONObject answer);
	void submitAuthenticationChallengeSuccess();
	void submitAuthenticationChallengeFailure(JSONObject info);

}
