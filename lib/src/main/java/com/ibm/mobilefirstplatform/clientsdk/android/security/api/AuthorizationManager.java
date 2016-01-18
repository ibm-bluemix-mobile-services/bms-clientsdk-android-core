package com.ibm.mobilefirstplatform.clientsdk.android.security.api;

import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public interface AuthorizationManager {

	public static final String WWW_AUTHENTICATE_HEADER_NAME = "Www-Authenticate";

	public boolean isAuthorizationRequired(int statusCode, Map<String, List<String>> headers);
	public boolean isAuthorizationRequired(HttpURLConnection urlConnection) throws IOException;

	public void obtainAuthorization (Context context, ResponseListener listener, Object... params);
	public String getCachedAuthorizationHeader();

	public void clearAuthorizationData();
}

