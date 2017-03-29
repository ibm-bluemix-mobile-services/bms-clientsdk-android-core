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

package com.ibm.mobilefirstplatform.clientsdk.android.core.api;


/**
 * The listener that will be called when the Android device's network connection type changes.
 *
 * To listen for network changes, pass this listener to the `NetworkMonitor` constructor, and use
 *
 */
public interface NetworkConnectionChangeListener {

    void networkChanged(NetworkConnectionType newConnection);
}
