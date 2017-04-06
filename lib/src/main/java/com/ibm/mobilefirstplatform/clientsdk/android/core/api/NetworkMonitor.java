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

import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;


/**
 * <p>
 * Use the NetworkMonitor class to monitor the status of the Android device's connection to the internet.
 * </p>
 *
 * <p>
 * <b>Important: </b>Before using this class, make sure that your application has the required permissions.
 * At a minimum, the AndroidManifest.xml should contain the following permissions: android.permission.INTERNET
 * and android.permission.ACCESS_NETWORK_STATE. If you want to use {@link #getMobileNetworkType()}, you need to
 * also include android.permission.READ_PHONE_STATE in your AndroidManifest.xml and obtain user permission
 * at runtime as described in the Android developer's guide for
 * <a href="https://developer.android.com/training/permissions/requesting.html">Requesting Permissions at Run Time</a>.
 * </p>
 *
 * <p>
 * To listen for network changes, pass a {@link NetworkConnectionListener} in {@link #NetworkMonitor(Context, NetworkConnectionListener)},
 * and use the {@link #startMonitoringNetworkChanges()} method. To turn off these notifications, call
 * {@link #stopMonitoringNetworkChanges()}.
 * </p>
 *
 * <p>
 * To get the type of network connection currently available, use {@link #getCurrentConnectionType()}.
 * If this method returns {@link NetworkConnectionType#MOBILE}, you can narrow down the type further with
 * {@link #getMobileNetworkType()} (only available on Android API 24 and higher),
 * which shows whether the device is using 4G, 3G, or 2G.
 * </p>
 */
public class NetworkMonitor {

    private static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + NetworkMonitor.class.getSimpleName());

    private Context context;
    private NetworkChangeReceiver networkReceiver;
    private TelephonyManager telephonyManager;

    /**
     * The constructor for NetworkMonitor.
     *
     * @param context The Android application context
     * @param listener An optional network change listener
     */
    public NetworkMonitor(Context context, NetworkConnectionListener listener) {
        this.context = context;
        if (listener != null) {
            this.networkReceiver = new NetworkChangeReceiver(listener);
        }
        this.telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Begin monitoring changes in the device's network connection.
     * Before using this method, be sure to pass a {@link NetworkConnectionListener} to
     * {@link #NetworkMonitor(Context, NetworkConnectionListener)}.
     */
    public void startMonitoringNetworkChanges() {
        if (networkReceiver != null) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(networkReceiver, filter);
        } else {
            logger.warn("Cannot monitor device network changes. Make sure that a NetworkConnectionListener was passed to the NetworkMonitor constructor.");
        }
    }

    /**
     * Stop monitoring changes in the device's network connection.
     */
    public void stopMonitoringNetworkChanges() {
        if (networkReceiver != null) {
            context.unregisterReceiver(networkReceiver);
        } else {
            logger.info("Cannot stop monitoring network changes because they were not being monitored.");
        }
    }

    /**
     * Get the type of mobile data network connection.
     * It is recommended to call {@link #getCurrentConnectionType()} before this method to make sure
     * that the device does have a mobile data connection.
     *
     * <p><b>Note:</b> This method is only available for Android API 24 and up. When used on a lower API version,
     * this method will return "unknown".</p>
     *
     * @return "4G", "3G", "2G", or "unknown"
     */
    @TargetApi(24)
    public String getMobileNetworkType() {
        String resultUnknown = "unknown";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return resultUnknown;
        }

        if (getTelephonyManager() == null) {
            return resultUnknown;
        }
        switch (getTelephonyManager().getDataNetworkType()) {
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
     * @return The type of network connection that the device is currently using.
     */
    public NetworkConnectionType getCurrentConnectionType() {
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo();

        if (activeNetworkInfo == null || !isInternetAccessAvailable()) {
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
     * @return Whether the device has internet access
     */
    public boolean isInternetAccessAvailable() {
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo();
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }


    protected TelephonyManager getTelephonyManager() {
        return this.telephonyManager;
    }

    protected NetworkChangeReceiver getNetworkReceiver() {
        return this.networkReceiver;
    }

    protected NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }


    // Responds to changes in the device's network connection
    protected class NetworkChangeReceiver extends BroadcastReceiver {

        private NetworkConnectionListener networkChangeListener;
        private NetworkConnectionType previousConnectionType;

        protected NetworkChangeReceiver(NetworkConnectionListener listener) {
            super();
            this.networkChangeListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkConnectionType newConnectionType = getCurrentConnectionType();
                if (!newConnectionType.equals(previousConnectionType)) {
                    networkChangeListener.networkChanged(newConnectionType);
                }
                previousConnectionType = newConnectionType;
            }
        }
    }
}
