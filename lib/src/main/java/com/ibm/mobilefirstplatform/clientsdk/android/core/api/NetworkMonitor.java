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

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;


/**
 * Use the `NetworkMonitor` class to determine the current status of the Android device's connection to the internet.
 *
 * To get the type of network connection currently available, use {@link #getCurrentConnectionType(Context)}.
 * If this method returns {@link NetworkConnectionType#MOBILE}, you can narrow down the type further with
 * {@link #getMobileNetworkType(Context)}, which shows whether the device is using 4G, 3G, or 2G.
 *
 * To listen for network changes, pass a `NetworkConnectionChangeListener` in the `NetworkMonitor` constructor,
 * and use the {@link #startMonitoringNetworkChanges()} method. To turn off these notifications, use
 * {@link #stopMonitoringNetworkChanges()}.
 */
public class NetworkMonitor {

    private Context context;
    private NetworkChangeReceiver networkReceiver;

    public NetworkMonitor(Context context, NetworkConnectionChangeListener listener) {
        this.context = context;
        if (listener != null) {
            this.networkReceiver = new NetworkChangeReceiver(listener);
        }
    }

    public void startMonitoringNetworkChanges() {
        if (networkReceiver != null) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(networkReceiver, filter);
        }
    }

    public void stopMonitoringNetworkChanges() {
        if (networkReceiver != null) {
            context.unregisterReceiver(networkReceiver);
        }
    }

    @TargetApi(24)
    public String getMobileNetworkType(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

        String resultUnknown = "unknown";

        if (telephonyManager == null) {
            return resultUnknown;
        }
        switch (telephonyManager.getDataNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            default:
                return resultUnknown;
        }
    }

    // IMPORTANT: Call from background thread
    public NetworkConnectionType getCurrentConnectionType(Context context) {
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo(context);

        if (activeNetworkInfo == null || !isInternetAccessAvailable(context)) {
            return NetworkConnectionType.NO_CONNECTION;
        }
        switch (activeNetworkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return NetworkConnectionType.WIFI;
            case ConnectivityManager.TYPE_MOBILE:
                return NetworkConnectionType.MOBILE;
            case ConnectivityManager.TYPE_WIMAX:
                return NetworkConnectionType.WIMAX;
            case ConnectivityManager.TYPE_ETHERNET:
                return NetworkConnectionType.ETHERNET;
            default:
                return NetworkConnectionType.NO_CONNECTION;
        }
    }

    // IMPORTANT: Call from background thread
    public boolean isInternetAccessAvailable(Context context) {
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo(context);
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

    private NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }


    protected class NetworkChangeReceiver extends BroadcastReceiver {

        private NetworkConnectionChangeListener networkChangeListener;

        protected NetworkChangeReceiver(NetworkConnectionChangeListener listener) {
            super();
            this.networkChangeListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkConnectionType newConnectionType = getCurrentConnectionType(context);
            networkChangeListener.networkChanged(newConnectionType);
        }
    }
}
