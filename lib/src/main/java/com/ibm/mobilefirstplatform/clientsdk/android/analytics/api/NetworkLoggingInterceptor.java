/*
 *     Copyright 2015 IBM Corp.
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

package com.ibm.mobilefirstplatform.clientsdk.android.analytics.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

public class NetworkLoggingInterceptor implements Interceptor{
    @Override public com.squareup.okhttp.Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        Logger logger = Logger.getInstance(Logger.INTERNAL_PREFIX + "analytics");

        logger.analytics("BaseRequest outbound", null);

        long t1 = System.currentTimeMillis();

        String trackingid = UUID.randomUUID().toString();

        Request requestWithHeaders = request.newBuilder()
                .header("x-wl-analytics-tracking-id", trackingid)
                .header("x-mfp-analytics-metadata", generateAnalyticsMetadataHeader())
                .build();

        com.squareup.okhttp.Response response = chain.proceed(requestWithHeaders);

        long t2 = System.currentTimeMillis();

        try {
            JSONObject metadata = new JSONObject();
            metadata.put("$url", request.urlString());
            metadata.put("$category", "network");
            metadata.put("$trackingid", trackingid);
            metadata.put("$outboundTimestamp", t1);
            metadata.put("$inboundTimestamp", t2);
            metadata.put("$duration", t2- t1);

            if(response != null){
                metadata.put("$statusCode", response.code());
            }

            if(response != null && response.body() != null && response.body().contentLength() >= 0){
                metadata.put("$bytesReceived", response.body().contentLength());
            }

            logger.analytics("BaseRequest inbound", metadata);
        } catch (JSONException e) {
            //Do nothing, since it is just for analytics.
        }

        return response;
    }

    private String generateAnalyticsMetadataHeader() {
        // add required analytics headers:
        JSONObject metadataHeader = new JSONObject();
        try {

            Context context = BMSClient.getAppContext();  // we assume the developer has called BMSClient.getInstance at least once by this point, so context is not

            // we try to keep the keys short to conserve bandwidth
            metadataHeader.put("deviceID", getDeviceID(context));  // we require a unique deviceID
            metadataHeader.put("os", "android");
            metadataHeader.put("osVersion", Build.VERSION.RELEASE);  // human-readable o/s version; like "5.0.1"
            metadataHeader.put("brand", Build.BRAND);  // human-readable brand; like "Samsung"
            metadataHeader.put("model", Build.MODEL);  // human-readable model; like "Galaxy Nexus 5"

            PackageInfo pInfo;
            try {
                pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                metadataHeader.put("appVersionDisplay", pInfo.versionName);  // human readable display version
                metadataHeader.put("appVersionCode", pInfo.versionCode);  // version as known to the app store
                metadataHeader.put("firstInstall", pInfo.firstInstallTime);  // useful!
                metadataHeader.put("lastUpdate", pInfo.lastUpdateTime);  // also useful!
            } catch (PackageManager.NameNotFoundException e) {
                Logger.getInstance(Logger.LOG_TAG_NAME).error("Could not get PackageInfo.", e);
            }
            metadataHeader.put("appLabel", getAppLabel(context));  // human readable app name - it's what shows in the app store, on the app icon, and may not align with mfpAppName

        } catch (JSONException e) {
            // there is no way this exception gets thrown when adding simple strings to a JSONObject
        }
        return metadataHeader.toString();
    }

    private String getDeviceID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public String getAppLabel(Context context) {
        PackageManager lPackageManager = context.getPackageManager();
        ApplicationInfo lApplicationInfo = null;
        try {
            lApplicationInfo = lPackageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.getInstance(Logger.LOG_TAG_NAME).error("Could not get ApplicationInfo.", e);
        }
        return (String) (lApplicationInfo != null ? lPackageManager.getApplicationLabel(lApplicationInfo) : Build.UNKNOWN);
    }
}
