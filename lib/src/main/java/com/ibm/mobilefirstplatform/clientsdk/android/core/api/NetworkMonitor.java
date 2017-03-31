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
import android.os.Build;
import android.telephony.TelephonyManager;


/**
 * <p>
 * Use the NetworkMonitor class to monitor the status of the Android device's connection to the internet.
 * </p>
 *
 * <p>
 * <b>Important: </b>Before using this class, make sure that your application has the required permissions.
 * At a minimum, the AndroidManifest.xml should contain the following permissions: android.permission.INTERNET
 * and android.permission.ACCESS_NETWORK_STATE. If you want to use {@link #getMobileNetworkType(Context)}, you need to
 * also include android.permission.READ_PHONE_STATE in your AndroidManifest.xml and obtain user permission
 * at runtime as described in the Android developer's guide for
 * <a href="https://developer.android.com/training/permissions/requesting.html">Requesting Permissions at Run Time</a>.
 * </p>
 *
 * <p>
 * To listen for network changes, pass a {@link NetworkConnectionChangeListener} in {@link #NetworkMonitor(Context, NetworkConnectionChangeListener)},
 * and use the {@link #startMonitoringNetworkChanges()} method. To turn off these notifications, call
 * {@link #stopMonitoringNetworkChanges()}.
 * </p>
 *
 * <p>
 * To get the type of network connection currently available, use {@link #getCurrentConnectionType(Context)}.
 * If this method returns {@link NetworkConnectionType#MOBILE}, you can narrow down the type further with
 * {@link #getMobileNetworkType(Context)} (only available on Android API 24 and higher),
 * which shows whether the device is using 4G, 3G, or 2G.
 * </p>
 */
public class NetworkMonitor {

    private Context context;
    private NetworkChangeReceiver networkReceiver;

    /**
     * The constructor for NetworkMonitor.
     *
     * @param context The Android application context
     * @param listener An optional network change listener
     */
    public NetworkMonitor(Context context, NetworkConnectionChangeListener listener) {
        this.context = context;
        if (listener != null) {
            this.networkReceiver = new NetworkChangeReceiver(listener);
        }
    }

    /**
     * Begin monitoring changes in the device's network connection.
     * Before using this method, be sure to pass a {@link NetworkConnectionChangeListener} to
     * {@link #NetworkMonitor(Context, NetworkConnectionChangeListener)}.
     */
    public void startMonitoringNetworkChanges() {
        if (networkReceiver != null) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(networkReceiver, filter);
        }
    }

    /**
     * Stop monitoring changes in the device's network connection.
     */
    public void stopMonitoringNetworkChanges() {
        if (networkReceiver != null) {
            context.unregisterReceiver(networkReceiver);
        }
    }

    /**
     * Get the type of mobile data network connection.
     * It is recommended to call {@link #getCurrentConnectionType(Context)} before this method to make sure
     * that the device does have a mobile data connection.
     *
     * <p><b>Note:</b> This method is only available for Android API 24 and up. When used on a lower API version,
     * this method will return "unknown".</p>
     *
     * @param context The Android application context
     * @return "4G", "3G", "2G", or "unknown"
     */
    @TargetApi(24)
    public String getMobileNetworkType(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

        String resultUnknown = "unknown";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return resultUnknown;
        }

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

    /**
     * Get the type of network connection that the device is currently using
     * to connect to the internet.
     *
     * @param context The Android application context
     * @return The type of network connection that the device is currently using.
     */
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

    /**
     * Check if the device currently has internet access.
     *
     * @param context The Android application context
     * @return Whether the device has internet access
     */
    public boolean isInternetAccessAvailable(Context context) {
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo(context);
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

    private NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }


    // Responds to changes in the device's network connection
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
