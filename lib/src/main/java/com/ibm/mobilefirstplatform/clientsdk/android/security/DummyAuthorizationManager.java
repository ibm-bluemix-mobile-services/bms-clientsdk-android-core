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

package com.ibm.mobilefirstplatform.clientsdk.android.security;

import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class DummyAuthorizationManager implements AuthorizationManager {

	@Override
	public boolean isAuthorizationRequired (int statusCode, Map<String, List<String>> headers) {
		return false;
	}

	@Override
	public boolean isAuthorizationRequired (HttpURLConnection urlConnection) throws IOException {
		return false;
	}

	@Override
	public void obtainAuthorization (Context context, ResponseListener listener, Object... params) {
		listener.onSuccess(null);
	}

	@Override
	public String getCachedAuthorizationHeader () {
		return null;
	}

	@Override
	public void clearAuthorizationData () {

	}
}
