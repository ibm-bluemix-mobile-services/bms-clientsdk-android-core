/*
 *     Copyright 2017 IBM Corp.
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

package com.ibm.mobilefirstplatform.clientsdk.android.core.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.NetworkConnectionListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.NetworkConnectionType;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.NetworkMonitor;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api.MCAAuthorizationManager;

import java.net.MalformedURLException;


public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

	private final static String backendURL = "http://9.148.225.153:9080"; // your BM application URL
	private final static String backendGUID = "vit1"; // the GUID you get from the dashboard
	private final static String customResourceURL = "http://9.148.225.153:3000/v1/apps/vit1/service"; // any protected resource
	private final static String customRealm = "customAuthRealm_1"; // auth realm

	private NetworkMonitor networkMonitor;
	private static final int MY_PERMISSIONS_READ_PHONE_STATE = 42;


	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		performInitializations();
		getNetworkInfo();
		sendSomeRequests();
	}

	// Initialize BMSClient and set the authorization manager
	private void performInitializations() {
		try {
			BMSClient.getInstance().initialize(getApplicationContext(), backendURL, backendGUID, BMSClient.REGION_US_SOUTH);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		MCAAuthorizationManager mcaAuthorizationManager = MCAAuthorizationManager.createInstance(this.getApplicationContext());
		mcaAuthorizationManager.registerAuthenticationListener(customRealm, new MyChallengeHandler());

		// to make the authorization happen next time
		mcaAuthorizationManager.clearAuthorizationData();
		BMSClient.getInstance().setAuthorizationManager(mcaAuthorizationManager);
	}

	// Exercise the NetworkMonitor API
	private void getNetworkInfo() {
		// Create a listener to check for new network connections (e.g. switching from mobile to Wifi, or losing internet connection)
		NetworkConnectionListener listener = new NetworkConnectionListener() {

			@Override
			public void networkChanged(NetworkConnectionType newConnection) {
				Log.i("BMSCore", "New network connection: " + newConnection.toString());
			}
		};

		// Initilize the network monitor with the application context and network change listener
		this.networkMonitor = new NetworkMonitor(getApplicationContext(), listener);

		// Start listening for network changes
		networkMonitor.startMonitoringNetworkChanges();

		// See if the device currently has internet access, and see what type of connection it is using
		Log.i("BMSCore", "Is connected to the internet: " + networkMonitor.isInternetAccessAvailable());
		Log.i("BMSCore", "Connection type: " + networkMonitor.getCurrentConnectionType().toString());

		// Check that the user has given permissions to read the phone's state.
		// If permission is granted, get the type of mobile data network being used.
		int networkPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
		if (networkPermission == PackageManager.PERMISSION_GRANTED) {
			Log.i("BMSCore", "Mobile network type: " + networkMonitor.getMobileNetworkType());
		}
		else {
			Log.i("BMSCore", "Obtaining permission to read phone state");
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
				// Asynchronously explain to the user why you are attempting to request this permission.
				// Once this is done, try to request the permission again.
			}
			else {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_READ_PHONE_STATE);
			}
		}
	}

	// Exercise the Request and Response APIs
	private void sendSomeRequests() {
		ResponseListener listener = new MyResponseListener();

		// Use custom URL defined at the top of this file
		Request customUrlRequest = new Request(customResourceURL, Request.GET);
		customUrlRequest.send(getApplicationContext(), listener);

		// Exercise auto-retries by trying to resend a request up to 5 times with a 504 (gateway timeout) response
		Request request504 = new Request("http://httpstat.us/504", Request.POST, 1000, 5);
		request504.send(getApplicationContext(), listener);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_READ_PHONE_STATE:
				if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
					if (networkMonitor != null) {
						Log.i("BMSCore", "Mobile network type: " + networkMonitor.getMobileNetworkType());
					}
				}
				else {
					Log.i("BMSCore", "Did not receive permission from user to read phone state");
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}
}
