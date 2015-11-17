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

package com.ibm.mobilefirstplatform.clientsdk.android.core.api;

import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.internal.BaseRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.AuthorizationRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.Utils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The BMSClient is a singleton that serves as the entry point to MobileFirst.
 */
public class BMSClient extends MFPClient {
    public final static String HTTP_SCHEME = "http";
    public final static String HTTPS_SCHEME = "https";

    private final static String QUERY_PARAM_SUBZONE = "subzone";

    private String backendRoute;
    private String backendGUID;
    private String rewriteDomain;

    private static Context appContext = null;  // set this to Application context and use it throughout

    /**
     * Should be called to obtain the instance of BMSClient.
     * @return the instance of BMSClient.
     */
    public static BMSClient getInstance() {
        if (instance == null) {
            instance = new BMSClient();

            BaseRequest.setup(); //Set up network interceptor to log network event times analytics for requests.
            AuthorizationRequest.setup(); //Set up network interceptor to log network event times analytics for authorization requests.
        }

        return (BMSClient)instance;
    }

    private BMSClient() {
    }

    /**
     * Sets the base URL for the authorization server.
     * <p>
     * This method should be called before you send the first request that requires authorization.
     * </p>
     * @param context Android application context
     * @param bluemixAppRoute Specifies the base URL for the authorization server
     * @param bluemixAppGUID Specifies the GUID of the application
     * @throws MalformedURLException {@code backendRoute} could not be parsed as a URL.
     */
    public void initialize(Context context, String bluemixAppRoute, String bluemixAppGUID) throws MalformedURLException {
        appContext = context.getApplicationContext();
        this.backendGUID = bluemixAppGUID;
        this.backendRoute = bluemixAppRoute;
        this.rewriteDomain = null;
        String subzone = null;

        if (backendRoute != null) {
            URL url = new URL(backendRoute);

            String query = url.getQuery();
            if (query != null) {
                subzone = Utils.getParameterValueFromQuery(query, QUERY_PARAM_SUBZONE);
                this.backendRoute = backendRoute.substring(0, backendRoute.length() - query.length() - 1);
            }
        }

        this.rewriteDomain = Utils.buildRewriteDomain(this.backendRoute, subzone);
        AuthorizationManager.createInstance(appContext);
        Logger.setContext(appContext);
    }

    /**
     *
     * @return backend route url
     */
    public String getBluemixAppRoute() {
        return backendRoute;
    }

    /**
     *
     * @return backend GUID
     */
    public String getBluemixAppGUID() {
        return backendGUID;
    }

    /**
     * @exclude
     * @return rewrite domain generated from backend route url.
     */
    public String getRewriteDomain() {
        return rewriteDomain;
    }

    /**
     *
     * @return the Android Application Context object
     */
    public static Context getAppContext() {
        return appContext;
    }
}
