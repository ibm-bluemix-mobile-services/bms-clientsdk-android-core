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

package com.ibm.mobilefirstplatform.clientsdk.android.core.internal;

import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import java.net.CookieManager;


public abstract class AbstractClient {

    protected static final CookieManager cookieManager = new CookieManager();

	protected AuthorizationManager authorizationManager = null;
    private int defaultTimeout = 20000;

	/**
	 * Gets active authorization manager.
	 *
	 * @return AuthorizationManager
	 */
	public AuthorizationManager getAuthorizationManager(){
		return authorizationManager;
	}

	/**
	 * Sets active authorization manager.
	 *
	 * @param authorizationManager    authorization manager.
	 */
	public void setAuthorizationManager(AuthorizationManager authorizationManager){
		this.authorizationManager = authorizationManager;
	}

    /**
     * @return default timeout, in milliseconds
     */
    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    /**
     * Sets the default timeout. The SDK's default value is 20000 milliseconds.
     *
     * @param timeout specifies the new timeout, in milliseconds
     */
    public void setDefaultTimeout(int timeout) {
        defaultTimeout = timeout;
    }

    /**
     * @return cookieManager cookie manager
     */
    public CookieManager getCookieManager(){
        return cookieManager;
    }


}
