BMSCore
===

[![Build Status](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core.svg?branch=master)](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/33762c419c1a4743a0348c93686acb1c)](https://www.codacy.com/app/ibm-bluemix-mobile-services/bms-clientsdk-android-core?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ibm-bluemix-mobile-services/bms-clientsdk-android-core&amp;utm_campaign=Badge_Grade)
[![Coverage Status](https://coveralls.io/repos/github/ibm-bluemix-mobile-services/bms-clientsdk-android-core/badge.svg?branch=development)](https://coveralls.io/github/ibm-bluemix-mobile-services/bms-clientsdk-android-core?branch=development)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ibm.mobilefirstplatform.clientsdk.android/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ibm.mobilefirstplatform.clientsdk.android/core)
[![](https://img.shields.io/badge/bluemix-powered-blue.svg)](https://bluemix.net)


BMSCore is the core component of the Android SDKs for [IBM® Bluemix® Mobile services](https://console.ng.bluemix.net/docs/cloudnative/sdk.html#sdk).



## Table of Contents
* [Summary](#summary)
* [Requirements](#requirements)
* [Installation](#installation)
* [Example Usage](#example-usage)
* [Release Notes](https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-android-core/blob/master/CHANGELOG.md)
* [License](#license)



## Summary

BMSCore provides the HTTP infrastructure that the other Bluemix Mobile Services (BMS) client SDKs use to communicate with their corresponding Bluemix services. These other SDKs include [BMSAnalytics](https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-android-analytics), [BMSPush](https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-android-push), [FacebookAuthentication](https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-android-security-facebookauthentication), and [GoogleAuthentication](https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-android-security-googleauthentication).

You can use this SDK to make network requests to any resource using `Request`. The `Request` class can be used for typical network requests as well as uploading and downloading large amounts of data with the option to monitor the upload/download progress. BMSCore also provides a `NetworkMonitor` API that can detect and monitor changes in the type of network connection that is available to the Android device.

BMSCore is also available for [iOS/watchOS](https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-swift-core) and [Cordova](https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-cordova-plugin-core).



## Requirements

* Android API 15+
* Android 4.0.3+



## Installation

Add the following line to your app's build.gradle (substituting the version for the one you want):

```gradle
compile 'com.ibm.mobilefirstplatform.clientsdk.android:core:3.0.0'
```



## Example Usage

* [Import the library](#import-the-library)
* [Initialize the client](#initialize-the-client)
* [Monitor the network connection](#monitor-the-network-connection)
* [Make network requests](#make-network-requests)

> View the complete API reference [here](https://www.javadoc.io/doc/com.ibm.mobilefirstplatform.clientsdk.android/core).

---

### Import the library

```Java
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.*;
```

---

### Initialize the client

Before using BMSCore, first initialize the `BMSClient`:

```Java
    BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_US_SOUTH); // Replace the region with the Bluemix region you are using
```

---

### Monitor the network connection

With the `NetworkMonitor` API, you can monitor the status of the Android device's connection to the internet. You can use this information to decide when to send network requests and to handle offline or slow network conditions.

Before using this API, ensure that the AndroidManifest.xml contains the following permissions: `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`.

(Optional) Create a `NetworkConnectionListener` to get notified of network connection changes.

```Java
NetworkConnectionListener networkListener = new NetworkConnectionListener() {
    @Override
    public void networkChanged(NetworkConnectionType newConnection) {
        Log.i("MyApp", "Network connection changed to " + newConnection.toString());
    }
};
```

Next, create a new instance of the `NetworkMonitor`. Only one instance is needed per app. If you do not want to use a `NetworkConnectionListener`, use `null` for the second parameter.

```Java
NetworkMonitor networkMonitor = new NetworkMonitor(getApplicationContext(), networkListener);
```

For the `NetworkConnectionListener` to start or stop receiving network change broadcasts, use the following methods.

```Java
networkMonitor.startMonitoringNetworkChanges();
networkMonitor.stopMonitoringNetworkChanges();
```

To get the current type of network connection (WiFi, mobile data, no connection, etc.), use `networkMonitor.getCurrentConnectionType()`.

If the device has a mobile data enabled, you can see whether they have access to 4G, 3G, or 2G with `networkMonitor.getMobileNetworkType()`. **Important**: this method requires the `android.permission.ACCESS_NETWORK_STATE` permission in the AndroidManifest.xml, as well as obtaining user permission at runtime, as described in the Android developer's guide for <a href="https://developer.android.com/training/permissions/requesting.html">Requesting Permissions at Run Time</a>.

---

### Make network requests

First, create a new `Request` with the URL and an HTTP verb (and, optionally, a timeout).

```Java
String resourceURL = "http://httpbin.org/GET";
int timeout = 500; // milliseconds
Request request = new Request(resourceURL, Request.GET, timeout);
```

You can also add headers and query parameters as follows.

```Java
Map<String, String> queryParameters = new HashMap<>();
queryParameters.put("key", "value");
request.setQueryParameters(queryParameters);
request.addHeader("Content-Type", "text/plain");
```

Define a `ResponseListener`.

```Java
class MyResponseListener implements ResponseListener {

    @Override
    public void onSuccess(Response response) {
        if (response != null) {
            Log.i("MyApp", "Response status: " + response.getStatus());
            Log.i("MyApp", "Response headers: " + response.getHeaders());
            Log.i("MyApp", "Response body: " + response.getResponseText());
        }
    }

    @Override
    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
        if (response != null) {
            Log.i("MyApp", "Response status: " + response.getStatus());
            Log.i("MyApp", "Response body: " + response.getResponseText());
        }
        if (t != null && t.getMessage() != null) {
            Log.i("MyApp", "Error: " + t.getMessage());
        }
    }
}
```

Finally, send the request with the request body and `ResponseListener`.

```Java
String requestBody = "Request body text";
ResponseListener responseListener = new MyResponseListener();
request.send(getApplicationContext(), requestBody, responseListener);
```


#### Monitoring upload/download progress

Network requests can alternatively be sent using one of the `upload()` or `download()` methods. These work the same way as `send()` while providing an additional listener for monitoring the progress of the upload or download as it proceeds.

To use one of these methods, first define a `ProgressListener`.

```Java
class MyProgressListener implements ProgressListener {

    @Override
    public void onProgress(long bytesSoFar, long totalBytesToSend) {
        double progress = (double)bytesSoFar / (double)(totalBytesToSend) * 100;
        Log.i("MyApp", String.format("Progress: %.1f%%", progress));
    }
}
```

Then download.

```Java
ProgressListener progressListener = new MyProgressListener();
ResponseListener responseListener = new MyResponseListener();

String url = "https://cdn.spacetelescope.org/archives/images/screen/heic1502a.jpg";
Request request = new Request(url, Request.GET);
request.download(getApplicationContext(), progressListener, responseListener);
```

Or upload.
```Java
ProgressListener progressListener = new MyProgressListener();
ResponseListener responseListener = new MyResponseListener();

byte[] uploadData = new byte[1000000];
new Random().nextBytes(uploadData);

String url = "http://httpbin.org/post";
Request request = new Request(url, Request.POST);
request.upload(getApplicationContext(), uploadData, progressListener, responseListener);
```


---

#### Automatically resend requests

There is a `Request` constructor that accepts an `autoRetries` parameter. This specifies the number of times that the request will be automatically resent if it fails. These automatic retries occur if no response was received (possibly due to a lost network connection), the request timed out, or a 504 (gateway timeout) response was received.

```Java
Request request = new Request("www.example.com, Request.GET, 500, 3); // Automatically retry the request up to 3 times
```

If a `Request` is created without the `autoRetries` parameter, no automatic retries will occur.


---


## License

Copyright 2017 IBM Corp.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
