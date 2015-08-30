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

package com.ibm.mobilefirstplatform.clientsdk.android.security.internal;

import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by cirilla on 7/29/15.
 */
public class AuthorizationHeaderHelper {

    public static final String BEARER = "Bearer";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String REALM_IMF_AUTHENTICATION = "realm=\"imfAuthentication\"";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";


    /**
     * check if the params came from response that requires authorization
     * @param statusCode of the response
     * @param responseAuthorizationHeader 'WWW-Authenticate' header
     * @return true if status is 401 or 403 and The value of the header contains 'Bearer' AND 'realm="imfAuthentication"'
     */
    public static boolean isAuthorizationRequired(int statusCode, String responseAuthorizationHeader) {
        return isAuthorizationRequired(statusCode, Arrays.asList(responseAuthorizationHeader));
    }

    /**
     * A response is an OAuth error response only if,
     * 1. it's status is 401 or 403
     * 2. The value of the "WWW-Authenticate" header contains 'Bearer' AND 'realm="imfAuthentication"'
     *
     * @param response to check the conditions for.
     * @return true if the response satisfies both conditions
     */
    public static boolean isAuthorizationRequired(Response response) {
        return isAuthorizationRequired(response.code(), response.headers(WWW_AUTHENTICATE_HEADER));
    }

    /**
     * A response is an OAuth error response only if,
     * 1. it's status is 401 or 403
     * 2. The value of the "WWW-Authenticate" header contains 'Bearer' AND 'realm="imfAuthentication"'
     *
     * @param urlConnection connection to check the authorization conditions for.
     * @return true if the response satisfies both conditions
     * @throws IOException in case connection dosn't contains response code.
     */
    public static boolean isAuthorizationRequired(HttpURLConnection urlConnection) throws IOException {
        return isAuthorizationRequired(urlConnection.getResponseCode(),urlConnection.getHeaderField(WWW_AUTHENTICATE_HEADER));
    }

    /**
     * Adds the authorization header to the given URL connection object.
     * @param urlConnection The URL connection to add the header to.
     */
    public static void addAuthorizationHeader(URLConnection urlConnection, String header) {
        if (header != null) {
            urlConnection.setRequestProperty(AUTHORIZATION_HEADER, header);
        }
    }

    private static boolean isAuthorizationRequired(int statusCode, List<String> wwwAuthenticateHeaders) {

        if (statusCode == 401 || statusCode == 403) {
            
            //It is possible that there will be more then one header for this header-name. This is why we need the loop here.
            for (String header : wwwAuthenticateHeaders) {
                if (header.contains(BEARER) && header.contains(REALM_IMF_AUTHENTICATION)) {
                    return true;
                }
            }
        }

        return false;
    }
}
