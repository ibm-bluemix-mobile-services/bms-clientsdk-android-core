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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.internal.BaseRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;

import org.json.JSONObject;

/**
 * Created on 7/14/15.
 */

/**
 * The AuthenticationContext interface is implemented by SDK.
 * The context is passed to a custom authentication listener and should be called back to provide
 * the internal challenge handler with authentication challenge answer. The challenge answer may contain
 * user credentials to be sent to the server.
 */
public interface AuthenticationContext {
	/**
	 * Submits authentication challenge response.
	 * @param answer JSON with challenge response.
	 */
	void submitAuthenticationChallengeAnswer(JSONObject answer);

	/**
	 * Informs about authentication success.
	 */
	void submitAuthenticationSuccess ();

	/**
	 * Informs about authentication failure. This function must be called from a custom challenge
	 * handler when the authorization request should be canceled for any reason (for example,
	 * when user clicks 'cancel' on login dialog). The original {@link BaseRequest}
	 * will be failed.
	 * @param info Extended information about the failure. It will be passed to {@link com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener#onFailure(Response, Throwable, JSONObject)} of
	 *             the resource request as 'extendedInfo' object.
	 */
	void submitAuthenticationFailure (JSONObject info);

}
