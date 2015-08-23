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

import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.challengehandlers.ChallengeHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashMap;

public class BMSClient {
	public final static String HTTP_SCHEME = "http";

	private final static String QUERY_PARAM_SUBZONE = "subzone";
	private final static String HTTPS_SCHEME = "https";
	private final static String BLUEMIX_NAME = "bluemix";
	private final static String BLUEMIX_DOMAIN = "bluemix.net";
	private final static String STAGE1_NAME = "stage1";

	private static BMSClient instance = null;
	private String backendRoute;
	private String backendGUID;
	private String subzone;
	private String rewriteDomain;

	private int defaultTimeout = 20000;

	private Context context = null;

	//TODO: change to challage hander
	private HashMap<String, ChallengeHandler> challengeHandlers = new HashMap<String, ChallengeHandler>();

	public static BMSClient getInstance() {
		if (instance == null) {
			instance = new BMSClient();

			MFPRequest.setup(); //Set up network interceptor to log network event times analytics.
		}
		return instance;
	}

	//TODO: make private constractor

	/**
	 * Sets the base URL for the authorization server.
	 * <p>
	 * This method should be called before you send the first request that requires authorization.
	 * </p>
	 * @param backendRoute Specifies the base URL for the authorization server
	 * @param backendGUID Specifies the GUID of the application
	 * @throws MalformedURLException if {@code backendRoute} could not be parsed as a URL.
	 */
	public void initialize(String backendRoute, String backendGUID) throws MalformedURLException {
		this.backendGUID = backendGUID;
		this.backendRoute = backendRoute;
		this.subzone = null;
		this.rewriteDomain = null;

		if (backendRoute != null) {
			URL url = new URL(backendRoute);

			String query = url.getQuery();
			if (query != null) {
				final String[] params = query.split("&");
				for (String param : params) {
					final String[] keyVal = param.split("=");

					if (keyVal.length < 2) {
						continue;
					}

					if (keyVal[0].equalsIgnoreCase(QUERY_PARAM_SUBZONE)) {
						this.subzone = keyVal[1];
						break;
					}
				}

				this.backendRoute = backendRoute.substring(0, backendRoute.length() - query.length() - 1);
			}
		}

		this.rewriteDomain = buildRewriteDomain();
	}

	public String getBackendRoute() {return backendRoute;}
	public String getBackendGUID() {return backendGUID;}

	public void registerAuthenticationListener(String realm, AuthenticationListener authenticationListener) {
		if (realm == null || realm.isEmpty()) {
			throw new InvalidParameterException("The realm name can't be null or empty.");
		}

		if (authenticationListener == null) {
			throw new InvalidParameterException("The authentication listener object can't be null.");
		}

		ChallengeHandler handler = new ChallengeHandler();
		handler.initialize(realm, authenticationListener);
		challengeHandlers.put(realm, handler);
	}

	public void unregisterAuthenticationListener(String realm) {
		if (realm != null && !realm.isEmpty()) {
			challengeHandlers.remove(realm);
		}
	}

	public ChallengeHandler getChallengeHandler(String realm) {return challengeHandlers.get(realm);}

	public int getDefaultTimeout() {return defaultTimeout;}
	public void setDefaultTimeout(int timeout) {defaultTimeout = timeout;}

	public String getRewriteDomain() {return rewriteDomain;}

	private String buildRewriteDomain() throws MalformedURLException {
		if (backendRoute == null || backendRoute.isEmpty()) {
			return null;
		}

		String applicationRoute = backendRoute;

		if (!applicationRoute.startsWith(HTTP_SCHEME)) {
			applicationRoute = String.format("%s://%s", HTTPS_SCHEME, applicationRoute);
		} else if (!applicationRoute.startsWith(HTTPS_SCHEME) && applicationRoute.contains(BLUEMIX_NAME)) {
			applicationRoute = applicationRoute.replace(HTTP_SCHEME, HTTPS_SCHEME);
		}

		URL url = new URL(applicationRoute);

		String host = url.getHost();
		String rewriteDomain;
		String regionInDomain = "ng";
		int port = url.getPort();

		String serviceUrl = String.format("%s://%s", url.getProtocol(), host);

		if (port != 0) {
			serviceUrl += ":" + String.valueOf(port);
		}

		String[] hostElements = host.split(".");

		if (!serviceUrl.contains(STAGE1_NAME)) {
			// Multi-region: myApp.eu-gb.mybluemix.net
			// US: myApp.mybluemix.net
			if (hostElements.length == 4) {
				regionInDomain = hostElements[hostElements.length - 3];
			}

			// this is production, because STAGE1 is not found
			// Multi-Region Eg: eu-gb.bluemix.net
			// US Eg: ng.bluemix.net
			rewriteDomain = String.format("%s.%s", regionInDomain, BLUEMIX_DOMAIN);
		} else {
			// Multi-region: myApp.stage1.eu-gb.mybluemix.net
			// US: myApp.stage1.mybluemix.net
			if (hostElements.length == 5) {
				regionInDomain = hostElements[hostElements.length - 3];
			}

			if (subzone != null && !subzone.isEmpty()) {
				// Multi-region Dev subzone Eg: stage1-Dev.eu-gb.bluemix.net
				// US Dev subzone Eg: stage1-Dev.ng.bluemix.net
				rewriteDomain = String.format("%s-%s.%s.%s", STAGE1_NAME, subzone, regionInDomain, BLUEMIX_DOMAIN);
			} else {
				// Multi-region Eg: stage1.eu-gb.bluemix.net
				// US  Eg: stage1.ng.bluemix.net
				rewriteDomain = String.format("%s.%s.%s", STAGE1_NAME, regionInDomain, BLUEMIX_DOMAIN);
			}
		}

		return rewriteDomain;
	}

	public void setApplicationContext(Context context){
		this.context = context;

		Logger.setContext(context);
	}
}
