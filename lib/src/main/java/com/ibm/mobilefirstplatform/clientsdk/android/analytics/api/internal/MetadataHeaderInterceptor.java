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

package com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.internal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.MFPAnalytics;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

public class MetadataHeaderInterceptor implements Interceptor {
    protected final String analyticsMetadataHeader;

    public MetadataHeaderInterceptor(Context appContext){
        super();

        analyticsMetadataHeader = generateAnalyticsMetadataHeader(appContext);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();

        Request requestWithHeaders = request.newBuilder()
                .header("x-mfp-analytics-metadata", analyticsMetadataHeader)
                .build();

        com.squareup.okhttp.Response response = chain.proceed(requestWithHeaders);

        return response;
    }

    private String generateAnalyticsMetadataHeader(Context context) {
        // add required analytics headers:
        JSONObject metadataHeader = new JSONObject();

        try {
            // we try to keep the keys short to conserve bandwidth
            metadataHeader.put("deviceID", getDeviceID(context));  // we require a unique deviceID
            metadataHeader.put("os", "android");
            metadataHeader.put("osVersion", Build.VERSION.RELEASE);  // human-readable o/s version; like "5.0.1"
            metadataHeader.put("brand", Build.BRAND);  // human-readable brand; like "Samsung"
            metadataHeader.put("model", Build.MODEL);  // human-readable model; like "Galaxy Nexus 5"

            String applicationPackageName = context.getPackageName();
            metadataHeader.put("appStoreId", applicationPackageName);
            metadataHeader.put("appStoreLabel", getAppLabel(context));  // human readable app name - it's what shows in the app store, on the app icon, and may not align with mfpAppName

            if(MFPAnalytics.appName != null){
                metadataHeader.put("mfpAppName", MFPAnalytics.appName);
            }

            PackageInfo pInfo;
            try {
                pInfo = context.getPackageManager().getPackageInfo(applicationPackageName, 0);
                metadataHeader.put("appVersionDisplay", pInfo.versionName);  // human readable display version
                metadataHeader.put("appVersionCode", pInfo.versionCode);  // version as known to the app store
            } catch (PackageManager.NameNotFoundException e) {
                Logger.getLogger(Logger.LOG_TAG_NAME).error("Could not get PackageInfo.", e);
            }
        } catch (JSONException e) {
            // there is no way this exception gets thrown when adding simple strings to a JSONObject
        }
        return metadataHeader.toString();
    }


    protected String getDeviceID(Context context) {
        String uuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        return UUID.nameUUIDFromBytes(uuid.getBytes()).toString();
    }

    public String getAppLabel(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.getLogger(Logger.LOG_TAG_NAME).error("Could not get ApplicationInfo.", e);
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : Build.UNKNOWN);
    }
}
