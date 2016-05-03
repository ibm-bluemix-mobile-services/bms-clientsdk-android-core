IBM Bluemix Mobile Services - Client SDK Android Core
===

This is the core component of Android SDK for IBM Bluemix Mobile Services. https://console.ng.bluemix.net/solutions/mobilefirst

###Installation
You can either download and import this package to your Android Studio project or get it via Gradle.

###Contents
This package contains the core components of Android SDK
* HTTP Infrastructure
* Security and Authentication
* Logger
* Analytics

###Supported Levels
The package is supported on Android API level 14 and up (Android 4.0 and up).

###Change log
####2.0.0
* To use all the Analytics features, the new Analytics SDK will now need to be added as a dependency. Refer to https://github.com/ibm-bluemix-mobile-services/bms-clientsdk-android-analytics for more details.
* Several APIs have been updated, and those would not be compatible with previous versions.
    * For example, BMSClient.initialize() now requires a region to be passed to it as well.

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
