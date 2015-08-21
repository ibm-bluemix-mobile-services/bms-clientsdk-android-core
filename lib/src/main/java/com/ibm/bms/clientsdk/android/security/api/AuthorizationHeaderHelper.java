package com.ibm.bms.clientsdk.android.security.api;

import com.squareup.okhttp.Response;

import org.apache.http.HttpRequest;

import java.net.URLConnection;
import java.util.List;

/**
 * Created by cirilla on 7/29/15.
 */
public class AuthorizationHeaderHelper {

    public static final String BEARER = "Bearer";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String REALM_IMF_AUTHENTICATION = "realm=\"imfAuthentication\"";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";


    static public boolean isAuthorizationRequired(int statusCode, String responseAuthorizationHeader) {
        return (statusCode == 401 || statusCode == 403) && isHeaderContainsAuthenticationData(responseAuthorizationHeader);
    }

    /**
     * A response is an OAuth error response only if,
     * 1. it's status is 401 or 403
     * 2. The value of the "WWW-Authenticate" header contains 'Bearer' AND 'realm="imfAuthentication"'
     *
     * @param response response to check the conditions for.
     * @return true if the response satisfies both conditions
     */
    static public boolean isAuthorizationRequired(Response response) {

        int status = response.code();
        if (status == 401 || status == 403) {
            List<String> wwwAuthenticateHeaders = response.headers(WWW_AUTHENTICATE_HEADER);

            //It is possible that there will be more then one header for this header-name. This is why we need the loop here.
            for (String headerValue : wwwAuthenticateHeaders) {
                if (isHeaderContainsAuthenticationData(headerValue)) {
                    return true;
                }
            }
        }

        return false;
    }


    static private boolean isHeaderContainsAuthenticationData(String header) {
        return header.contains(BEARER) && header.contains(REALM_IMF_AUTHENTICATION);
    }

    static public void addAuthorizationHeader(HttpRequest connection, String header) {
        //TODO: imp[lement for okHttp
    }

    /**
     * Adds the authorization header to the given URL connection object.
     *
     * @param urlConnection The URL connection to add the header to.
     */
    static public void addAuthorizationHeader(URLConnection urlConnection, String header) {
        if (header != null) {
            urlConnection.setRequestProperty(AUTHORIZATION_HEADER, header);
        }
    }

}
