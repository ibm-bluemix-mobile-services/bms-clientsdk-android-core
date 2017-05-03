# Change Log

`BMSCore` adheres to [Semantic Versioning](http://semver.org/).

### 3.0.0
* <b>Auto-retries: </b>The `Request` class has a new constructor parameter to optionally specify the number of times to retry any requests made with that object if the request fails to receive a response (or receives a 504 status).
* <b>Uploading and Downloading: </b>The `Request` class has several new methods for `upload()` and `download()` as alternatives to `send()` for large data transfers. By passing a `ProgressListener` to these methods, the progress of the upload or download can be monitored.
* <b>Network connection monitoring: </b>There is a new API, `NetworkMonitor`, that can be used to determine the current type of network connection available to the Android device, as well as monitor changes in network connection with a `NetworkConnectionListener`.
* <b>Response: </b>The `Response` class includes new methods to retrive response information, including `getRequestURL()`, `getResponseJSON()`, `getResponseByteStream()`, and `getContentLength()`.
* Changed `getResponseText()` and `getResponseJSON()` from the `Response` class to return `null` instead of throwing an exception if the response body cannot be parsed into the relevant data type.

### 2.2.7
* Fixed issue when sending a Request with a byte array body, where it would throw a `NullPointerException` when trying to read the content type.

### 2.2.6
* Fixed a logout issue when working with Liberty with a TAI backend.

### 2.2.5
* Fixed issue where if no content type was specified, sending binary data would fail with a `NullPointerException`.
* Fixed issue where if there was a trailing slash in the URL when creating a request, sending the request would sometimes have issues.

### 2.2.4
* Fixed an issue with the new initializer; signature remains the same.

#### 2.2.3
* Fixed conditional logic error when sending requests.

### 2.2.2
* Any request using the HEAD HTTP verb would fail, due to the Request class adding a body. This is fixed, and now HEAD requests are sent without a body.

### 2.2.1
* Android Nougat officially supported; changed the target SDK version to 24.

### 2.2.0
* Added a new initializer for BMSClient that does not require the app route and app guid: `BMSClient.initialize(context, BMSClient.REGION_US_SOUTH);`
* The old initializer has now been deprecated and will be removed on the next major version (3.x). Note that this initializer has no checked exceptions, unlike the old one.

### 2.1.0
* Added MCAAuthorizationManager create methods with tenantId and region parameters, in order to be able to support service keys

### 2.0.2
* Fixed problem in the Security capabilities when getting the authorization header.

### 2.0.0
* To use all the Analytics features, the new Analytics SDK will now need to be added as a dependency. Refer to https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-android-analytics for more details.
* Several APIs have been updated, and those would not be compatible with previous versions.
    * For example, BMSClient.initialize() now requires a region to be passed to it as well.

### 1.2.0
* Added logout functionality. To use it call
  AuthorizationManager.getInstance().logout(getApplicationContext(), listener), where listener is to be called when the logout completes (you can also pass null instead).

### 1.1.0
* Fixed send for Logger.
* Added the ability to hide this SDK's 'debug' and 'info' level logs from Logcat. To show them again, call Logger.setSDKInternalLoggingEnabled(true).
* Deprecated logger.getPackageName(), replaced by logger.getName().

### 1.0.3
* Support relative paths when creating Requests.
* Fixed Analytics reporting.

### 1.0.2
* Fixed issue with MCA.

### 1.0.1
* Bug fixes.

### 1.0.0
* Initial release
