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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public interface AuthorizationManager {

	String WWW_AUTHENTICATE_HEADER_NAME = "Www-Authenticate";

	/**
	 * @return Whether authorization is required
	 * @param headers http response headers
	 * @param statusCode http response status code
	 */
	boolean isAuthorizationRequired(int statusCode, Map<String, List<String>> headers);

	/**
	 * @return Whether authorization is required
	 * @param urlConnection HttpURLConnection representing http response
	 */
	boolean isAuthorizationRequired(HttpURLConnection urlConnection) throws IOException;

	/**
	 * Starts authorization process
	 * @param context Context for obtaining authorization. Should be Activity if authorization in interactive
	 */
	void obtainAuthorization (Context context, ResponseListener listener, Object... params);

	/**
	 * Returns previously obtained authorization header. The value will be added to all outgoing requests
	 * as Authorization header.
	 * @return cached authorization header
	 */
	String getCachedAuthorizationHeader();

	/**
	 * Clears authorization data
	 */
	void clearAuthorizationData();

	/**
	 * @return UserIdentity object
	 */
	UserIdentity getUserIdentity();

	/**
	 * @return DeviceIdentity object
	 */
	DeviceIdentity getDeviceIdentity();

	/**
	 * @return AppIdentity object
	 */
	AppIdentity getAppIdentity();

	/**
	 * logs out user
	 * @param context Android Activity that will handle the authorization (like facebook or google)
	 * @param listener Response listener
	 */
	void logout(Context context, ResponseListener listener);

}

