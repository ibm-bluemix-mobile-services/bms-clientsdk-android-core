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

package com.ibm.bms.clientsdk.android.security.internal.security;


import android.test.InstrumentationTestCase;

import com.ibm.bms.clientsdk.android.security.api.AuthorizationManager;
import com.ibm.bms.clientsdk.android.security.api.AuthorizationManagerPreferences;

import junit.framework.Assert;

import org.json.JSONObject;

public class AuthorizationPreferencesTests extends InstrumentationTestCase {


	AuthorizationManagerPreferences preferences;

	final String SAVE_STRING = "Test_String_To_Save";
	JSONObject saveJSON;//

	@Override
	public void setUp() throws Exception {
		super.setUp();

		saveJSON = new JSONObject("{\"TestName\":\"TestValue\"}");
		preferences = new AuthorizationManagerPreferences(getInstrumentation().getTargetContext());
	}

	public void testStringSaving() throws Exception {
		preferences.clientId.set(SAVE_STRING);

		AuthorizationManagerPreferences preferences2 = new AuthorizationManagerPreferences(getInstrumentation().getTargetContext());
		Assert.assertEquals(SAVE_STRING, preferences2.clientId.get());
	}


	public void testJSONSaving() throws Exception {

		preferences.userIdentity.set(saveJSON);

		AuthorizationManagerPreferences preferences3 = new AuthorizationManagerPreferences(getInstrumentation().getTargetContext());
		Assert.assertEquals("TestValue", preferences3.userIdentity.getAsJSON().getString("TestName"));
	}


	//in this test we saving the values and thet are saved in runtime and on local storage
	public void testTokenSavingWithAlwaysPolicy() throws Exception {

		preferences.persistencePolicy.set(AuthorizationManager.PersistencePolicy.ALWAYS);

		preferences.accessToken.set(SAVE_STRING);
		preferences.idToken.set(SAVE_STRING);

		//check that the values are set in runtime
		Assert.assertEquals(SAVE_STRING, preferences.accessToken.get());
		Assert.assertEquals(SAVE_STRING, preferences.idToken.get());

		AuthorizationManagerPreferences preferences4 = new AuthorizationManagerPreferences(getInstrumentation().getTargetContext());

		Assert.assertEquals(SAVE_STRING, preferences4.accessToken.get());
		Assert.assertEquals(SAVE_STRING, preferences4.idToken.get());
	}


	//in this test we saving the values but they are not saved on disk, but only in runtime
	public void testTokenSavingWithNeverPolicy() throws Exception {

		preferences.persistencePolicy.set(AuthorizationManager.PersistencePolicy.NEVER);

		preferences.accessToken.set(SAVE_STRING);
		preferences.idToken.set(SAVE_STRING);

		//check that the values are set in runtime
		Assert.assertEquals(SAVE_STRING, preferences.accessToken.get());
		Assert.assertEquals(SAVE_STRING, preferences.idToken.get());

		AuthorizationManagerPreferences preferences5 = new AuthorizationManagerPreferences(getInstrumentation().getTargetContext());

		//check that the values were not saved on local storage
		Assert.assertNull(preferences5.accessToken.get());
		Assert.assertNull(preferences5.idToken.get());
	}



}