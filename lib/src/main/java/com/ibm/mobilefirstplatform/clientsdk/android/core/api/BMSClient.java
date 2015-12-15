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

import com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.NetworkLoggingInterceptor;
import com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.internal.MetadataHeaderInterceptor;
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

	public final static String REGION_US_SOUTH = "ng.bluemix.net";
	public final static String REGION_UK = "eu-gb.bluemix.net";
	public final static String REGION_SYDNEY = "au-syd.bluemix.net";

    public final static String HTTP_SCHEME = "http";
    public final static String HTTPS_SCHEME = "https";

    private final static String QUERY_PARAM_SUBZONE = "subzone";

    private String backendRoute;
    private String backendGUID;
    private String rewriteDomain;
	private String bluemixRegionSuffix;
    private String defaultProtocol = HTTPS_SCHEME;

    /**
     * Should be called to obtain the instance of BMSClient.
     * @return the instance of BMSClient.
     */
    public static BMSClient getInstance() {
        if (instance == null) {
            instance = new BMSClient();

            //Set up network interceptor to log network event times analytics for authorization requests.
            AuthorizationRequest.setup();
        }

        return (BMSClient)instance;
    }

    private BMSClient() {
    }

    /**
     * Initializes the SDK with supplied parameters
     * <p>
     * This method should be called before you send the first request
     * </p>
     * @param context Android application context
     * @param bluemixAppRoute Specifies the base URL for the authorization server
     * @param bluemixAppGUID Specifies the GUID of the application
     * @throws MalformedURLException {@code backendRoute} could not be parsed as a URL.
	 * @deprecated in 1.2.0. Use initialize(Context context, String bluemixAppRoute, String bluemixAppGUID, String bluemixRegion) instead
     */
    public void initialize(Context context, String bluemixAppRoute, String bluemixAppGUID) throws MalformedURLException {
        Context appContext = context.getApplicationContext();
        this.backendGUID = bluemixAppGUID;
        this.backendRoute = bluemixAppRoute;
		this.bluemixRegionSuffix = null;
        this.rewriteDomain = null;
        String subzone = null;

        if (backendRoute != null) {
            backendRoute = removeTrailingSlashesFromURL(backendRoute);

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

        //Intercept requests to add metadata header
        BaseRequest.registerInterceptor(new MetadataHeaderInterceptor(appContext));

        //NOTE: Disabled for the moment. To be reenabled later.
        //Set up network interceptor to log network event times analytics for requests.
        //BaseRequest.registerInterceptor(new NetworkLoggingInterceptor());
    }

    private String removeTrailingSlashesFromURL(String url) {
        if(url.trim().substring(url.length()-1).equalsIgnoreCase("/")){
            return url.trim().substring(0, url.length()-1);
        }

        return url;
    }

	/**
	 * Initializes the SDK with supplied parameters
	 * <p>
	 * This method should be called before you send the first request
	 * </p>
	 * @param context Android application context
	 * @param bluemixAppRoute Specifies the base URL for the authorization server
	 * @param bluemixAppGUID Specifies the GUID of the application
	 * @param bluemixRegion Specifies the Bluemix deployment to use. Use values in BMSClient.REGION* static props
	 * @throws MalformedURLException {@code backendRoute} could not be parsed as a URL.
	 */
	public void initialize(Context context, String bluemixAppRoute, String bluemixAppGUID, String bluemixRegion) throws MalformedURLException{
		this.backendGUID = bluemixAppGUID;
		this.backendRoute = bluemixAppRoute;
		this.bluemixRegionSuffix = bluemixRegion;
		this.rewriteDomain = null;
		AuthorizationManager.createInstance(context.getApplicationContext());
		Logger.setContext(context.getApplicationContext());

        //Intercept requests to add metadata header
        BaseRequest.registerInterceptor(new MetadataHeaderInterceptor(context.getApplicationContext()));

        //NOTE: Disabled for the moment. To be reenabled later.
        //Set up network interceptor to log network event times analytics for requests.
        //BaseRequest.registerInterceptor(new NetworkLoggingInterceptor());
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
	 * @exclude
	 * @return Bluemix region suffix for SDK components to build URLs
	 */
	public String getBluemixRegionSuffix(){ return bluemixRegionSuffix;}

    /**
     * @exclude
     * @return Bluemix region suffix for SDK components to build URLs
     */
    public String getDefaultProtocol(){ return defaultProtocol;}

    /**
     * @exclude
     * @return Bluemix region suffix for SDK components to build URLs
     */
    public void setDefaultProtocol(String protocol){ defaultProtocol = protocol;}

}
