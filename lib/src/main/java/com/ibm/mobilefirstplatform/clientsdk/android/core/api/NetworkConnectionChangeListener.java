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

import android.content.Context;


/**
 * <p>
 * The listener that will be called when the Android device's network connection type changes.
 * </p>
 *
 * <p>
 * To listen for network changes, pass this listener to {@link NetworkMonitor#NetworkMonitor(Context, NetworkConnectionChangeListener)},
 * and call {@link NetworkMonitor#startMonitoringNetworkChanges()}.
 * </p>
 */
public interface NetworkConnectionChangeListener {

    /**
     * Whenever the device's network connection changes to a different {@link NetworkConnectionType}, this method gets called.
     *
     * @param newConnection The new type of network connection.
     */
    void networkChanged(NetworkConnectionType newConnection);
}
