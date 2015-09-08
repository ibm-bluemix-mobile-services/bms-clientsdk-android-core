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

import org.json.JSONObject;

/**
 * ResponseListener is the interface that will be called after the ResourceRequest has completed or failed.
 */
public interface ResponseListener {

	/**
	 * This method will be called only when a response from the server has been received with a status
	 * in the 200 range.
	 * @param response the server response
	 */
	void onSuccess(Response response);

	/**
	 * This method will be called in the following cases:
	 * <ul>
	 * <li>There is no response from the server.</li>
	 * <li>The status from the server response is in the 400 or 500 ranges.</li>
	 * <li>There is an operational failure such as: authentication failure, data validation failure, or custom failure.</li>
	 * </ul>
	 * @param response Contains detail regarding why the Http request failed. May be null if the request did not reach the server
	 * @param t Exception that could have caused the request to fail. null if no Exception thrown.
	 * @param extendedInfo Contains details regarding operational failure. null if no operational failure occurred.
	 */

	void onFailure(Response response, Throwable t, JSONObject extendedInfo);

}
