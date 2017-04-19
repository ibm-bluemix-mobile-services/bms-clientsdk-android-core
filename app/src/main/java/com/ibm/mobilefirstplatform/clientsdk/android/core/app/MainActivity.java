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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.NetworkConnectionListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.NetworkConnectionType;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.NetworkMonitor;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ProgressListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api.MCAAuthorizationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Random;

import static android.provider.CalendarContract.CalendarCache.URI;


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

		Logger.setLogLevel(Logger.LEVEL.DEBUG);
		Logger.setSDKDebugLoggingEnabled(true);
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
		ResponseListener responseListener = new MyResponseListener();

		sendCustomUrlRequest(responseListener);
		sendAutoRetryRequest(responseListener);
		downloadImage(responseListener);
		uploadData(responseListener);
		uploadFile(responseListener);
		uploadText(responseListener);
	}

	private void sendCustomUrlRequest(ResponseListener responseListener) {
		Log.i("BMSCore", String.format("\n\nSending request to custom URL"));

		Request customUrlRequest = new Request(customResourceURL, Request.GET);
		customUrlRequest.send(getApplicationContext(), responseListener);
	}

	// Exercise auto-retries by trying to resend a request up to 5 times with a 504 (gateway timeout) response
	private void sendAutoRetryRequest(ResponseListener responseListener) {
		Log.i("BMSCore", String.format("\n\nSending request to 504 endpoint"));

		Request request504 = new Request("http://httpstat.us/504", Request.POST, 1000, 5);
		request504.send(getApplicationContext(), responseListener);
	}

	private void downloadImage(ResponseListener responseListener) {
		Log.i("BMSCore", String.format("\n\nDownloading an image"));

		// Large download
//		String url = "https://www.spacetelescope.org/static/archives/images/publicationtiff/heic1502a.tif";
		// Medium download
//		String url = "https://cdn.spacetelescope.org/archives/images/publicationjpg/heic1502a.jpg";
		// Small download
		String url = "https://cdn.spacetelescope.org/archives/images/screen/heic1502a.jpg";

		ProgressListener progressListener = new MyProgressListener(url);
		Request downloadRequest = new Request("https://cdn.spacetelescope.org/archives/images/publicationjpg/heic1502a.jpg", Request.GET);
		downloadRequest.download(getApplicationContext(), progressListener, responseListener);
	}

	private void uploadData(ResponseListener responseListener) {
		Log.i("BMSCore", String.format("\n\nUploading random data"));

		String url = "http://httpbin.org/post";

		ProgressListener progressListener = new MyProgressListener(url);

		Request dataUploadRequest = new Request(url, Request.POST);
		byte[] uploadData = new byte[1000000];
		new Random().nextBytes(uploadData);
		dataUploadRequest.upload(getApplicationContext(), uploadData, progressListener, responseListener);
	}

	private void uploadFile(ResponseListener responseListener) {
		Log.i("BMSCore", String.format("\n\nUploading andromeda image"));

		String url = "http://httpbin.org/post";

		ProgressListener progressListener = new MyProgressListener(url);

		File andromedaImage = new File(this.getFilesDir() + File.separator + "andromeda.jpg");
		try {
			InputStream imageInputStream = getResources().openRawResource(R.raw.andromeda);
			FileOutputStream fileOutputStream = new FileOutputStream(andromedaImage);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = imageInputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, length);
			}
			fileOutputStream.close();
			imageInputStream.close();
		}
		catch (IOException e) {
			Log.e("BMSCore", "Failed to load andromeda image into a file.");
		}

		Request imageUploadRequest = new Request(url, Request.POST);
		imageUploadRequest.addHeader("Content-Type", "image/jpg");
		imageUploadRequest.upload(getApplicationContext(), andromedaImage, progressListener, responseListener);
	}

	private void uploadText(ResponseListener responseListener) {
		Log.i("BMSCore", String.format("\n\nUploading some text"));

		String url = "http://httpbin.org/post";

		ProgressListener progressListener = new MyProgressListener(url);

		StringBuilder stringBuilder = new StringBuilder(3000000);
		for (int i = 0; i < 1000000; i++) {
			stringBuilder.append("ha ");
		}

		Request textUploadRequest = new Request(url, Request.POST);
		textUploadRequest.upload(getApplicationContext(), stringBuilder.toString(), progressListener, responseListener);
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
