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

import org.json.JSONObject;

/**
 * The AuthenticationListener interface should be implemented by application's authentication listeners
 * in order to receive notifications about authentication challenges.
 */
public interface AuthenticationListener {
	/**
	 * Called when authentication challenge was received. The implementor should handle the challenge and call
	 * {@link com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationContext#submitAuthenticationChallengeAnswer(JSONObject)}
	 * with authentication challenge answer.
	 * @param authContext Authentication context the answer should be sent to
	 * @param challenge Information about authentication challenge.
	 * @param context A {@link Context} object that was passed to
	 * {@link com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResourceRequest#ResourceRequest(Context, String, String)}, which triggered the
	 * authentication challenge.
	 */
	void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context);

	/**
	 * Called when authentication succeeded.
	 * @param info Extended data describing the authentication success.
	 */
	void onAuthenticationSuccess(JSONObject info);

	/**
	 * Called when authentication fails.
	 * @param info Extended data describing authentication failure.
	 */
	void onAuthenticationFailure(JSONObject info);

}
