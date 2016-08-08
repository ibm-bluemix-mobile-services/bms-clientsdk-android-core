IBM Bluemix Mobile Services - Client SDK Android Core
===

[![Build Status](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core.svg?branch=master)](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core)
[![Build Status](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core.svg?branch=development)](https://travis-ci.org/ibm-bluemix-mobile-services/bms-clientsdk-android-core)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ibm.mobilefirstplatform.clientsdk.android/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ibm.mobilefirstplatform.clientsdk.android/core)
[![Quality Gate](http://nemo.sonarqube.org/api/badges/gate?key=bms-clientsdk-android-core)](http://nemo.sonarqube.org/dashboard/index/bms-clientsdk-android-core) [![javadoc.io](https://javadoc-emblem.rhcloud.com/doc/com.ibm.mobilefirstplatform.clientsdk.android/core/badge.svg)](http://www.javadoc.io/doc/com.ibm.mobilefirstplatform.clientsdk.android/core)

This is the core component of Android SDK for [IBM® Bluemix® Mobile services] (https://console.ng.bluemix.net/docs/mobile/index.html).

###Installation
You can either download and import this package to your Android Studio project or get it via Gradle.

###Contents
This package contains the following APIs:
* HTTP Infrastructure
* Security and Authentication
* Logger
* Analytics

###Supported Levels
The package is supported on Android API level 14 and up (Android 4.0 and up).

###Change log

####2.0.2
* Fixed problem in the Security capabilities when getting the authorization header.

####2.0.0
* To use all the Analytics features, the new Analytics SDK will now need to be added as a dependency. Refer to https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-android-analytics for more details.
* Several APIs have been updated, and those would not be compatible with previous versions.
    * For example, BMSClient.initialize() now requires a region to be passed to it as well.

####1.2.0
* Added logout functionality. To use it call
  AuthorizationManager.getInstance().logout(getApplicationContext(), listener), where listener is to be called when the logout completes (you can also pass null instead).


####1.1.0
* Fixed send for Logger.
* Added the ability to hide this SDK's 'debug' and 'info' level logs from Logcat. To show them again, call Logger.setSDKInternalLoggingEnabled(true).
* Deprecated logger.getPackageName(), replaced by logger.getName().

####1.0.3
* Support relative paths when creating Requests.
* Fixed Analytics reporting.

####1.0.2
* Fixed issue with MCA.

####1.0.1
* Bug fixes.

####1.0.0
* Initial release


###License

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
