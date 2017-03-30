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
 * Describes how the device is currently connected to the internet.
 *
 * <ul>
 * <li>{@link #WIFI}</li>
 * <li>{@link #MOBILE}</li>
 * <li>{@link #WIMAX}</li>
 * <li>{@link #ETHERNET}</li>
 * <li>{@link #NO_CONNECTION}</li>
 * </ul>
 */
public enum NetworkConnectionType {

    /**
     * The device is connected via Wifi.
     */
    WIFI,

    /**
     * The device is connected via mobile data network (4G, 3G, or 2G).
     */
    MOBILE,

    /**
     * The device is connected via WiMax.
     */
    WIMAX,

    /**
     * The device is connected via Ethernet.
     */
    ETHERNET,

    /**
     * The device is not connected to the internet.
     */
    NO_CONNECTION;


    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
