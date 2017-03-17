IBM Bluemix Mobile Services - Client SDK Android Core
===

[![Build Status](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core.svg?branch=master)](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core)
[![Build Status](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core.svg?branch=development)](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ibm.mobilefirstplatform.clientsdk.android/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ibm.mobilefirstplatform.clientsdk.android/core)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/33762c419c1a4743a0348c93686acb1c)](https://www.codacy.com/app/ibm-bluemix-mobile-services/bms-clientsdk-android-core?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ibm-bluemix-mobile-services/bms-clientsdk-android-core&amp;utm_campaign=Badge_Grade)
[![Coverage Status](https://coveralls.io/repos/github/ibm-bluemix-mobile-services/bms-clientsdk-android-core/badge.svg?branch=code-coverage)](https://coveralls.io/github/ibm-bluemix-mobile-services/bms-clientsdk-android-core?branch=code-coverage)
[![javadoc.io](https://javadoc-emblem.rhcloud.com/doc/com.ibm.mobilefirstplatform.clientsdk.android/core/badge.svg)](http://www.javadoc.io/doc/com.ibm.mobilefirstplatform.clientsdk.android/core)


This is the core component of Android SDK for [IBM® Bluemix® Mobile services] (https://console.ng.bluemix.net/docs/mobile/index.html).

### Installation
You can either download and import this package to your Android Studio project or get it via Gradle.

### Using the SDK
Before doing anything, first initialize the BMS Core SDK by calling `BMSClient.initialize`:

```
    BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_US_SOUTH); //Replace the region with the Bluemix region you are using.
```

### Contents
This package contains the following APIs:
* HTTP Infrastructure
* Security and Authentication
* Logger
* Analytics

### Supported Levels
The package is supported on Android API level 15 and up (Android 4.0.3 and up).

### Change log

#### 2.2.7
* Fixed issue when sending a Request with a byte array body, where it would throw a `NullPointerException` when trying to read the content type.

#### 2.2.6
* Fixed a logout issue when working with Liberty with a TAI backend.

#### 2.2.5
* Fixed issue where if no content type was specified, sending binary data would fail with a `NullPointerException`.
* Fixed issue where if there was a trailing slash in the URL when creating a request, sending the request would sometimes have issues.

#### 2.2.4
* Fixed an issue with the new initializer; signature remains the same.

#### 2.2.3
* Fixed conditional logic error when sending requests.

#### 2.2.2
* Any request using the HEAD HTTP verb would fail, due to the Request class adding a body. This is fixed, and now HEAD requests are sent without a body.

#### 2.2.1
* Android Nougat officially supported; changed the target SDK version to 24.

#### 2.2.0
* Added a new initializer for BMSClient that does not require the app route and app guid: `BMSClient.initialize(context, BMSClient.REGION_US_SOUTH);` 
* The old initializer has now been deprecated and will be removed on the next major version (3.x). Note that this initializer has no checked exceptions, unlike the old one.

#### 2.1.0
* Added MCAAuthorizationManager create methods with tenantId and region parameters, in order to be able to support service keys

#### 2.0.2
* Fixed problem in the Security capabilities when getting the authorization header.

#### 2.0.0
* To use all the Analytics features, the new Analytics SDK will now need to be added as a dependency. Refer to https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-android-analytics for more details.
* Several APIs have been updated, and those would not be compatible with previous versions.
    * For example, BMSClient.initialize() now requires a region to be passed to it as well.

#### 1.2.0
* Added logout functionality. To use it call
  AuthorizationManager.getInstance().logout(getApplicationContext(), listener), where listener is to be called when the logout completes (you can also pass null instead).


#### 1.1.0
* Fixed send for Logger.
* Added the ability to hide this SDK's 'debug' and 'info' level logs from Logcat. To show them again, call Logger.setSDKInternalLoggingEnabled(true).
* Deprecated logger.getPackageName(), replaced by logger.getName().

#### 1.0.3
* Support relative paths when creating Requests.
* Fixed Analytics reporting.

#### 1.0.2
* Fixed issue with MCA.

#### 1.0.1
* Bug fixes.

#### 1.0.0
* Initial release


### License

Copyright 2015 IBM Corp.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
