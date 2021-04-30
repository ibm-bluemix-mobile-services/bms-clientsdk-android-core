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

import com.ibm.mobilefirstplatform.clientsdk.android.core.internal.AbstractClient;
import com.ibm.mobilefirstplatform.clientsdk.android.security.DummyAuthorizationManager;

import java.net.CookiePolicy;
import java.net.MalformedURLException;


/**
 * The BMSClient is a singleton that serves as the entry point to MobileFirst.
 */
public class BMSClient extends AbstractClient {
	
    public final static String REGION_US_SOUTH = ".ng.bluemix.net";
    public final static String REGION_UK = ".eu-gb.bluemix.net";
    public final static String REGION_SYDNEY = ".au-syd.bluemix.net";
    public final static String REGION_GERMANY = ".eu-de.bluemix.net";
    public final static String REGION_US_EAST = ".us-east.bluemix.net";
    public final static String REGION_TOKYO = ".jp-tok.bluemix.net";

    public final static String HTTP_SCHEME = "http";
    public final static String HTTPS_SCHEME = "https";
	
    private String backendRoute = null;
    private String backendGUID = null;
    private String bluemixRegionSuffix = null;
    private String defaultProtocol = HTTPS_SCHEME;

    protected static AbstractClient instance = null;

    /**
     * Should be called to obtain the instance of BMSClient.
     * @return the instance of BMSClient.
     */
    public static BMSClient getInstance() {
        if (instance == null) {
            instance = new BMSClient();
		}

        return (BMSClient)instance;
    }

    private BMSClient() {
    }

	/**
	 * @deprecated As of release 2.2.0, replaced by {@link #initialize(Context, String)}
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
        if (null == this.authorizationManager) {
            this.authorizationManager = new DummyAuthorizationManager(context);
        }
		Request.setCookieManager(cookieManager);
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
	}

	/**
	 * Initializes the SDK with supplied parameters.
	 * <p>
	 * This method should be called before you send the first request
	 * </p>
	 * @param context Android application context
	 * @param bluemixRegion Specifies the Bluemix region to use. Use values in BMSClient.REGION* static props.
	 * @throws MalformedURLException {@code backendRoute} could not be parsed as a URL.
	 */
	public void initialize(Context context, String bluemixRegion){
		this.bluemixRegionSuffix = bluemixRegion; // Change this if we ever support retries with multiple regions
        if (null == this.authorizationManager) {
            this.authorizationManager = new DummyAuthorizationManager(context);
        }
		Request.setCookieManager(cookieManager);
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
	}

    /**
     * @deprecated As of release 2.2.0. if you use the new initialize methoud this function return null.
     * Will be removed as release 3.x
     * @return backend route url
     */
    public String getBluemixAppRoute() {
        return backendRoute;
    }

    /**
     * @deprecated As of release 2.2.0. if you use the new initialize methoud this function return null.
     * Will be removed as release 3.x
     * @return backend GUID
     */
    public String getBluemixAppGUID() {
        return backendGUID;
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
